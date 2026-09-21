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
import com.example.data.local.entities.ShiftEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.MembershipAlertLevel
import com.example.ui.components.calculateMembershipExpiration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Local Notification System to send reminders to students:
 * 1. When their membership is about to expire or has expired
 * 2. When their seat reservation / scheduled shift starts
 */
object StudentNotificationHelper {
    const val CHANNEL_ID_STUDENT_MEMBERSHIP = "libdesk_student_membership_expiry_channel"
    const val CHANNEL_NAME_STUDENT_MEMBERSHIP = "Student Membership Expiry Reminders"
    const val CHANNEL_DESC_STUDENT_MEMBERSHIP = "Timely reminders when library desk membership is nearing expiration"

    const val CHANNEL_ID_STUDENT_SEAT = "libdesk_student_seat_reservation_channel"
    const val CHANNEL_NAME_STUDENT_SEAT = "Seat Reservation & Shift Reminders"
    const val CHANNEL_DESC_STUDENT_SEAT = "Notifications when your reserved library seat shift begins"

    const val NOTIFICATION_ID_EXPIRY_BASE = 9100
    const val NOTIFICATION_ID_SEAT_BASE = 9200
    const val NOTIFICATION_ID_TEST_EXPIRY = 9901
    const val NOTIFICATION_ID_TEST_SEAT = 9902

    const val ACTION_OPEN_STUDENT_PORTAL = "com.example.ACTION_OPEN_STUDENT_PORTAL"
    const val ACTION_OPEN_QR_SCANNER = "com.example.ACTION_OPEN_QR_SCANNER"
    const val EXTRA_NOTIFICATION_ACTION = "extra_notification_action"

    private const val PREFS_NAME = "libdesk_student_notification_prefs"
    private const val KEY_EXPIRY_REMINDERS_ENABLED = "key_student_expiry_reminders_enabled"
    private const val KEY_SEAT_REMINDERS_ENABLED = "key_student_seat_reminders_enabled"
    private const val KEY_LAST_EXPIRY_NOTIFIED_DATE = "key_last_expiry_notified_date"
    private const val KEY_LAST_SEAT_NOTIFIED_DATE = "key_last_seat_notified_date"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isExpiryReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_EXPIRY_REMINDERS_ENABLED, true)
    }

    fun setExpiryReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_EXPIRY_REMINDERS_ENABLED, enabled).apply()
    }

    fun isSeatReminderEnabled(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_SEAT_REMINDERS_ENABLED, true)
    }

    fun setSeatReminderEnabled(context: Context, enabled: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_SEAT_REMINDERS_ENABLED, enabled).apply()
    }

    /**
     * Initializes both student notification channels.
     */
    fun initChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val expiryChannel = NotificationChannel(
                CHANNEL_ID_STUDENT_MEMBERSHIP,
                CHANNEL_NAME_STUDENT_MEMBERSHIP,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_STUDENT_MEMBERSHIP
                enableLights(true)
                lightColor = Color.parseColor("#E11D48") // Alert Rose
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 150, 300)
                setShowBadge(true)
            }

            val seatChannel = NotificationChannel(
                CHANNEL_ID_STUDENT_SEAT,
                CHANNEL_NAME_STUDENT_SEAT,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_STUDENT_SEAT
                enableLights(true)
                lightColor = Color.parseColor("#0284C7") // Sky Blue
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                setShowBadge(true)
            }

            notificationManager.createNotificationChannel(expiryChannel)
            notificationManager.createNotificationChannel(seatChannel)
        }
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Sends a reminder notification to a student when their membership is about to expire or has expired.
     */
    fun sendMembershipExpiryReminder(
        context: Context,
        student: StudentEntity,
        libraryName: String = "Your Library",
        force: Boolean = false
    ): Boolean {
        if (!force && !isExpiryReminderEnabled(context)) return false
        if (!hasNotificationPermission(context)) return false

        initChannels(context)

        val expInfo = calculateMembershipExpiration(
            startDateStr = student.joiningDate,
            expiryDateStr = student.expiryDate,
            rawStatus = student.status
        )

        // Only notify if expiring soon, critical, today, or expired (unless forced for test)
        if (!force && expInfo.level == MembershipAlertLevel.ACTIVE) {
            return false
        }

        val notificationId = if (force) {
            NOTIFICATION_ID_TEST_EXPIRY
        } else {
            NOTIFICATION_ID_EXPIRY_BASE + abs(student.id.hashCode() % 500)
        }

        // Tap intent opens app
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_STUDENT_PORTAL
            putExtra(EXTRA_NOTIFICATION_ACTION, "MEMBERSHIP_RENEWAL")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seatTag = if (student.seatNumber.isNotBlank()) "Seat ${student.seatNumber}" else "Study Desk"
        val title = when (expInfo.level) {
            MembershipAlertLevel.EXPIRED -> "🚫 Membership Expired: $seatTag"
            MembershipAlertLevel.EXPIRING_TODAY -> "⚠️ Membership Expires Today! ($seatTag)"
            MembershipAlertLevel.EXPIRING_CRITICAL -> "⏳ Expiring in ${expInfo.daysRemaining} Days ($seatTag)"
            MembershipAlertLevel.EXPIRING_SOON -> "🔔 Membership Renewal Due Soon ($seatTag)"
            else -> "📅 Membership Status Notice ($seatTag)"
        }

        val body = when (expInfo.level) {
            MembershipAlertLevel.EXPIRED ->
                "Hi ${student.fullName}, your membership ended on ${expInfo.formattedExpiryDate}. Renew your plan now to retain your reserved seat."
            MembershipAlertLevel.EXPIRING_TODAY ->
                "Hi ${student.fullName}, your membership expires today! Please renew now to prevent automatic seat release."
            MembershipAlertLevel.EXPIRING_CRITICAL ->
                "Hi ${student.fullName}, only ${expInfo.daysRemaining} days left on your ${student.planName.ifEmpty { "library" }} plan (Valid till ${expInfo.formattedExpiryDate})."
            else ->
                "Hi ${student.fullName}, your membership at $libraryName expires in ${expInfo.daysRemaining} days. Tap to review renewal options."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_STUDENT_MEMBERSHIP)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
                    .setSummaryText(libraryName)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_red_dark))
            .addAction(
                android.R.drawable.ic_menu_view,
                "Renew Membership",
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            getPrefs(context).edit().putString(KEY_LAST_EXPIRY_NOTIFIED_DATE, todayStr).apply()
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    /**
     * Sends a reminder notification to a student when their reserved seat shift begins.
     */
    fun sendSeatReservationStartReminder(
        context: Context,
        student: StudentEntity,
        shift: ShiftEntity? = null,
        libraryName: String = "Your Library",
        force: Boolean = false
    ): Boolean {
        if (!force && !isSeatReminderEnabled(context)) return false
        if (!hasNotificationPermission(context)) return false

        initChannels(context)

        val notificationId = if (force) {
            NOTIFICATION_ID_TEST_SEAT
        } else {
            NOTIFICATION_ID_SEAT_BASE + abs(student.id.hashCode() % 500)
        }

        // Tap intent opens app directly to the QR attendance scanner
        val qrIntent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_OPEN_QR_SCANNER
            putExtra(EXTRA_NOTIFICATION_ACTION, "SCAN_ATTENDANCE")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            qrIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val seatTag = if (student.seatNumber.isNotBlank()) "Seat ${student.seatNumber}" else "Your Reserved Desk"
        val shiftTitle = shift?.name ?: student.shiftName.ifEmpty { "Daily Study Session" }
        val shiftTiming = if (shift != null) " (${shift.startTime} - ${shift.endTime})" else ""

        val title = "🪑 Seat Reservation Starting: $seatTag"
        val body = "Hi ${student.fullName}, your $shiftTitle$shiftTiming has started! Open the QR scanner to punch attendance and check in to your desk."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID_STUDENT_SEAT)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .setBigContentTitle(title)
                    .bigText(body)
                    .setSummaryText(libraryName)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setColor(ContextCompat.getColor(context, android.R.color.holo_blue_dark))
            .addAction(
                android.R.drawable.ic_menu_camera,
                "Scan QR to Check In",
                pendingIntent
            )

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            getPrefs(context).edit().putString(KEY_LAST_SEAT_NOTIFIED_DATE, todayStr).apply()
            return true
        } catch (_: SecurityException) {
            return false
        }
    }

    /**
     * Schedules a daily alarm at the student's shift start time to trigger a shift reminder.
     */
    @SuppressLint("ScheduleExactAlarm")
    fun scheduleSeatShiftReminderAlarm(
        context: Context,
        student: StudentEntity,
        shift: ShiftEntity?
    ) {
        val shiftTimeStr = shift?.startTime ?: "07:00 AM"
        val (hour, minute) = parseHourAndMinute(shiftTimeStr)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, StudentReminderAlarmReceiver::class.java).apply {
            action = StudentReminderAlarmReceiver.ACTION_STUDENT_SHIFT_REMINDER
            putExtra(StudentReminderAlarmReceiver.EXTRA_STUDENT_ID, student.id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            9301 + abs(student.id.hashCode() % 100),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
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
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        } catch (_: Exception) {}
    }

    private fun parseHourAndMinute(timeStr: String): Pair<Int, Int> {
        val formats = arrayOf("hh:mm a", "h:mm a", "HH:mm", "H:mm")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                val d = sdf.parse(timeStr.trim())
                if (d != null) {
                    val cal = Calendar.getInstance().apply { time = d }
                    return Pair(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                }
            } catch (_: Exception) {}
        }
        return Pair(8, 0)
    }

    /**
     * Sends both sample notifications immediately for testing and instant user feedback.
     */
    fun sendTestReminders(
        context: Context,
        student: StudentEntity,
        shift: ShiftEntity? = null,
        libraryName: String = "Your Library"
    ): Pair<Boolean, Boolean> {
        val expirySuccess = sendMembershipExpiryReminder(
            context = context,
            student = student,
            libraryName = libraryName,
            force = true
        )
        val seatSuccess = sendSeatReservationStartReminder(
            context = context,
            student = student,
            shift = shift,
            libraryName = libraryName,
            force = true
        )
        return Pair(expirySuccess, seatSuccess)
    }
}
