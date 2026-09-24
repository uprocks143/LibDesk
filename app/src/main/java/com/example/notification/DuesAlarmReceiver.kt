package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.entities.StudentEntity
import com.example.data.remote.SupabaseClient
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
                            val (_, sArray) = SupabaseClient.queryTable("students?status=eq.ACTIVE&select=*")
                            val students = mutableListOf<StudentEntity>()
                            if (sArray != null) {
                                for (i in 0 until sArray.length()) {
                                    val obj = sArray.getJSONObject(i)
                                    students.add(
                                        StudentEntity(
                                            id = obj.optString("id", ""),
                                            libraryId = obj.optString("libraryId", ""),
                                            fullName = obj.optString("fullName", ""),
                                            studentCode = obj.optString("studentCode", ""),
                                            mobile = obj.optString("mobile", ""),
                                            email = obj.optString("email", ""),
                                            expiryDate = obj.optString("expiryDate", ""),
                                            dueAmount = obj.optDouble("dueAmount", 0.0),
                                            totalFee = obj.optDouble("totalFee", 0.0),
                                            paidAmount = obj.optDouble("paidAmount", 0.0),
                                            seatNumber = obj.optString("seatNumber", ""),
                                            shiftName = obj.optString("shiftName", ""),
                                            planName = obj.optString("planName", "")
                                        )
                                    )
                                }
                            }
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
