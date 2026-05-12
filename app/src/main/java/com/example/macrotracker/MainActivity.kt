package com.example.macrotracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.macrotracker.data.FoodEntity
import com.example.macrotracker.ui.theme.MacroTrackerTheme
import java.text.SimpleDateFormat
import java.util.*

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

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object LogFood : Screen("log_food")
    data object History : Screen("history")
    data object Profile : Screen("profile")
}

@Composable
fun MainScreen(viewModel: FoodAssistantViewModel = viewModel()) {
    val loggedFoods by viewModel.loggedFoods.collectAsState()
    val historyFoods by viewModel.historyFoods.collectAsState()
    val uiState = viewModel.uiState
    val recentMeals = viewModel.recentMeals

    MainScreenContent(
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
    loggedFoods: List<FoodEntity>,
    historyFoods: List<FoodEntity>,
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String) -> Unit,
    onConfirmMeal: (MacroResponse) -> Unit,
    onResetState: () -> Unit
) {
    val screenState = remember { mutableStateOf<Screen>(Screen.Dashboard) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = { VitalityTopBar() },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = screenState.value == Screen.Dashboard,
                    onClick = { screenState.value = Screen.Dashboard },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF006D37),
                        indicatorColor = Color(0xFF2ECC71)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AddCircleOutline, contentDescription = "Log Food") },
                    label = { Text("Log Food") },
                    selected = screenState.value == Screen.LogFood,
                    onClick = { screenState.value = Screen.LogFood },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF006D37),
                        indicatorColor = Color(0xFF2ECC71)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("History") },
                    selected = screenState.value == Screen.History,
                    onClick = { screenState.value = Screen.History },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF006D37),
                        indicatorColor = Color(0xFF2ECC71)
                    )
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.PersonOutline, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = screenState.value == Screen.Profile,
                    onClick = { screenState.value = Screen.Profile },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = Color(0xFF006D37),
                        indicatorColor = Color(0xFF2ECC71)
                    )
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { screenState.value = Screen.LogFood },
                containerColor = Color(0xFF2ECC71),
                contentColor = Color.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Food")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (screenState.value) {
                Screen.Dashboard -> MainDashboardContent(loggedFoods)
                Screen.LogFood -> LogFoodScreenContent(
                    uiState = uiState,
                    recentMeals = recentMeals,
                    onAnalyzeMeal = onAnalyzeMeal,
                    onConfirmMeal = onConfirmMeal,
                    onResetState = onResetState
                )
                Screen.History -> HistoryScreenContent(historyFoods)
                else -> Text("Coming Soon")
            }
        }
    }
}

@Composable
fun MainDashboardContent(foods: List<FoodEntity>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFB)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CalorieOverview()
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MacroCard(
                    label = "Protein",
                    value = "120g",
                    progress = 0.7f,
                    color = Color(0xFF006D37),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Carbs",
                    value = "180g",
                    progress = 0.5f,
                    color = Color(0xFF446180),
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    label = "Fats",
                    value = "45g",
                    progress = 0.4f,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreenContent(
    uiState: FoodAssistantUiState,
    recentMeals: List<String>,
    onAnalyzeMeal: (String) -> Unit,
    onConfirmMeal: (MacroResponse) -> Unit,
    onResetState: () -> Unit
) {
    var mealInput by remember { mutableStateOf("") }

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
                        onClick = { /* Mic */ },
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
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D37)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Analyze")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
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

        // Show result overlay
        if (uiState is FoodAssistantUiState.Success) {
            AlertDialog(
                onDismissRequest = { onResetState() },
                confirmButton = {
                    TextButton(onClick = { onConfirmMeal(uiState.macro) }) {
                        Text("Add to Log")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { onResetState() }) {
                        Text("Cancel")
                    }
                },
                title = { Text("Meal Analysis") },
                text = {
                    Column {
                        Text("Name: ${uiState.macro.name}", fontWeight = FontWeight.Bold)
                        Text("Calories: ${uiState.macro.calories} kcal")
                        Text("Protein: ${uiState.macro.protein}g")
                        Text("Carbs: ${uiState.macro.carbs}g")
                        Text("Fat: ${uiState.macro.fat}g")
                    }
                }
            )
        }
        
        if (uiState is FoodAssistantUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
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
fun CalorieOverview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            MacroRing(
                progress = 0.75f,
                color = Color(0xFF2ECC71),
                size = 240.dp,
                strokeWidth = 24.dp,
                inactiveColor = Color(0xFFEEEEEE)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "1,500",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    fontSize = 48.sp
                )
                Text(
                    "/ 2,000 KCAL",
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
                        "75% GOAL",
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF2F4F5))
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                // Image placeholder
            }
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
        // We can't easily preview the LogFood screen with MainScreen because it resets to Dashboard.
        // But we can preview LogFoodScreenContent directly.
        LogFoodScreenContent(
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
fun HistoryScreenPreview() {
    MacroTrackerTheme {
        HistoryScreenContent(foods = sampleFoodEntities)
    }
}
