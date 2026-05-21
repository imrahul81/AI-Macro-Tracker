package com.rahul.macrotracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "foods")
data class FoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mealType: String, // Breakfast, Lunch, Snack, Dinner
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val timestamp: Long, // Use for date grouping
    val time: String, // e.g. "08:15 AM"
    val imageUrl: String? = null
)

@Dao
interface FoodDao {
    @Insert
    suspend fun insert(food: FoodEntity)

    @Query("SELECT * FROM foods ORDER BY timestamp DESC")
    fun getAllFoods(): Flow<List<FoodEntity>>

    @Query("SELECT * FROM foods WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp ASC")
    fun getFoodsForDay(startOfDay: Long, endOfDay: Long): Flow<List<FoodEntity>>

    @Query("DELETE FROM foods WHERE mealType = :mealType AND timestamp >= :startOfDay AND timestamp < :endOfDay")
    suspend fun deleteFoodsByMealAndDay(mealType: String, startOfDay: Long, endOfDay: Long)
}

@Database(entities = [FoodEntity::class], version = 2)
abstract class FoodDatabase : RoomDatabase() {
    abstract fun foodDao(): FoodDao

    companion object {
        @Volatile
        private var INSTANCE: FoodDatabase? = null

        fun getDatabase(context: Context): FoodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FoodDatabase::class.java,
                    "food_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
