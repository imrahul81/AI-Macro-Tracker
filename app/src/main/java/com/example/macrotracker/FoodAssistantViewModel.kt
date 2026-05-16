package com.example.macrotracker

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.macrotracker.data.FoodDatabase
import com.example.macrotracker.data.FoodEntity
import com.example.macrotracker.data.FoodRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.example.macrotracker.BuildConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@Serializable
data class FoodItemAnalysis(
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val imageUrl: String? = null
)

@Serializable
data class MacroResponse(
    val originalInput: String,
    val items: List<FoodItemAnalysis>,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    var manualMealType: String? = null
)

class FoodAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FoodRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    init {
        val foodDao = FoodDatabase.getDatabase(application).foodDao()
        repository = FoodRepository(foodDao)
    }

    val loggedFoods: StateFlow<List<FoodEntity>> = repository.getFoodsForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyFoods: StateFlow<List<FoodEntity>> = repository.allFoods
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Note: In a real production app, never hardcode API keys.
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-flash-preview",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    var uiState by mutableStateOf<FoodAssistantUiState>(FoodAssistantUiState.Idle)
        private set
    
    val recentMeals = mutableStateListOf("Oatmeal", "Greek Yogurt")

    // User Profile State with Persistence
    private var _profileImageUri by mutableStateOf<Uri?>(
        prefs.getString("profile_image_uri", null)?.let { Uri.parse(it) }
    )
    var profileImageUri: Uri?
        get() = _profileImageUri
        set(value) {
            _profileImageUri = value
            prefs.edit().putString("profile_image_uri", value?.toString()).apply()
        }

    private var _height by mutableStateOf(prefs.getString("height", "182") ?: "182")
    var height: String
        get() = _height
        set(value) {
            _height = value
            prefs.edit().putString("height", value).apply()
        }

    private var _weight by mutableStateOf(prefs.getString("weight", "78.5") ?: "78.5")
    var weight: String
        get() = _weight
        set(value) {
            _weight = value
            prefs.edit().putString("weight", value).apply()
        }

    private var _age by mutableStateOf(prefs.getString("age", "29") ?: "29")
    var age: String
        get() = _age
        set(value) {
            _age = value
            prefs.edit().putString("age", value).apply()
        }

    private var _activityLevel by mutableStateOf(prefs.getString("activity_level", "Very Active") ?: "Very Active")
    var activityLevel: String
        get() = _activityLevel
        set(value) {
            _activityLevel = value
            prefs.edit().putString("activity_level", value).apply()
        }

    private var _dailyCalorieGoal by mutableStateOf(prefs.getInt("daily_calorie_goal", 2000))
    var dailyCalorieGoal: Int
        get() = _dailyCalorieGoal
        set(value) {
            _dailyCalorieGoal = value
            prefs.edit().putInt("daily_calorie_goal", value).apply()
        }

    // Dynamic Macro Calculations
    val proteinGoal: Int
        get() {
            val weightKg = weight.toDoubleOrNull() ?: 70.0
            val proteinPerKg = when (activityLevel) {
                "Sedentary" -> 1.2
                "Lightly active" -> 1.5
                "Moderately active" -> 1.8
                "Very active" -> 2.2
                else -> 1.5
            }
            return (weightKg * proteinPerKg).toInt()
        }

    val fatGoal: Int
        get() {
            // Usually 25-30% of calories
            return (dailyCalorieGoal * 0.25 / 9).toInt()
        }

    val carbsGoal: Int
        get() {
            // Remaining calories
            val proteinCalories = proteinGoal * 4
            val fatCalories = fatGoal * 9
            val carbCalories = dailyCalorieGoal - proteinCalories - fatCalories
            return (carbCalories / 4).coerceAtLeast(0)
        }

    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    fun analyzeMeal(input: String) {
        if (input.isBlank()) return

        uiState = FoodAssistantUiState.Loading
        viewModelScope.launch {
            try {
                val prompt = """
                    You are a professional nutritionist assistant. 
                    Analyze the following meal description and provide the nutritional information in JSON format.
                    
                    Rules:
                    1. Breakdown the meal into individual items.
                    2. Estimate calories, protein, carbs, and fat for each item.
                    3. Calculate the total values for the entire meal.
                    4. 'description' should be a very short detail about the preparation (e.g., 'Large', '30g slice', 'Boiled').
                    5. Provide a realistic image URL for each food item from a public source like Unsplash (e.g., https://images.unsplash.com/photo-...) or similar. Use high quality food images.
                    
                    JSON Structure:
                    {
                      "originalInput": "$input",
                      "items": [
                        {
                          "name": "string",
                          "description": "string",
                          "calories": integer,
                          "protein": integer,
                          "carbs": integer,
                          "fat": integer,
                          "imageUrl": "string"
                        }
                      ],
                      "totalCalories": integer,
                      "totalProtein": integer,
                      "totalCarbs": integer,
                      "totalFat": integer
                    }
                    
                    Only return the JSON object, no other text or markdown formatting.
                """.trimIndent()

                val response = generativeModel.generateContent(prompt)
                val responseText = response.text ?: throw Exception("Empty response")
                
                // Extract JSON if there's markdown or other text
                val jsonRegex = Regex("""\{.*\}""", RegexOption.DOT_MATCHES_ALL)
                val match = jsonRegex.find(responseText)
                val jsonText = match?.value ?: responseText.trim()

                val macroResponse = json.decodeFromString<MacroResponse>(jsonText)
                
                uiState = FoodAssistantUiState.Success(macroResponse)
            } catch (e: Exception) {
                uiState = FoodAssistantUiState.Error(e.message ?: "Failed to analyze meal")
            }
        }
    }

    fun confirmMeal(macro: MacroResponse) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            
            // Use manual meal type if provided, otherwise determine based on hour
            val mealType = macro.manualMealType ?: run {
                val hour = now.get(Calendar.HOUR_OF_DAY)
                when {
                    hour < 11 -> "Breakfast"
                    hour < 15 -> "Lunch"
                    hour < 18 -> "Afternoon Snack"
                    else -> "Dinner"
                }
            }

            macro.items.forEach { item ->
                val newEntity = FoodEntity(
                    name = item.name,
                    mealType = mealType,
                    calories = item.calories,
                    protein = item.protein,
                    carbs = item.carbs,
                    fat = item.fat,
                    timestamp = now.timeInMillis,
                    time = timeFormat.format(now.time),
                    imageUrl = item.imageUrl
                )
                repository.insert(newEntity)
            }
            uiState = FoodAssistantUiState.Idle
        }
    }

    fun resetState() {
        uiState = FoodAssistantUiState.Idle
    }
}

sealed interface FoodAssistantUiState {
    object Idle : FoodAssistantUiState
    object Loading : FoodAssistantUiState
    data class Success(val macro: MacroResponse) : FoodAssistantUiState
    data class Error(val message: String) : FoodAssistantUiState
}
