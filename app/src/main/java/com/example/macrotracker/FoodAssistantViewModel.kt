package com.example.macrotracker

import android.app.Application
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
    val fat: Int
)

@Serializable
data class MacroResponse(
    val originalInput: String,
    val items: List<FoodItemAnalysis>,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int
)

class FoodAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FoodRepository

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
                          "fat": integer
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
            
            // Determine meal type based on hour
            val hour = now.get(Calendar.HOUR_OF_DAY)
            val mealType = when {
                hour < 11 -> "Breakfast"
                hour < 15 -> "Lunch"
                hour < 18 -> "Afternoon Snack"
                else -> "Dinner"
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
                    time = timeFormat.format(now.time)
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
