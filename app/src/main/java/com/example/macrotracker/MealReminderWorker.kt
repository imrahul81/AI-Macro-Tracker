package com.example.macrotracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.macrotracker.data.FoodDatabase
import com.example.macrotracker.data.FoodRepository
import kotlinx.coroutines.flow.first

class MealReminderWorker(
    context: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val mealType = inputData.getString("meal_type") ?: return Result.failure()
        
        if (mealType == "Final Reminder") {
            // Check if user logged anything today
            val db = FoodDatabase.getDatabase(applicationContext)
            val repository = FoodRepository(db.foodDao())
            val todayFoods = repository.getFoodsForToday().first()
            
            if (todayFoods.isNotEmpty()) {
                // User already logged something, no need for final reminder
                return Result.success()
            }
        }

        showNotification(mealType)
        return Result.success()
    }

    private fun showNotification(mealType: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "meal_reminders"

        val channel = NotificationChannel(
            channelId,
            "Meal Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to log your meals"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (mealType) {
            "Final Reminder" -> "Don't forget to log!"
            else -> "Time for $mealType!"
        }
        
        val message = when (mealType) {
            "Final Reminder" -> "You haven't logged any meals today. Take a moment to track your nutrition."
            else -> "It's around $mealType time. Stay on track with your goals by logging your meal now."
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            notificationManager.notify(mealType.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission not granted
        }
    }
}
