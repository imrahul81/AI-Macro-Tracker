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
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.macrotracker.data.FoodDatabase
import com.example.macrotracker.data.FoodEntity
import com.example.macrotracker.data.FoodRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.example.macrotracker.BuildConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
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
    var manualMealType: String? = null,
    val detectedMealType: String? = null
)

class FoodAssistantViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FoodRepository
    private val prefs: SharedPreferences = application.getSharedPreferences("user_profile", Context.MODE_PRIVATE)

    private val masterKey = MasterKey.Builder(application)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        application,
        "secure_user_profile",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    init {
        val foodDao = FoodDatabase.getDatabase(application).foodDao()
        repository = FoodRepository(foodDao)
    }

    val loggedFoods: StateFlow<List<FoodEntity>> = repository.getFoodsForToday()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // History for a specific selected date
    private val _selectedHistoryDate = MutableStateFlow(System.currentTimeMillis())
    val selectedHistoryDate: StateFlow<Long> = _selectedHistoryDate

    @OptIn(ExperimentalCoroutinesApi::class)
    val historyFoods: StateFlow<List<FoodEntity>> = _selectedHistoryDate
        .flatMapLatest { timestamp ->
            repository.getFoodsForDate(timestamp)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSelectedHistoryDate(timestamp: Long) {
        _selectedHistoryDate.value = timestamp
    }

    private var _apiKey by mutableStateOf(prefs.getString("api_key", BuildConfig.GEMINI_API_KEY) ?: BuildConfig.GEMINI_API_KEY)
    var apiKey: String
        get() = _apiKey
        set(value) {
            _apiKey = value
            prefs.edit().putString("api_key", value).apply()
            updateGenerativeModel()
        }

    private var _selectedModel by mutableStateOf(prefs.getString("selected_model", "gemini-1.5-flash") ?: "gemini-1.5-flash")
    var selectedModel: String
        get() = _selectedModel
        set(value) {
            _selectedModel = value
            prefs.edit().putString("selected_model", value).apply()
            updateGenerativeModel()
        }

    // Note: In a real production app, never hardcode API keys.
    private var generativeModel = GenerativeModel(
        modelName = _selectedModel,
        apiKey = _apiKey
    )

    private fun updateGenerativeModel() {
        generativeModel = GenerativeModel(
            modelName = selectedModel,
            apiKey = apiKey
        )
    }

    var uiState by mutableStateOf<FoodAssistantUiState>(FoodAssistantUiState.Idle)
        private set
    
    val recentMeals = mutableStateListOf("Oatmeal", "Greek Yogurt")

    // User Profile State with Persistence
    private var _name by mutableStateOf(prefs.getString("name", "Alex Johnson") ?: "Alex Johnson")
    var name: String
        get() = _name
        set(value) {
            _name = value
            prefs.edit().putString("name", value).apply()
        }

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

    private var _isDarkMode by mutableStateOf(prefs.getBoolean("is_dark_mode", false))
    private var _useSystemTheme by mutableStateOf(prefs.getBoolean("use_system_theme", true))

    var useSystemTheme: Boolean
        get() = _useSystemTheme
        set(value) {
            _useSystemTheme = value
            prefs.edit().putBoolean("use_system_theme", value).apply()
        }

    var isDarkMode: Boolean
        get() = _isDarkMode
        set(value) {
            _isDarkMode = value
            prefs.edit().putBoolean("is_dark_mode", value).apply()
            // When user manually toggles, we disable "follow system"
            useSystemTheme = false
        }

    private var _dailyCalorieGoal by mutableStateOf(prefs.getInt("daily_calorie_goal", 2000))
    var dailyCalorieGoal: Int
        get() = _dailyCalorieGoal
        set(value) {
            _dailyCalorieGoal = value
            prefs.edit().putInt("daily_calorie_goal", value).apply()
        }

    private var _remindersEnabled by mutableStateOf(prefs.getBoolean("reminders_enabled", true))
    var remindersEnabled: Boolean
        get() = _remindersEnabled
        set(value) {
            _remindersEnabled = value
            prefs.edit().putBoolean("reminders_enabled", value).apply()
            if (value) {
                scheduleAllReminders()
            } else {
                cancelAllReminders()
            }
        }

    private fun scheduleAllReminders() {
        val workManager = WorkManager.getInstance(getApplication())
        
        val mealTimes = listOf(
            "Breakfast" to 10,
            "Lunch" to 13,
            "Afternoon Snack" to 17,
            "Dinner" to 21,
            "Final Reminder" to 23
        )

        mealTimes.forEach { (meal, hour) ->
            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                if (before(Calendar.getInstance())) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val delay = calendar.timeInMillis - System.currentTimeMillis()
            
            // We use periodic work to repeat every 24 hours
            val workRequest = PeriodicWorkRequestBuilder<MealReminderWorker>(24, java.util.concurrent.TimeUnit.HOURS)
                .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("meal_type" to meal))
                .build()

            workManager.enqueueUniquePeriodicWork(
                "reminder_$meal",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }
    }

    private fun cancelAllReminders() {
        val workManager = WorkManager.getInstance(getApplication())
        workManager.cancelAllWorkByTag("reminder_") // This might not work as expected with cancelAllWorkByTag
        // Better way since we used unique names:
        val meals = listOf("Breakfast", "Lunch", "Afternoon Snack", "Dinner", "Final Reminder")
        meals.forEach { meal ->
            workManager.cancelUniqueWork("reminder_$meal")
        }
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
                if (apiKey.isBlank()) {
                    throw Exception("API Key is missing. Please enter your Gemini API Key in Settings.")
                }

                val prompt = """
                    You are a professional nutritionist assistant. 
                    Analyze the following meal description and provide the nutritional information in JSON format.
                    
                    Rules:
                    1. Breakdown the meal into individual items.
                    2. Estimate calories, protein, carbs, and fat for each item.
                    3. Calculate the total values for the entire meal.
                    4. 'description' should be a very short detail about the preparation (e.g., 'Large', '30g slice', 'Boiled').
                    5. Provide a realistic image URL for each food item from a public source like Unsplash (e.g., https://images.unsplash.com/photo-...) or similar. Use high quality food images.
                    6. Detect if the user explicitly mentioned a meal type (Breakfast, Lunch, Afternoon Snack, Dinner). If found, put it in 'detectedMealType'.
                    
                    JSON Structure:
                    {
                      "originalInput": "$input",
                      "detectedMealType": "string or null",
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
