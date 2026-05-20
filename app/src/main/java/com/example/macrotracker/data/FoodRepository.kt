package com.example.macrotracker.data

import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class FoodRepository(private val foodDao: FoodDao) {
    val allFoods: Flow<List<FoodEntity>> = foodDao.getAllFoods()

    fun getFoodsForToday(): Flow<List<FoodEntity>> {
        return getFoodsForDate(System.currentTimeMillis())
    }

    fun getFoodsForDate(timestamp: Long): Flow<List<FoodEntity>> {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        return foodDao.getFoodsForDay(startOfDay, endOfDay)
    }

    suspend fun insert(food: FoodEntity) {
        foodDao.insert(food)
    }

    suspend fun deleteMeal(mealType: String, dateTimestamp: Long) {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = dateTimestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfDay = calendar.timeInMillis
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000
        foodDao.deleteFoodsByMealAndDay(mealType, startOfDay, endOfDay)
    }
}
