package com.rahul.macrotracker

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.rahul.macrotracker.data.FoodEntity
import com.rahul.macrotracker.ui.theme.MacroTrackerTheme
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FoodAssistantViewModel = viewModel()
            val systemInDarkTheme = isSystemInDarkTheme()
            
            val darkTheme = if (viewModel.useSystemTheme) {
                systemInDarkTheme
            } else {
                viewModel.isDarkMode
            }

            MacroTrackerTheme(darkTheme = darkTheme) {
                MainScreen(viewModel)
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Welcome : Screen("welcome", "Welcome", Icons.Default.Handshake)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.GridView)
    data object LogFood : Screen("log_food", "Log Food", Icons.Default.AddCircleOutline)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Profile : Screen("profile", "Profile", Icons.Default.PersonOutline)
    data object ReviewMeal : Screen("review_meal", "Review Meal", Icons.Default.CheckCircle)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings)
}

@Composable
fun MainScreen(viewModel: FoodAssistantViewModel = viewModel()) {
    val loggedFoods by viewModel.loggedFoods.collectAsState()
    val historyFoods by viewModel.historyFoods.collectAsState()
    val historyDate by viewModel.selectedHistoryDate.collectAsState()
    val uiState = viewModel.uiState
    val recentMeals = viewModel.recentMeals
    val systemInDarkTheme = isSystemInDarkTheme()

    MainScreenContent(
        profileImageUri = viewModel.profileImageUri,
        dailyCalorieGoal = viewModel.dailyCalorieGoal,
        proteinGoal = viewModel.proteinGoal,
        carbsGoal = viewModel.carbsGoal,
        fatGoal = viewModel.fatGoal,
        name = viewModel.name,
        height = viewModel.height,
        weight = viewModel.weight,
        targetWeight = viewModel.targetWeight,
        bodyFat = viewModel.bodyFat,
        targetBodyFat = viewModel.targetBodyFat,
        goalPace = viewModel.goalPace,
        gender = viewModel.gender,
        age = viewModel.age,
        activityLevel = viewModel.activityLevel,
        isDarkMode = if (viewModel.useSystemTheme) systemInDarkTheme else viewModel.isDarkMode,
        useSystemTheme = viewModel.useSystemTheme,
        isFirstTime = viewModel.isFirstTime,
        apiKey = viewModel.apiKey,
        selectedModel = viewModel.selectedModel,
        onNameChange = { viewModel.name = it },
        onGenderChange = { viewModel.gender = it },
        onProfileImageChange = { viewModel.profileImageUri = it },
        onHeightChange = { viewModel.height = it },
        onWeightChange = { viewModel.weight = it },
        onTargetWeightChange = { viewModel.targetWeight = it },
        onBodyFatChange = { viewModel.bodyFat = it },
        onTargetBodyFatChange = { viewModel.targetBodyFat = it },
        onGoalPaceChange = { viewModel.goalPace = it },
        onAgeChange = { viewModel.age = it },
        onActivityLevelChange = { viewModel.activityLevel = it },
        onCalorieGoalChange = { viewModel.dailyCalorieGoal = it },
        onSavePersonalInfo = { viewModel.calculateNutritionalGoals() },
        onDarkModeChange = { viewModel.isDarkMode = it },
        onUseSystemThemeChange = { viewModel.useSystemTheme = it },
        onApiKeyChange = { viewModel.apiKey = it },
        onModelChange = { viewModel.selectedModel = it },
        isPersonalInfoExpanded = viewModel.isPersonalInfoExpanded,
        onPersonalInfoExpandedChange = { viewModel.isPersonalInfoExpanded = it },
        remindersEnabled = viewModel.remindersEnabled,
        onRemindersEnabledChange = { viewModel.remindersEnabled = it },
        loggedFoods = loggedFoods,
        historyFoods = historyFoods,
        historyDate = historyDate,
        uiState = uiState,
        recentMeals = recentMeals,
        onAnalyzeMeal = { text, bitmap -> viewModel.analyzeMeal(text, bitmap) },
        onConfirmMeal = { viewModel.confirmMeal(it) },
        onResetState = { viewModel.resetState() },
        onDateSelected = { viewModel.setSelectedHistoryDate(it) },
        onDeleteMeal = { mealType, date -> viewModel.deleteMeal(mealType, date) },
        onFirstTimeFinished = { viewModel.isFirstTime = false }
    )
}

@Composable
fun MainScreenContent(
    profileImageUri: Uri?,
    dailyCalorieGoal: Int,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int,
    name: String,
    height: String,
    weight: String,
    targetWeight: String,
    bodyFat: String,
    targetBodyFat: String,
    goalPace: String,
    gender: String,
    age: String,
    activityLevel: String,
    isDarkMode: Boolean,
    useSystemTheme: Boolean,
    isFirstTime: Boolean,
    apiKey: String,
    selectedModel: String,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onProfileImageChange: (Uri) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onTargetBodyFatChange: (String) -> Unit,
    onGoalPaceChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit,
    onCalorieGoalChange: (Int) -> Unit,
    onSavePersonalInfo: () -> Unit,
    onDarkModeChange: (Boolean) -> Unit,
    onUseSystemThemeChange: (Boolean) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onModelChange: (String) -> Unit,
    isPersonalInfoExpanded: Boolean,
    onPersonalInfoExpandedChange: (Boolean) -> Unit,
    remindersEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    loggedFoods: List<FoodEntity>,
    historyFoods: List<FoodEntity>,
    historyDate: Long,
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String, Bitmap?) -> Unit,
    onConfirmMeal: (MacroResponse) -> Unit,
    onResetState: () -> Unit,
    onDateSelected: (Long) -> Unit,
    onDeleteMeal: (String, Long) -> Unit,
    onFirstTimeFinished: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isWelcomeScreen = currentDestination?.route == Screen.Welcome.route
    val isSettingsScreen = currentDestination?.route == Screen.Settings.route

    val pagerScreens = listOf(
        Screen.Dashboard,
        Screen.LogFood,
        Screen.History,
        Screen.Profile
    )
    val pagerState = rememberPagerState(pageCount = { pagerScreens.size })
    val coroutineScope = rememberCoroutineScope()

    val screenOrder = listOf(
        Screen.Welcome.route,
        "main_tabs",
        Screen.ReviewMeal.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { 
            if (!isSettingsScreen && !isWelcomeScreen) {
                VitalityTopBar(
                    profileImageUri = profileImageUri,
                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                    onProfileClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerScreens.indexOf(Screen.Profile))
                        }
                        if (currentDestination?.route != "main_tabs") {
                            navController.navigate("main_tabs") {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                ) 
            }
        },
        bottomBar = {
            if (!isSettingsScreen && !isWelcomeScreen) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    pagerScreens.forEachIndexed { index, screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.route == "main_tabs" && pagerState.currentPage == index,
                            onClick = {
                                if (currentDestination?.route != "main_tabs") {
                                    navController.navigate("main_tabs") {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = Color(0xFF2ECC71),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = if (isFirstTime) Screen.Welcome.route else "main_tabs",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = {
                val initialIndex = screenOrder.indexOf(initialState.destination.route)
                val targetIndex = screenOrder.indexOf(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300))
                }
            },
            exitTransition = {
                val initialIndex = screenOrder.indexOf(initialState.destination.route)
                val targetIndex = screenOrder.indexOf(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) + fadeOut(animationSpec = tween(300))
                }
            },
            popEnterTransition = {
                val initialIndex = screenOrder.indexOf(initialState.destination.route)
                val targetIndex = screenOrder.indexOf(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { it }) + fadeIn(animationSpec = tween(300))
                } else {
                    slideInHorizontally(animationSpec = tween(300), initialOffsetX = { -it }) + fadeIn(animationSpec = tween(300))
                }
            },
            popExitTransition = {
                val initialIndex = screenOrder.indexOf(initialState.destination.route)
                val targetIndex = screenOrder.indexOf(targetState.destination.route)
                if (targetIndex > initialIndex) {
                    slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { -it }) + fadeOut(animationSpec = tween(300))
                } else {
                    slideOutHorizontally(animationSpec = tween(300), targetOffsetX = { it }) + fadeOut(animationSpec = tween(300))
                }
            }
        ) {
            composable(Screen.Welcome.route) {
                WelcomeScreenContent(
                    onGetStarted = {
                        onFirstTimeFinished()
                        navController.navigate("main_tabs") {
                            popUpTo(Screen.Welcome.route) { inclusive = true }
                        }
                    }
                )
            }
            composable("main_tabs") {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (pagerScreens[page]) {
                        Screen.Dashboard -> MainDashboardContent(loggedFoods, dailyCalorieGoal, proteinGoal, carbsGoal, fatGoal)
                        Screen.LogFood -> LogFoodScreenContent(
                            uiState = uiState,
                            recentMeals = recentMeals,
                            onAnalyzeMeal = onAnalyzeMeal,
                            onSuccess = {
                                navController.navigate(Screen.ReviewMeal.route)
                            },
                            onResetState = onResetState
                        )
                        Screen.History -> HistoryScreenContent(historyFoods, historyDate, dailyCalorieGoal, onDateSelected, onDeleteMeal)
                        Screen.Profile -> ProfileScreenContent(
                            name = name,
                            profileImageUri = profileImageUri,
                            height = height,
                            weight = weight,
                            targetWeight = targetWeight,
                            bodyFat = bodyFat,
                            targetBodyFat = targetBodyFat,
                            goalPace = goalPace,
                            gender = gender,
                            age = age,
                            activityLevel = activityLevel,
                            isExpanded = isPersonalInfoExpanded,
                            onExpandedChange = onPersonalInfoExpandedChange,
                            dailyCalorieGoal = dailyCalorieGoal,
                            proteinGoal = proteinGoal,
                            carbsGoal = carbsGoal,
                            fatGoal = fatGoal,
                            onNameChange = onNameChange,
                            onGenderChange = onGenderChange,
                            onProfileImageChange = onProfileImageChange,
                            onHeightChange = onHeightChange,
                            onWeightChange = onWeightChange,
                            onTargetWeightChange = onTargetWeightChange,
                            onBodyFatChange = onBodyFatChange,
                            onTargetBodyFatChange = onTargetBodyFatChange,
                            onGoalPaceChange = onGoalPaceChange,
                            onAgeChange = onAgeChange,
                            onActivityLevelChange = onActivityLevelChange,
                            onCalorieGoalChange = onCalorieGoalChange,
                            onSavePersonalInfo = onSavePersonalInfo
                        )
                        else -> {}
                    }
                }
            }
            composable(Screen.ReviewMeal.route) {
                var lastMacro by remember { mutableStateOf<MacroResponse?>(null) }
                if (uiState is FoodAssistantUiState.Success) {
                    lastMacro = uiState.macro
                }

                lastMacro?.let { macro ->
                    ReviewMealScreenContent(
                        macro = macro,
                        onConfirmMeal = {
                            onConfirmMeal(it)
                            navController.navigate("main_tabs") {
                                popUpTo("main_tabs") { inclusive = true }
                            }
                        },
                        onBack = {
                            onResetState()
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(Screen.Settings.route) {
                SettingsScreenContent(
                    isDarkMode = isDarkMode,
                    onDarkModeChange = onDarkModeChange,
                    useSystemTheme = useSystemTheme,
                    onUseSystemThemeChange = onUseSystemThemeChange,
                    apiKey = apiKey,
                    onApiKeyChange = onApiKeyChange,
                    selectedModel = selectedModel,
                    onModelChange = onModelChange,
                    remindersEnabled = remindersEnabled,
                    onRemindersEnabledChange = onRemindersEnabledChange,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
fun WelcomeScreenContent(onGetStarted: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        
        Icon(
            Icons.Default.AutoAwesome,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Color(0xFF2ECC71)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            "Welcome to AI Macro Tracker",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            "Your intelligent companion for a healthier lifestyle",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        FeatureRow(
            icon = Icons.Default.GridView,
            title = "Personal Dashboard",
            description = "Track your daily calorie and macro progress at a glance. See how much you have left and if you're hitting your goals."
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FeatureRow(
            icon = Icons.Default.AddCircleOutline,
            title = "AI Food Logging",
            description = "Simply describe what you ate in natural language or use your voice. Our AI will analyze the nutrients for you."
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        FeatureRow(
            icon = Icons.Default.History,
            title = "Meal History",
            description = "Review your past logs, see detailed breakdowns of each meal, and manage your history with easy deletion and editing."
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onGetStarted,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text("Get Started", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun FeatureRow(icon: ImageVector, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(24.dp))
        
        Column {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MainDashboardContent(
    foods: List<FoodEntity>,
    calorieGoal: Int,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int
) {
    val totalCalories = foods.sumOf { it.calories }
    val totalProtein = foods.sumOf { it.protein }
    val totalCarbs = foods.sumOf { it.carbs }
    val totalFat = foods.sumOf { it.fat }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            CalorieOverview(totalCalories, calorieGoal)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                MacroCard(
                    label = "Protein",
                    value = "${totalProtein}g",
                    progress = (totalProtein.toFloat() / proteinGoal).coerceIn(0f, 1f),
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Carbs",
                    value = "${totalCarbs}g",
                    progress = (totalCarbs.toFloat() / carbsGoal).coerceIn(0f, 1f),
                    color = Color(0xFF1976D2),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Fats",
                    value = "${totalFat}g",
                    progress = (totalFat.toFloat() / fatGoal).coerceIn(0f, 1f),
                    color = Color(0xFFE65100),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Text(
                "Recent Log",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        if (foods.isEmpty()) {
            item {
                Text(
                    "No meals logged today yet. Tap the Log Food button to start!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
        } else {
            items(foods) { foodEntity ->
                FoodListItemEntity(foodEntity)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreenContent(foods: List<FoodEntity>, selectedDate: Long, calorieGoal: Int, onDateSelected: (Long) -> Unit, onDeleteMeal: (String, Long) -> Unit) {
    val groupedFoods = foods.groupBy { it.mealType }
    val mealTypes = listOf("Breakfast", "Lunch", "Afternoon Snack", "Dinner")
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
    
    val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateString = if (android.text.format.DateUtils.isToday(selectedDate)) {
        "Today, ${SimpleDateFormat("MMM dd", Locale.getDefault()).format(selectedDate)}"
    } else {
        dateFormatter.format(selectedDate)
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateSelected(it) }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text("History", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        dateString,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item {
            HistorySummaryCard(foods, calorieGoal)
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Timeline items
        mealTypes.forEachIndexed { index, mealType ->
            item {
                val mealFoods = groupedFoods[mealType] ?: emptyList()
                val totalCalories = mealFoods.sumOf { it.calories }
                val icon = when (mealType) {
                    "Breakfast" -> Icons.Default.Coffee
                    "Lunch" -> Icons.Default.Restaurant
                    "Afternoon Snack" -> Icons.Default.Fastfood
                    else -> Icons.Default.Dining
                }
                
                TimelineItem(
                    mealType = mealType,
                    time = if (mealFoods.isNotEmpty()) mealFoods.first().time else "--:--",
                    calories = totalCalories,
                    foods = mealFoods,
                    icon = icon,
                    iconColor = if (mealFoods.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    isFirst = index == 0,
                    isLast = index == mealTypes.size - 1,
                    onDelete = { onDeleteMeal(mealType, selectedDate) }
                )
            }
        }
    }
}

@Composable
fun HistorySummaryCard(foods: List<FoodEntity>, goal: Int) {
    val consumed = foods.sumOf { it.calories }
    val left = (goal - consumed).coerceAtLeast(0)
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1f)
    val isOverBudget = consumed > goal

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.Center) {
                MacroRing(
                    progress = progress,
                    color = if (isOverBudget) Color(0xFFC0392B) else MaterialTheme.colorScheme.primary,
                    size = 100.dp,
                    strokeWidth = 10.dp,
                    inactiveColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (isOverBudget) "OVER" else "LEFT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (isOverBudget) (consumed - goal).toString() else left.toString(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isOverBudget) Color(0xFFC0392B) else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text("EATEN", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append(String.format(Locale.getDefault(), "%, d", consumed))
                        }
                        withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(" kcal")
                        }
                    }
                )
            }

            Column {
                Text("GOAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append(String.format(Locale.getDefault(), "%, d", goal))
                        }
                        withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                            append(" kcal")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TimelineItem(
    mealType: String,
    time: String,
    calories: Int,
    foods: List<FoodEntity>,
    icon: ImageVector,
    iconColor: Color,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    onDelete: () -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Meal") },
            text = { Text("Are you sure you want to delete this $mealType entry and all its items?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirmation = false
                    }
                ) {
                    Text("Delete", color = Color(0xFFC0392B), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(48.dp)
        ) {
            if (!isFirst) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (iconColor == MaterialTheme.colorScheme.surfaceVariant) MaterialTheme.colorScheme.onSurfaceVariant else Color.White)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                onClick = { if (foods.isNotEmpty()) isExpanded = !isExpanded }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mealType, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(time, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF446180))) {
                                    append(calories.toString())
                                }
                                withStyle(SpanStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append(" kcal")
                                }
                            }
                        )
                        if (foods.isNotEmpty()) {
                            IconButton(onClick = { showDeleteConfirmation = true }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete Meal",
                                    tint = Color(0xFFC0392B)
                                )
                            }
                        }
                    }

                    if (foods.isNotEmpty() && isExpanded) {
                        Spacer(modifier = Modifier.height(16.dp))
                        foods.forEach { food ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val imageUrl = if (food.imageUrl.isNullOrBlank() || food.imageUrl == "null" || food.imageUrl == "string") {
                                        "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=100&auto=format&fit=crop"
                                    } else {
                                        food.imageUrl
                                    }
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = food.name,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("${food.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val totalProtein = foods.sumOf { it.protein }
                            val totalCarbs = foods.sumOf { it.carbs }
                            val totalFat = foods.sumOf { it.fat }
                            
                            MacroBadge("Protein", "${totalProtein}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF2E7D32))
                            MacroBadge("Carbs", "${totalCarbs}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF1976D2))
                            MacroBadge("Fats", "${totalFat}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFFE65100))
                        }
                    } else if (foods.isNotEmpty()) {
                         Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MacroBadge(label: String, value: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun ProfileScreenContent(
    name: String,
    profileImageUri: Uri?,
    height: String,
    weight: String,
    targetWeight: String,
    bodyFat: String,
    targetBodyFat: String,
    goalPace: String,
    gender: String,
    age: String,
    activityLevel: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    dailyCalorieGoal: Int,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int,
    onNameChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onProfileImageChange: (Uri) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onTargetBodyFatChange: (String) -> Unit,
    onGoalPaceChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit,
    onCalorieGoalChange: (Int) -> Unit,
    onSavePersonalInfo: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
                // Not a persistable URI, but we'll still try to use it
            }
            onProfileImageChange(it)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        item {
            ProfileHeader(
                name = name,
                imageUri = profileImageUri,
                isEditing = isEditing,
                onNameChange = onNameChange,
                onEditImage = { 
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
        item {
            PersonalInfoSection(
                height = height,
                weight = weight,
                targetWeight = targetWeight,
                bodyFat = bodyFat,
                targetBodyFat = targetBodyFat,
                goalPace = goalPace,
                gender = gender,
                age = age,
                activityLevel = activityLevel,
                isExpanded = isExpanded,
                onExpandedChange = onExpandedChange,
                isEditing = isEditing,
                onEditClick = { 
                    if (isEditing) onSavePersonalInfo()
                    isEditing = !isEditing 
                },
                onHeightChange = onHeightChange,
                onWeightChange = onWeightChange,
                onTargetWeightChange = onTargetWeightChange,
                onBodyFatChange = onBodyFatChange,
                onTargetBodyFatChange = onTargetBodyFatChange,
                onGoalPaceChange = onGoalPaceChange,
                onGenderChange = onGenderChange,
                onAgeChange = onAgeChange,
                onActivityLevelChange = onActivityLevelChange
            )
        }
        item {
            NutritionalGoalsSection(
                calorieGoal = dailyCalorieGoal,
                onCalorieGoalChange = onCalorieGoalChange,
                proteinGoal = proteinGoal,
                carbsGoal = carbsGoal,
                fatGoal = fatGoal
            )
        }
    }
}

@Composable
fun ProfileHeader(name: String, imageUri: Uri?, onEditImage: () -> Unit, isEditing: Boolean, onNameChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(4.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                    )
                } else {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        tint = Color.LightGray
                    )
                }
            }
            Surface(
                modifier = Modifier
                    .size(32.dp)
                    .clickable { onEditImage() },
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit Profile Image",
                    tint = Color.White,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        if (isEditing) {
            TextField(
                value = name,
                onValueChange = onNameChange,
                modifier = Modifier.width(200.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        } else {
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun PersonalInfoSection(
    height: String,
    weight: String,
    targetWeight: String,
    bodyFat: String,
    targetBodyFat: String,
    goalPace: String,
    gender: String,
    age: String,
    activityLevel: String,
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onBodyFatChange: (String) -> Unit,
    onTargetBodyFatChange: (String) -> Unit,
    onGoalPaceChange: (String) -> Unit,
    onGenderChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!isExpanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Personal Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                TextButton(onClick = {
                    if (!isExpanded) onExpandedChange(true)
                    onEditClick()
                }) {
                    Text(if (isEditing) "Save" else "Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        EditableInfoItem(
                            label = "Height",
                            value = height,
                            unit = "cm",
                            isEditing = isEditing,
                            onValueChange = onHeightChange,
                            modifier = Modifier.weight(1f)
                        )
                        EditableInfoItem(
                            label = "Weight",
                            value = weight,
                            unit = "kg",
                            isEditing = isEditing,
                            onValueChange = onWeightChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        EditableInfoItem(
                            label = "Target Weight",
                            value = targetWeight,
                            unit = "kg",
                            isEditing = isEditing,
                            onValueChange = onTargetWeightChange,
                            modifier = Modifier.weight(1f)
                        )
                        EditableInfoItem(
                            label = "Age",
                            value = age,
                            unit = "years",
                            isEditing = isEditing,
                            onValueChange = onAgeChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        EditableInfoItem(
                            label = "Body Fat",
                            value = bodyFat,
                            unit = "%",
                            isEditing = isEditing,
                            onValueChange = onBodyFatChange,
                            modifier = Modifier.weight(1f)
                        )
                        EditableInfoItem(
                            label = "Target Body Fat",
                            value = targetBodyFat,
                            unit = "%",
                            isEditing = isEditing,
                            onValueChange = onTargetBodyFatChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        GenderDropdown(
                            value = gender,
                            isEditing = isEditing,
                            onValueChange = onGenderChange,
                            modifier = Modifier.weight(1f)
                        )
                        GoalPaceDropdown(
                            value = goalPace,
                            isEditing = isEditing,
                            onValueChange = onGoalPaceChange,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    ActivityLevelDropdown(
                        value = activityLevel,
                        isEditing = isEditing,
                        onValueChange = onActivityLevelChange,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GenderDropdown(
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Male", "Female")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("Gender", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalPaceDropdown(
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Moderate", "Aggressive", "Extreme")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("Goal Pace", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLevelDropdown(
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Sedentary", "Lightly active", "Moderately active", "Very active")
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        Text("Activity Level", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        
        if (isEditing) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                TextField(
                    value = value,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedIndicatorColor = MaterialTheme.colorScheme.primary
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onValueChange(option)
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EditableInfoItem(
    label: String,
    value: String,
    unit: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(4.dp))
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    focusedIndicatorColor = MaterialTheme.colorScheme.primary
                )
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (unit.isNotEmpty()) "$value $unit" else value,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun NutritionalGoalsSection(
    calorieGoal: Int,
    onCalorieGoalChange: (Int) -> Unit,
    proteinGoal: Int,
    carbsGoal: Int,
    fatGoal: Int
) {
    var isEditing by remember { mutableStateOf(false) }
    var tempCalorieInput by remember(calorieGoal) { mutableStateOf(calorieGoal.toString()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nutritional Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = { 
                    if (isEditing) {
                        onCalorieGoalChange(tempCalorieInput.toIntOrNull() ?: calorieGoal)
                    }
                    isEditing = !isEditing 
                }) {
                    Text(if (isEditing) "Save" else "Edit", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Calories", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (isEditing) {
                        TextField(
                            value = tempCalorieInput,
                            onValueChange = { tempCalorieInput = it },
                            modifier = Modifier.width(100.dp),
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary
                            ),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF446180),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )
                    } else {
                        Text(
                            String.format(Locale.getDefault(), "%, d kcal", calorieGoal),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF446180)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroGoalBadge("Protein", "${proteinGoal}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                MacroGoalBadge("Carbs", "${carbsGoal}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF1976D2), modifier = Modifier.weight(1f))
                MacroGoalBadge("Fats", "${fatGoal}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFFE65100), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun MacroGoalBadge(label: String, value: String, bgColor: Color, textColor: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreenContent(
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String, Bitmap?) -> Unit,
    onSuccess: () -> Unit,
    onResetState: () -> Unit
) {
    var mealInput by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            val inputStream = context.contentResolver.openInputStream(it)
            selectedBitmap = BitmapFactory.decodeStream(inputStream)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        selectedBitmap = bitmap
    }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                mealInput = results[0]
            }
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Snap a photo or choose from gallery to identify nutrients.") },
            confirmButton = {
                TextButton(onClick = {
                    imagePickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    showImageSourceDialog = false
                }) {
                    Text("Gallery", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    cameraLauncher.launch(null)
                    showImageSourceDialog = false
                }) {
                    Text("Camera", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    LaunchedEffect(uiState) {
        if (uiState is FoodAssistantUiState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "What did you eat?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Describe your meal in your own words.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (selectedBitmap != null) {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(bottom = 12.dp)) {
                        AsyncImage(
                            model = selectedBitmap,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                        IconButton(
                            onClick = { selectedBitmap = null },
                            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Color.Black.copy(alpha = 0.5f), CircleShape).size(28.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image", tint = Color.White, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                TextField(
                    value = mealInput,
                    onValueChange = { mealInput = it },
                    placeholder = {
                        Text(
                            "Message AI Assistant...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = MaterialTheme.typography.bodyLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { showImageSourceDialog = true },
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Attach", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your meal...")
                                }
                                speechLauncher.launch(intent)
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(Icons.Default.MicNone, contentDescription = "Voice", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onAnalyzeMeal(mealInput, selectedBitmap) },
                            enabled = uiState !is FoodAssistantUiState.Loading && (mealInput.isNotBlank() || selectedBitmap != null),
                            modifier = Modifier
                                .background(
                                    if (mealInput.isNotBlank() || selectedBitmap != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    CircleShape
                                )
                                .size(40.dp)
                        ) {
                            if (uiState is FoodAssistantUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.ArrowUpward, contentDescription = "Analyze", tint = Color.White)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (uiState is FoodAssistantUiState.Error) {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        uiState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        InfoCard(
            icon = Icons.Default.Lightbulb,
            title = "Be Specific",
            description = "Mention portion sizes like 'a handful' or 'half a plate' for better accuracy.",
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
fun ReviewMealScreenContent(
    macro: MacroResponse,
    onConfirmMeal: (MacroResponse) -> Unit,
    onBack: () -> Unit
) {
    var selectedMealType by remember { mutableStateOf(macro.detectedMealType) }
    val mealTypes = listOf("Breakfast", "Lunch", "Afternoon Snack", "Dinner")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Review Meal",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "\"${macro.originalInput}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Card(
                        modifier = Modifier.weight(1.2f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL CALORIES", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                macro.totalCalories.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroSummaryBadge("Protein", "${macro.totalProtein}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF2E7D32))
                        MacroSummaryBadge("Carbs", "${macro.totalCarbs}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFF1976D2))
                        MacroSummaryBadge("Fats", "${macro.totalFat}g", MaterialTheme.colorScheme.surfaceVariant, Color(0xFFE65100))
                    }
                }
            }

            item {
                Text(
                    "Detected Items",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Tag this meal (Optional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        mealTypes.forEach { type ->
                            val isSelected = selectedMealType == type
                            Surface(
                                onClick = { selectedMealType = if (isSelected) null else type },
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                                modifier = Modifier.height(40.dp)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = type,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(macro.items) { item ->
                DetectedItemCard(item)
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Button(
                onClick = { 
                    macro.manualMealType = selectedMealType
                    onConfirmMeal(macro) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2ECC71)),
                shape = RoundedCornerShape(28.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Log Meal", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MacroSummaryBadge(label: String, value: String, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun DetectedItemCard(item: FoodItemAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AsyncImage(
                        model = item.imageUrl ?: "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=200&auto=format&fit=crop",
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                        error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CAL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.calories.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF446180))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MacroMiniInfo("P", "${item.protein}g")
                    MacroMiniInfo("C", "${item.carbs}g")
                    MacroMiniInfo("F", "${item.fat}g")
                }
            }
        }
    }
}

@Composable
fun MacroMiniInfo(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = contentColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, color = contentColor)
            Spacer(modifier = Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = contentColor)
        }
    }
}


@Composable
fun SettingsScreenContent(
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    useSystemTheme: Boolean,
    onUseSystemThemeChange: (Boolean) -> Unit,
    apiKey: String,
    onApiKeyChange: (String) -> Unit,
    selectedModel: String,
    onModelChange: (String) -> Unit,
    remindersEnabled: Boolean,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onBack: () -> Unit
) {
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showModelDialog by remember { mutableStateOf(false) }
    var driveBackupEnabled by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onRemindersEnabledChange(true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Settings",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingsSection(title = "Appearance") {
                    SettingsToggleItem(
                        label = "Dark Mode",
                        icon = Icons.Default.DarkMode,
                        checked = isDarkMode,
                        onCheckedChange = onDarkModeChange
                    )
                }
            }

            item {
                SettingsSection(title = "AI Configuration") {
                    SettingsClickItem(
                        label = "Gemini API Key",
                        value = if (apiKey.isEmpty()) "Not Set" else "••••••••",
                        icon = Icons.Default.VpnKey,
                        onClick = { showApiKeyDialog = true }
                    )
                    SettingsClickItem(
                        label = "Model",
                        value = selectedModel,
                        icon = Icons.Default.AutoAwesome,
                        onClick = { showModelDialog = true }
                    )
                }
            }

            item {
                SettingsSection(title = "Data & Sync") {
                    SettingsToggleItem(
                        label = "Google Drive Backup",
                        icon = Icons.Default.CloudUpload,
                        checked = driveBackupEnabled,
                        onCheckedChange = { driveBackupEnabled = it },
                        subtitle = "Sync meal history (Coming Soon)"
                    )
                }
            }

            item {
                SettingsSection(title = "Notifications") {
                    SettingsToggleItem(
                        label = "Logging Reminders",
                        icon = Icons.Default.NotificationsActive,
                        checked = remindersEnabled,
                        onCheckedChange = {
                            if (it) {
                                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onRemindersEnabledChange(false)
                            }
                        },
                        subtitle = "Get notified if you forget to log meals"
                    )
                }
            }

            item {
                SettingsSection(title = "Data Management") {
                    SettingsClickItem(
                        label = "Clear All Data",
                        value = "",
                        icon = Icons.Default.DeleteForever,
                        onClick = { /* Implement clear data logic */ }
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC0392B)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC0392B))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Logout", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }

    if (showApiKeyDialog) {
        var tempApiKey by remember { mutableStateOf(apiKey) }
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = { Text("Enter Gemini API Key") },
            text = {
                Column {
                    Text(
                        "Your key is stored securely on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    TextField(
                        value = tempApiKey,
                        onValueChange = { tempApiKey = it },
                        placeholder = { Text("Paste your API Key here") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onApiKeyChange(tempApiKey)
                    showApiKeyDialog = false
                }) {
                    Text("Save", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showModelDialog) {
        val models = listOf("gemini-1.5-flash", "gemini-1.5-pro", "gemini-3-flash-preview")
        AlertDialog(
            onDismissRequest = { showModelDialog = false },
            title = { Text("Select Gemini Model") },
            text = {
                Column {
                    models.forEach { model ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { 
                                    onModelChange(model)
                                    showModelDialog = false
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (model == selectedModel),
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(model)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModelDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsToggleItem(
    label: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = Color(0xFF446180))
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2ECC71))
        )
    }

}

@Composable
fun SettingsClickItem(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF446180))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
        }
    }
}

@Composable
fun SettingsInputItem(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    icon: ImageVector,
    placeholder: String,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    var isVisible by remember { mutableStateOf(visualTransformation == VisualTransformation.None) }
    
    Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color(0xFF446180))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                if (visualTransformation != VisualTransformation.None) {
                    IconButton(onClick = { isVisible = !isVisible }) {
                        Icon(
                            if (isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (isVisible) "Hide" else "Show"
                        )
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedIndicatorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(12.dp)
        )
    }

}

@Composable
fun VitalityTopBar(
    profileImageUri: Uri?,
    onSettingsClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { onProfileClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = profileImageUri,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                            error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().padding(8.dp),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "AI Macro Tracker",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = onSettingsClick) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun CalorieOverview(consumed: Int, goal: Int) {
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    val isOverBudget = consumed > goal
    val arcColor = if (isOverBudget) Color(0xFFC0392B) else MaterialTheme.colorScheme.primary
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            MacroRing(
                progress = progress,
                color = arcColor,
                size = 240.dp,
                strokeWidth = 24.dp,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format(Locale.getDefault(), "%, d", consumed),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp,
                    color = if (isOverBudget) Color(0xFFC0392B) else MaterialTheme.colorScheme.onBackground
                )
                Text(
                    "/ $goal KCAL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = arcColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text(
                        if (isOverBudget) "OVER BUDGET" else "$percentage% GOAL",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = arcColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MacroCard(label: String, value: String, progress: Float, color: Color, modifier: Modifier = Modifier) {
    val isOverBudget = progress >= 1f
    val displayColor = if (isOverBudget) Color(0xFFC0392B) else color

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MacroRing(
                progress = progress.coerceIn(0f, 1f),
                color = displayColor,
                size = 64.dp,
                strokeWidth = 8.dp,
                inactiveColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                label, 
                style = MaterialTheme.typography.labelLarge, 
                color = MaterialTheme.colorScheme.onSurfaceVariant, 
                fontWeight = FontWeight.Bold
            )
            Text(
                value, 
                style = MaterialTheme.typography.titleLarge, 
                color = displayColor, 
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun MacroRing(
    progress: Float,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    strokeWidth: androidx.compose.ui.unit.Dp,
    inactiveColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    Canvas(modifier = Modifier.size(size)) {
        drawArc(
            color = inactiveColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * progress,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
        )
    }

}

@Composable
fun FoodListItemEntity(foodEntity: FoodEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val imageUrl = if (foodEntity.imageUrl.isNullOrBlank() || foodEntity.imageUrl == "null" || foodEntity.imageUrl == "string") {
                "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=200&auto=format&fit=crop"
            } else {
                foodEntity.imageUrl
            }
            AsyncImage(
                model = imageUrl,
                contentDescription = foodEntity.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    foodEntity.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${foodEntity.mealType} • ${foodEntity.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${foodEntity.calories}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "kcal",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FoodListItem(foodItem: FoodItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=200&auto=format&fit=crop",
                contentDescription = foodItem.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                placeholder = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant),
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    foodItem.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${foodItem.meal} • ${foodItem.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${foodItem.calories}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF446180),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

data class FoodItem(val name: String, val meal: String, val time: String, val calories: Int)

val mockFoodItems = listOf(
    FoodItem("Salmon & Quinoa Bowl", "Lunch", "1:30 PM", 485)
)

val sampleFoodEntities = listOf(
    FoodEntity(
        id = 1,
        name = "Oatmeal with Berries",
        mealType = "Breakfast",
        calories = 350,
        protein = 12,
        carbs = 60,
        fat = 8,
        timestamp = System.currentTimeMillis(),
        time = "08:30 AM"
    ),
    FoodEntity(
        id = 2,
        name = "Grilled Chicken Salad",
        mealType = "Lunch",
        calories = 450,
        protein = 35,
        carbs = 15,
        fat = 25,
        timestamp = System.currentTimeMillis(),
        time = "12:45 PM"
    )
)

@Preview(showBackground = true)
@Composable
fun DashboardScreenPreview() {
    MacroTrackerTheme(darkTheme = false) {
        MainScreenContent(
            profileImageUri = null,
            dailyCalorieGoal = 2000,
            proteinGoal = 150,
            carbsGoal = 200,
            fatGoal = 65,
            name = "Alex Johnson",
            height = "182",
            weight = "78.5",
            targetWeight = "75.0",
            bodyFat = "20.0",
            targetBodyFat = "15.0",
            goalPace = "Moderate",
            gender = "Male",
            age = "29",
            activityLevel = "Very Active",
            isDarkMode = false,
            useSystemTheme = true,
            isFirstTime = false,
            apiKey = "",
            selectedModel = "gemini-3-flash-preview",
            onNameChange = {},
            onGenderChange = {},
            onProfileImageChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onTargetWeightChange = {},
            onBodyFatChange = {},
            onTargetBodyFatChange = {},
            onGoalPaceChange = {},
            onAgeChange = {},
            onActivityLevelChange = {},
            onCalorieGoalChange = {},
            onSavePersonalInfo = {},
            onDarkModeChange = {},
            onUseSystemThemeChange = {},
            onApiKeyChange = {},
            onModelChange = {},
            isPersonalInfoExpanded = true,
            onPersonalInfoExpandedChange = {},
            remindersEnabled = true,
            onRemindersEnabledChange = {},
            loggedFoods = sampleFoodEntities,
            historyFoods = sampleFoodEntities,
            historyDate = System.currentTimeMillis(),
            uiState = FoodAssistantUiState.Idle,
            recentMeals = listOf("Oatmeal", "Greek Yogurt"),
            onAnalyzeMeal = { _, _ -> },
            onConfirmMeal = {},
            onResetState = {},
            onDateSelected = {},
            onDeleteMeal = { _, _ -> },
            onFirstTimeFinished = {}
        )
    }

}

@Preview(showBackground = true)
@Composable
fun LogFoodScreenPreview() {
    MacroTrackerTheme {
        LogFoodScreenContent(
            uiState = FoodAssistantUiState.Idle,
            recentMeals = listOf("Oatmeal", "Greek Yogurt"),
            onAnalyzeMeal = { _, _ -> },
            onSuccess = {},
            onResetState = {}
        )
    }

}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    MacroTrackerTheme {
        HistoryScreenContent(
            foods = sampleFoodEntities,
            selectedDate = System.currentTimeMillis(),
            calorieGoal = 2000,
            onDateSelected = {},
            onDeleteMeal = { _, _ -> }
        )
    }

}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MacroTrackerTheme(darkTheme = false) {
        ProfileScreenContent(
            name = "Alex Johnson",
            profileImageUri = null,
            height = "182",
            weight = "78.5",
            targetWeight = "75.0",
            bodyFat = "20.0",
            targetBodyFat = "15.0",
            goalPace = "Moderate",
            gender = "Male",
            age = "29",
            activityLevel = "Very Active",
            isExpanded = true,
            onExpandedChange = {},
            dailyCalorieGoal = 2000,
            proteinGoal = 150,
            carbsGoal = 200,
            fatGoal = 65,
            onNameChange = {},
            onGenderChange = {},
            onProfileImageChange = {},
            onHeightChange = {},
            onWeightChange = {},
            onTargetWeightChange = {},
            onBodyFatChange = {},
            onTargetBodyFatChange = {},
            onGoalPaceChange = {},
            onAgeChange = {},
            onActivityLevelChange = {},
            onCalorieGoalChange = {},
            onSavePersonalInfo = {}
        )
    }

}
