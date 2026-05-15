package com.example.macrotracker

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
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
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import com.example.macrotracker.data.FoodEntity
import com.example.macrotracker.ui.theme.MacroTrackerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MacroTrackerTheme {
                MainScreen()
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.GridView)
    data object LogFood : Screen("log_food", "Log Food", Icons.Default.AddCircleOutline)
    data object History : Screen("history", "History", Icons.Default.History)
    data object Profile : Screen("profile", "Profile", Icons.Default.PersonOutline)
    data object ReviewMeal : Screen("review_meal", "Review Meal", Icons.Default.CheckCircle)
}

@Composable
fun MainScreen(viewModel: FoodAssistantViewModel = viewModel()) {
    val loggedFoods by viewModel.loggedFoods.collectAsState()
    val historyFoods by viewModel.historyFoods.collectAsState()
    val uiState = viewModel.uiState
    val recentMeals = viewModel.recentMeals

    MainScreenContent(
        viewModel = viewModel,
        loggedFoods = loggedFoods,
        historyFoods = historyFoods,
        uiState = uiState,
        recentMeals = recentMeals,
        onAnalyzeMeal = { viewModel.analyzeMeal(it) },
        onConfirmMeal = { viewModel.confirmMeal(it) },
        onResetState = { viewModel.resetState() }
    )
}

@Composable
fun MainScreenContent(
    viewModel: FoodAssistantViewModel,
    loggedFoods: List<FoodEntity>,
    historyFoods: List<FoodEntity>,
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String) -> Unit,
    onConfirmMeal: (MacroResponse) -> Unit,
    onResetState: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { VitalityTopBar() },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val screens = listOf(
                    Screen.Dashboard,
                    Screen.LogFood,
                    Screen.History,
                    Screen.Profile
                )
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = Color(0xFF006D37),
                            indicatorColor = Color(0xFF2ECC71)
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.LogFood.route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                containerColor = Color(0xFF2ECC71),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) {
                MainDashboardContent(loggedFoods)
            }
            composable(Screen.LogFood.route) {
                LogFoodScreenContent(
                    uiState = uiState,
                    recentMeals = recentMeals,
                    onAnalyzeMeal = onAnalyzeMeal,
                    onSuccess = {
                        navController.navigate(Screen.ReviewMeal.route)
                    },
                    onResetState = onResetState
                )
            }
            composable(Screen.ReviewMeal.route) {
                if (uiState is FoodAssistantUiState.Success) {
                    ReviewMealScreenContent(
                        macro = uiState.macro,
                        onConfirmMeal = {
                            onConfirmMeal(it)
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(Screen.Dashboard.route) { inclusive = true }
                            }
                        },
                        onBack = {
                            onResetState()
                            navController.popBackStack()
                        }
                    )
                }
            }
            composable(Screen.History.route) {
                HistoryScreenContent(historyFoods)
            }
            composable(Screen.Profile.route) {
                ProfileScreenContent(viewModel)
            }
        }
    }
}

@Composable
fun MainDashboardContent(foods: List<FoodEntity>) {
    val totalCalories = foods.sumOf { it.calories }
    val totalProtein = foods.sumOf { it.protein }
    val totalCarbs = foods.sumOf { it.carbs }
    val totalFat = foods.sumOf { it.fat }

    // Hardcoded goals for now
    val calorieGoal = 2000
    val proteinGoal = 150
    val carbsGoal = 250
    val fatGoal = 70

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalorieOverview(totalCalories, calorieGoal)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroCard(
                    label = "Protein",
                    value = "${totalProtein}g",
                    progress = (totalProtein.toFloat() / proteinGoal).coerceIn(0f, 1f),
                    color = Color(0xFF006D37),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Carbs",
                    value = "${totalCarbs}g",
                    progress = (totalCarbs.toFloat() / carbsGoal).coerceIn(0f, 1f),
                    color = Color(0xFF446180),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Fats",
                    value = "${totalFat}g",
                    progress = (totalFat.toFloat() / fatGoal).coerceIn(0f, 1f),
                    color = Color(0xFF006397),
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
            items(mockFoodItems) { foodItem ->
                FoodListItem(foodItem)
            }
        } else {
            items(foods) { foodEntity ->
                FoodListItemEntity(foodEntity)
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = true,
                    onClick = { },
                    label = { Text("High Protein") },
                    leadingIcon = { Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFE3F2FD),
                        selectedLabelColor = Color(0xFF1976D2),
                        selectedLeadingIconColor = Color(0xFF1976D2)
                    ),
                    border = null
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Recent") },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFEEEEEE),
                        labelColor = Color.Gray,
                        iconColor = Color.Gray
                    ),
                    border = null
                )
                FilterChip(
                    selected = false,
                    onClick = { },
                    label = { Text("Favorites") },
                    leadingIcon = { Icon(Icons.Default.StarOutline, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    shape = CircleShape,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFFEEEEEE),
                        labelColor = Color.Gray,
                        iconColor = Color.Gray
                    ),
                    border = null
                )
            }
        }
    }
}

@Composable
fun HistoryScreenContent(foods: List<FoodEntity>) {
    val groupedFoods = foods.groupBy { it.mealType }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text("History", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Today, Oct 24",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .background(Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Calendar", tint = Color.Gray)
                    }
                }
            }
        }

        item {
            HistorySummaryCard()
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }

        // Timeline items
        item {
            TimelineItem(
                mealType = "Breakfast",
                time = "08:15 AM",
                calories = 420,
                foods = groupedFoods["Breakfast"] ?: emptyList(),
                icon = Icons.Default.Coffee,
                iconColor = Color(0xFF2ECC71),
                isFirst = true
            )
        }
        item {
            TimelineItem(
                mealType = "Lunch",
                time = "12:45 PM",
                calories = 680,
                foods = groupedFoods["Lunch"] ?: emptyList(),
                icon = Icons.Default.Restaurant,
                iconColor = Color.LightGray
            )
        }
        item {
            TimelineItem(
                mealType = "Afternoon Snack",
                time = "03:30 PM",
                calories = 150,
                foods = groupedFoods["Afternoon Snack"] ?: emptyList(),
                icon = Icons.Default.Fastfood,
                iconColor = Color.LightGray
            )
        }
        item {
            TimelineItem(
                mealType = "Dinner",
                time = "07:00 PM",
                calories = 308,
                foods = groupedFoods["Dinner"] ?: emptyList(),
                icon = Icons.Default.Dining,
                iconColor = Color.LightGray,
                isLast = true
            )
        }
    }
}

@Composable
fun HistorySummaryCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(contentAlignment = Alignment.Center) {
                MacroRing(
                    progress = 0.3f,
                    color = Color(0xFF006D37),
                    size = 100.dp,
                    strokeWidth = 10.dp,
                    inactiveColor = Color(0xFFEEEEEE)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEFT", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    Text("642", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text("EATEN", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append("1,558")
                        }
                        withStyle(SpanStyle(fontSize = 12.sp, color = Color.Gray)) {
                            append(" kcal")
                        }
                    }
                )
            }

            Column {
                Text("GOAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp)) {
                            append("2,200")
                        }
                        withStyle(SpanStyle(fontSize = 12.sp, color = Color.Gray)) {
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
    isLast: Boolean = false
) {
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
                        .background(Color.LightGray)
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (iconColor == Color.LightGray) Color.Gray else Color.White)
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(Color.LightGray)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(mealType, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(time, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF446180))) {
                                    append(calories.toString())
                                }
                                withStyle(SpanStyle(fontSize = 12.sp, color = Color.Gray)) {
                                    append(" kcal")
                                }
                            }
                        )
                    }

                    if (foods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        foods.forEach { food ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFF2F4F5))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(food.name, style = MaterialTheme.typography.bodyMedium)
                                }
                                Text("${food.calories} kcal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
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
                            
                            MacroBadge("Protein", "${totalProtein}g", Color(0xFFE3F2FD), Color(0xFF1976D2))
                            MacroBadge("Carbs", "${totalCarbs}g", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                            MacroBadge("Fats", "${totalFat}g", Color(0xFFFFF3E0), Color(0xFFE65100))
                        }
                    } else {
                         Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color.Gray,
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun ProfileScreenContent(viewModel: FoodAssistantViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isEditing by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                // Not a persistable URI, but we'll still try to use it
            }
            viewModel.profileImageUri = it
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            ProfileHeader(
                imageUri = viewModel.profileImageUri,
                onEditImage = { 
                    imagePickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
        item {
            PersonalInfoSection(
                height = viewModel.height,
                weight = viewModel.weight,
                age = viewModel.age,
                activityLevel = viewModel.activityLevel,
                isEditing = isEditing,
                onEditClick = { isEditing = !isEditing },
                onHeightChange = { viewModel.height = it },
                onWeightChange = { viewModel.weight = it },
                onAgeChange = { viewModel.age = it },
                onActivityLevelChange = { viewModel.activityLevel = it }
            )
        }
        item {
            NutritionalGoalsSection()
        }
        item {
            AccountSection()
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
    }
}

@Composable
fun ProfileHeader(imageUri: android.net.Uri?, onEditImage: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .padding(4.dp)
                    .background(Color(0xFF2ECC71), CircleShape)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                if (imageUri != null) {
                    AsyncImage(
                        model = imageUri,
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                color = Color(0xFF006D37),
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
        Text(
            "Alex Johnson",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            color = Color(0xFF2ECC71),
            shape = CircleShape
        ) {
            Text(
                "Pro Member",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PersonalInfoSection(
    height: String,
    weight: String,
    age: String,
    activityLevel: String,
    isEditing: Boolean,
    onEditClick: () -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onActivityLevelChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personal Info", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                TextButton(onClick = onEditClick) {
                    Text(if (isEditing) "Save" else "Edit", color = Color(0xFF006D37), fontWeight = FontWeight.Bold)
                }
            }
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
                    label = "Age",
                    value = age,
                    unit = "years",
                    isEditing = isEditing,
                    onValueChange = onAgeChange,
                    modifier = Modifier.weight(1f)
                )
                ActivityLevelDropdown(
                    value = activityLevel,
                    isEditing = isEditing,
                    onValueChange = onActivityLevelChange,
                    modifier = Modifier.weight(1f)
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
        Text("Activity Level", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
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
                        .menuAnchor()
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F4F5),
                        unfocusedContainerColor = Color(0xFFF2F4F5),
                        focusedIndicatorColor = Color(0xFF006D37)
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(Color.White)
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
                color = Color(0xFFF2F4F5),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = value,
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
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
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        if (isEditing) {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF2F4F5),
                    unfocusedContainerColor = Color(0xFFF2F4F5),
                    focusedIndicatorColor = Color(0xFF006D37)
                )
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF2F4F5),
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
fun NutritionalGoalsSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Nutritional Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Tune, contentDescription = "Edit Goals", tint = Color(0xFF006D37))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF2F4F5),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFF006D37))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Calories", style = MaterialTheme.typography.bodyLarge)
                    }
                    Text("2,850 kcal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color(0xFF446180))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroGoalBadge("Protein", "180g", Color(0xFFE3F2FD), Color(0xFF1976D2), modifier = Modifier.weight(1f))
                MacroGoalBadge("Carbs", "320g", Color(0xFFE8F5E9), Color(0xFF2E7D32), modifier = Modifier.weight(1f))
                MacroGoalBadge("Fats", "75g", Color(0xFFF8FAFB), Color(0xFF446180), modifier = Modifier.weight(1f))
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
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = textColor)
        }
    }
}

@Composable
fun AccountSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                "Account",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AccountItem(label = "Email Address", value = "alex.j@example.com")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F4F5))
            AccountItem(label = "Password", value = "••••••••••••")
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color(0xFFF2F4F5))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color(0xFF446180))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Push Notifications", style = MaterialTheme.typography.bodyLarge)
                }
                var notificationsEnabled by remember { mutableStateOf(true) }
                Switch(
                    checked = notificationsEnabled,
                    onCheckedChange = { notificationsEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF2ECC71))
                )
            }
        }
    }
}

@Composable
fun AccountItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreenContent(
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String) -> Unit,
    onSuccess: () -> Unit,
    onResetState: () -> Unit
) {
    var mealInput by remember { mutableStateOf("") }

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

    LaunchedEffect(uiState) {
        if (uiState is FoodAssistantUiState.Success) {
            onSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB))
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
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF2ECC71),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "AI Food Assistant",
                            color = Color(0xFF006D37),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Describe your meal...")
                            }
                            speechLauncher.launch(intent)
                        },
                        modifier = Modifier
                            .background(Color(0xFFE3F2FD), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.MicNone, contentDescription = "Voice", tint = Color(0xFF1976D2))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = mealInput,
                    onValueChange = { mealInput = it },
                    placeholder = {
                        Text(
                            "e.g., I had two eggs and a piece of whole grain toast with a small avocado for breakfast...",
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF2F4F5),
                        unfocusedContainerColor = Color(0xFFF2F4F5),
                        disabledContainerColor = Color(0xFFF2F4F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { onAnalyzeMeal(mealInput) },
                    modifier = Modifier.align(Alignment.End),
                    enabled = uiState !is FoodAssistantUiState.Loading,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D37)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    if (uiState is FoodAssistantUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyzing...")
                    } else {
                        Text("Analyze")
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                icon = Icons.Default.Lightbulb,
                title = "Be Specific",
                description = "Mention portion sizes like 'a handful' or 'half a plate' for better accuracy.",
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFFE3F2FD),
                contentColor = Color(0xFF1976D2)
            )
            RecentMealsCard(
                meals = recentMeals,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.LightGray)
        ) {
            // Background Image would go here
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text("AI Vision", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text("Snap a photo to let AI identify the nutrients instantly.", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(
                onClick = { /* Camera */ },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Camera", tint = Color.White)
            }
        }

        // Show result overlay - REMOVED, replaced by navigation to ReviewMeal
        
        if (uiState is FoodAssistantUiState.Error) {
            AlertDialog(
                onDismissRequest = onResetState,
                title = { Text("Analysis Failed") },
                text = { Text(uiState.message) },
                confirmButton = {
                    TextButton(onClick = onResetState) {
                        Text("Retry")
                    }
                }
            )
        }
    }
}

@Composable
fun ReviewMealScreenContent(
    macro: MacroResponse,
    onConfirmMeal: (MacroResponse) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB))
    ) {
        // Custom Top Bar for Review Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF006D37))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "VitalityTrack",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color(0xFF006D37),
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                // Image placeholder
                Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.padding(8.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    "Review Meal",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "\"${macro.originalInput}\"",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
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
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TOTAL CALORIES", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                macro.totalCalories.toString(),
                                style = MaterialTheme.typography.displayMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF006D37)
                            )
                            Text("kcal", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroSummaryBadge("Protein", "${macro.totalProtein}g", Color(0xFFE8F5E9), Color(0xFF2E7D32))
                        MacroSummaryBadge("Carbs", "${macro.totalCarbs}g", Color(0xFFE3F2FD), Color(0xFF1976D2))
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

            items(macro.items) { item ->
                DetectedItemCard(item)
            }

            item {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add another item")
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFE3F2FD).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF1976D2))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Did you add any butter or oil to your toast or eggs? Tapping an item lets you add condiments.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1976D2)
                        )
                    }
                }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 8.dp,
            color = Color.White
        ) {
            Button(
                onClick = { onConfirmMeal(macro) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D37)),
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .background(Color(0xFFF2F4F5)),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(item.description, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    }
                }
                IconButton(onClick = { }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray)
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF2F4F5))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CAL", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
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
fun RecentMealsCard(meals: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F5)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(Icons.Default.History, contentDescription = null, tint = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Recent Meals", fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            meals.forEach { meal ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        meal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun VitalityTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
            ) {
                // Placeholder for profile image
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                "AI Macro Tracker",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFF006D37),
                fontWeight = FontWeight.Bold
            )
        }
        IconButton(onClick = { }) {
            Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color(0xFF006D37))
        }
    }
}

@Composable
fun CalorieOverview(consumed: Int, goal: Int) {
    val progress = (consumed.toFloat() / goal).coerceIn(0f, 1f)
    val percentage = (progress * 100).toInt()
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            MacroRing(
                progress = progress,
                color = Color(0xFF2ECC71),
                size = 240.dp,
                strokeWidth = 24.dp,
                inactiveColor = Color(0xFFEEEEEE)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    String.format("%, d", consumed),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )
                Text(
                    "/ $goal KCAL",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFF2ECC71).copy(alpha = 0.1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        "$percentage% GOAL",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = Color(0xFF006D37),
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
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MacroRing(
                progress = progress,
                color = color,
                size = 60.dp,
                strokeWidth = 8.dp,
                inactiveColor = Color(0xFFEEEEEE)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, style = MaterialTheme.typography.titleLarge, color = color, fontWeight = FontWeight.Bold)
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = foodEntity.imageUrl ?: "https://images.unsplash.com/photo-1546069901-ba9599a7e63c?q=80&w=200&auto=format&fit=crop",
                contentDescription = foodEntity.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F4F5)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    foodEntity.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${foodEntity.mealType} • ${foodEntity.time}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${foodEntity.calories}",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF446180),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
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
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    .background(Color(0xFFEEEEEE)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
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
                    color = Color.Gray
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
                    color = Color.Gray
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
    MacroTrackerTheme {
        MainScreenContent(
            viewModel = viewModel(),
            loggedFoods = sampleFoodEntities,
            historyFoods = sampleFoodEntities,
            uiState = FoodAssistantUiState.Idle,
            recentMeals = listOf("Oatmeal", "Greek Yogurt"),
            onAnalyzeMeal = {},
            onConfirmMeal = {},
            onResetState = {}
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
            onAnalyzeMeal = {},
            onSuccess = {},
            onResetState = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HistoryScreenPreview() {
    MacroTrackerTheme {
        HistoryScreenContent(foods = sampleFoodEntities)
    }
}

@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview() {
    MacroTrackerTheme {
        // Mock ViewModel for preview
        ProfileScreenContent(viewModel())
    }
}
