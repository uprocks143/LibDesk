package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.ShiftEntity
import com.example.data.remote.SupabaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for scheduled student notifications (membership expiry & shift start).
 */
class StudentReminderAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_STUDENT_SHIFT_REMINDER = "com.example.ACTION_STUDENT_SHIFT_REMINDER"
        const val ACTION_STUDENT_EXPIRY_CHECK = "com.example.ACTION_STUDENT_EXPIRY_CHECK"
        const val EXTRA_STUDENT_ID = "extra_student_id"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val studentId = intent.getStringExtra(EXTRA_STUDENT_ID)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    "android.intent.action.QUICKBOOT_POWERON" -> {
                        // Re-schedule alarms after device reboot
                    }

                    ACTION_STUDENT_SHIFT_REMINDER -> {
                        if (StudentNotificationHelper.isSeatReminderEnabled(context) && !studentId.isNullOrBlank()) {
                            val (_, sArray) = SupabaseClient.queryTable("students?id=eq.$studentId&select=*")
                            val sObj = sArray?.optJSONObject(0)
                            if (sObj != null) {
                                val student = StudentEntity(
                                    id = sObj.optString("id", studentId),
                                    libraryId = sObj.optString("libraryId", ""),
                                    fullName = sObj.optString("fullName", "Student"),
                                    studentCode = sObj.optString("studentCode", ""),
                                    mobile = sObj.optString("mobile", ""),
                                    seatNumber = sObj.optString("seatNumber", ""),
                                    shiftName = sObj.optString("shiftName", "")
                                )
                                StudentNotificationHelper.sendSeatReservationStartReminder(
                                    context = context,
                                    student = student,
                                    shift = null,
                                    libraryName = "Your Library",
                                    force = false
                                )
                            }
                        }
                    }

                    ACTION_STUDENT_EXPIRY_CHECK -> {
                        if (StudentNotificationHelper.isExpiryReminderEnabled(context) && !studentId.isNullOrBlank()) {
                            val (_, sArray) = SupabaseClient.queryTable("students?id=eq.$studentId&select=*")
                            val sObj = sArray?.optJSONObject(0)
                            if (sObj != null) {
                                val student = StudentEntity(
                                    id = sObj.optString("id", studentId),
                                    libraryId = sObj.optString("libraryId", ""),
                                    fullName = sObj.optString("fullName", "Student"),
                                    studentCode = sObj.optString("studentCode", ""),
                                    mobile = sObj.optString("mobile", ""),
                                    expiryDate = sObj.optString("expiryDate", "")
                                )
                                StudentNotificationHelper.sendMembershipExpiryReminder(
                                    context = context,
                                    student = student,
                                    libraryName = "Your Library",
                                    force = false
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}
