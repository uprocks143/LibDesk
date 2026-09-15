package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DuesAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_CHECK_DUES_ALERT = "com.example.ACTION_CHECK_DUES_ALERT"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {

                if (DuesNotificationHelper.isDailyAlertEnabled(context)) {
                    val (hour, minute) = DuesNotificationHelper.getAlertTime(context)
                    DuesNotificationHelper.scheduleDailyDuesAlarm(context, hour, minute)
                }
            }

            ACTION_CHECK_DUES_ALERT -> {

                if (DuesNotificationHelper.isDailyAlertEnabled(context)) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getInstance(context)
                            val students = db.studentDao().getAllStudentsDirect()
                            val thresholdDays = DuesNotificationHelper.getThresholdDays(context)

                            DuesNotificationHelper.triggerDuesNotification(
                                context = context,
                                students = students,
                                thresholdDays = thresholdDays,
                                isTest = false
                            )

                            
                            val (hour, minute) = DuesNotificationHelper.getAlertTime(context)
                            DuesNotificationHelper.scheduleDailyDuesAlarm(context, hour, minute)
                        } catch (_: Exception) {
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
