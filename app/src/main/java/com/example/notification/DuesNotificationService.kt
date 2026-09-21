package com.example.notification

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entities.StudentEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DuesAlertInfo(
    val student: StudentEntity,
    val daysRemaining: Int,
    val isExpired: Boolean,
    val isExpiringToday: Boolean,
    val isExpiringSoon: Boolean,
    val dueAmount: Double
)

object DuesNotificationHelper {
    const val CHANNEL_ID_DUES = "libdesk_dues_alerts_channel"
    const val CHANNEL_NAME = "Membership Dues & Expirations"
    const val CHANNEL_DESCRIPTION = "Daily alerts and reminders for students with upcoming fee renewals and expiration dates"
    const val NOTIFICATION_ID_DUES_SUMMARY = 7001
    const val NOTIFICATION_ID_TEST_ALERT = 7002

    const val ACTION_OPEN_NOTIFICATIONS = "com.example.ACTION_OPEN_NOTIFICATIONS"
    const val EXTRA_OPEN_TAB = "extra_open_tab"

    private const val PREFS_NAME = "libdesk_dues_notification_prefs"
    private const val KEY_DAILY_ALERTS_ENABLED = "key_daily_dues_alerts_enabled"
    private const val KEY_THRESHOLD_DAYS = "key_dues_threshold_days"
    private const val KEY_ALERT_HOUR = "key_dues_alert_hour"
    private const val KEY_ALERT_MINUTE = "key_dues_alert_minute"
    private const val KEY_LAST_ALERT_TIMESTAMP = "key_last_alert_timestamp"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isDailyAlertEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_DAILY_ALERTS_ENABLED, true)
    }

    fun setDailyAlertEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_DAILY_ALERTS_ENABLED, enabled).apply()
        if (enabled) {
            val (h, m) = getAlertTime(context)
            scheduleDailyDuesAlarm(context, h, m)
        } else {
            cancelDailyDuesAlarm(context)
        }
    }

    fun getThresholdDays(context: Context): Int {
        return getPrefs(context).getInt(KEY_THRESHOLD_DAYS, 3)
    }

    fun setThresholdDays(context: Context, days: Int) {
        getPrefs(context).edit().putInt(KEY_THRESHOLD_DAYS, days).apply()
    }

    fun getAlertTime(context: Context): Pair<Int, Int> {
        val prefs = getPrefs(context)
        val hour = prefs.getInt(KEY_ALERT_HOUR, 9)
        val minute = prefs.getInt(KEY_ALERT_MINUTE, 0)
        return Pair(hour, minute)
    }

    fun setAlertTime(context: Context, hour: Int, minute: Int) {
        getPrefs(context).edit()
            .putInt(KEY_ALERT_HOUR, hour)
            .putInt(KEY_ALERT_MINUTE, minute)
            .apply()
        if (isDailyAlertEnabled(context)) {
            scheduleDailyDuesAlarm(context, hour, minute)
        }
    }

    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_DUES,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                lightColor = Color.parseColor("#0F172A")
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun parseDaysRemaining(expiryDateStr: String): Int {
        if (expiryDateStr.isBlank()) return 30
        val formats = arrayOf("yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd/MM/yyyy")
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(expiryDateStr.trim())
                if (date != null) {
                    val calTarget = Calendar.getInstance().apply {
                        time = date
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val calToday = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val diffMs = calTarget.timeInMillis - calToday.timeInMillis
                    return (diffMs / (1000 * 60 * 60 * 24)).toInt()
                }
            } catch (_: Exception) { }
        }
        return 30
    }

    fun getExpiringAndDueStudents(students: List<StudentEntity>, thresholdDays: Int = 3): List<DuesAlertInfo> {
        val result = mutableListOf<DuesAlertInfo>()
        for (student in students) {
            val days = parseDaysRemaining(student.expiryDate)
            val isExpiredStatus = student.status.equals("EXPIRED", ignoreCase = true) || days < 0
            val isExpiringToday = days == 0
            val isExpiringSoon = days in 1..thresholdDays && !isExpiredStatus
            val hasDue = student.dueAmount > 0

            if (isExpiredStatus || isExpiringToday || isExpiringSoon || hasDue) {
                result.add(
                    DuesAlertInfo(
                        student = student,
                        daysRemaining = days,
                        isExpired = isExpiredStatus,
                        isExpiringToday = isExpiringToday,
                        isExpiringSoon = isExpiringSoon,
                        dueAmount = student.dueAmount
                    )
                )
            }
        }

        
        result.sortWith(
            compareByDescending<DuesAlertInfo> { it.isExpired }
                .thenByDescending { it.isExpiringToday }
                .thenByDescending { it.isExpiringSoon }
                .thenByDescending { it.dueAmount }
        )
        return result
    }

    fun triggerDuesNotification(
        context: Context,
        students: List<StudentEntity>,
        thresholdDays: Int = getThresholdDays(context),
        isTest: Boolean = false
    ) {
        initNotificationChannel(context)

        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val alertItems = getExpiringAndDueStudents(students, thresholdDays)
        val notificationId = if (isTest) NOTIFICATION_ID_TEST_ALERT else NOTIFICATION_ID_DUES_SUMMARY

        
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_NOTIFICATIONS
            putExtra(EXTRA_OPEN_TAB, "RENEWALS")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalPendingAmount = alertItems.sumOf { it.dueAmount }
        val expiringCount = alertItems.count { it.isExpiringSoon || it.isExpiringToday }
        val expiredCount = alertItems.count { it.isExpired }
        val duesOnlyCount = alertItems.count { !it.isExpired && !it.isExpiringSoon && !it.isExpiringToday && it.dueAmount > 0 }

        val title = when {
            isTest -> "🔔 [Test Alert] Daily Dues & Renewal Follow-Up"
            expiredCount > 0 && expiringCount > 0 -> "⚠️ ${alertItems.size} Students: Urgent Expirations & Fee Dues"
            expiredCount > 0 -> "⚠️ $expiredCount Memberships Expired — Follow-Up Required"
            expiringCount > 0 -> "🔔 $expiringCount Students Nearing Expiration ($thresholdDays days)"
            duesOnlyCount > 0 -> "💰 $duesOnlyCount Students with Pending Dues"
            else -> "✓ LibDesk: All Membership Dues Clear"
        }

        val summaryText = if (alertItems.isNotEmpty()) {
            "Total Pending: ₹${totalPendingAmount.toInt()} across ${alertItems.size} student${if (alertItems.size > 1) "s" else ""}"
        } else {
            "No students currently require expiration or fee follow-up."
        }

        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(title)
            .setSummaryText(summaryText)

        if (alertItems.isEmpty()) {
            inboxStyle.addLine("✓ All active student memberships and payments are up to date.")
        } else {

            alertItems.take(6).forEach { item ->
                val statusLabel = when {
                    item.isExpired -> "EXPIRED"
                    item.isExpiringToday -> "Expires TODAY"
                    item.daysRemaining > 0 -> "${item.daysRemaining}d left"
                    else -> "Due"
                }
                val dueStr = if (item.dueAmount > 0) " | Due: ₹${item.dueAmount.toInt()}" else ""
                val seatStr = if (item.student.seatNumber.isNotBlank()) " (Seat ${item.student.seatNumber})" else ""
                inboxStyle.addLine("• ${item.student.fullName}$seatStr — $statusLabel$dueStr")
            }
            if (alertItems.size > 6) {
                inboxStyle.addLine("+ ${alertItems.size - 6} more students pending review...")
            }
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_DUES)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(summaryText)
            .setStyle(inboxStyle)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Dues Center",
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            getPrefs(context).edit().putLong(KEY_LAST_ALERT_TIMESTAMP, System.currentTimeMillis()).apply()
        } catch (_: SecurityException) {

        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleDailyDuesAlarm(context: Context, hourOfDay: Int = 9, minute: Int = 0) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DuesAlarmReceiver::class.java).apply {
            action = DuesAlarmReceiver.ACTION_CHECK_DUES_ALERT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    AlarmManager.INTERVAL_DAY,
                    pendingIntent
                )
            }
        } catch (_: Exception) {

            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    fun cancelDailyDuesAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, DuesAlarmReceiver::class.java).apply {
            action = DuesAlarmReceiver.ACTION_CHECK_DUES_ALERT
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            8001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
