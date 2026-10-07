package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.net.URLEncoder

object WhatsAppAlertUtils {

    /**
     * Sanitizes phone number to standard international format (defaults to India 91 if 10 digits).
     */
    fun formatPhoneNumberForWhatsApp(phone: String): String {
        val clean = phone.replace("[^0-9]".toRegex(), "")
        return when {
            clean.length == 10 -> "91$clean"
            clean.startsWith("91") && clean.length == 12 -> clean
            clean.startsWith("0") && clean.length == 11 -> "91" + clean.substring(1)
            else -> clean
        }
    }

    /**
     * Launches WhatsApp with a pre-filled encoded message.
     */
    fun openWhatsApp(context: Context, phone: String, message: String) {
        try {
            val formattedPhone = formatPhoneNumberForWhatsApp(phone)
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Pre-built 1-Tap Fee Due Reminder Alert Template with Direct UPI Payment Link & QR format
     */
    fun sendFeeDueReminder(
        context: Context,
        studentName: String,
        phone: String,
        libraryName: String,
        dueAmount: Double,
        dueDate: String,
        upiId: String = ""
    ) {
        val msg = buildString {
            append("🔔 *Fee Due Reminder - $libraryName*\n\n")
            append("Dear *$studentName*,\n")
            append("This is a friendly reminder regarding your pending library subscription fee.\n\n")
            append("💰 *Pending Due Amount:* ₹${String.format("%.0f", dueAmount)}\n")
            append("📅 *Due Date:* $dueDate\n")
            if (upiId.isNotBlank()) {
                val cleanLib = libraryName.replace(" ", "%20")
                val upiPaymentLink = "upi://pay?pa=$upiId&pn=$cleanLib&am=${String.format("%.0f", dueAmount)}&cu=INR"
                append("📲 *UPI Payment ID:* `$upiId`\n")
                append("🔗 *Instant Pay Link:* $upiPaymentLink\n")
            }
            append("\nPlease complete the payment on or before the due date to avoid seat deallocation.\n\n")
            append("For any queries, please reply directly to this message.\n\n")
            append("Regards,\n*$libraryName Administration* 📚")
        }
        openWhatsApp(context, phone, msg)
    }

    /**
     * Pre-built 1-Tap Payment Receipt Confirmation Alert
     */
    fun sendFeeReceiptAlert(
        context: Context,
        studentName: String,
        phone: String,
        libraryName: String,
        receiptNumber: String,
        amountPaid: Double,
        period: String,
        remainingDue: Double = 0.0
    ) {
        val msg = buildString {
            append("🧾 *Payment Receipt - $libraryName*\n\n")
            append("Dear *$studentName*,\n")
            append("We have received your payment. Here are the details:\n\n")
            append("🔖 *Receipt No:* $receiptNumber\n")
            append("💵 *Amount Paid:* ₹${String.format("%.0f", amountPaid)}\n")
            if (period.isNotBlank()) {
                append("🗓️ *Validity/Period:* $period\n")
            }
            if (remainingDue > 0) {
                append("⚠️ *Remaining Balance:* ₹${String.format("%.0f", remainingDue)}\n")
            } else {
                append("✅ *Payment Status:* Full Paid (Zero Due)\n")
            }
            append("\nThank you for choosing $libraryName! Best wishes for your studies. 📖✨\n\n")
            append("— $libraryName Desk")
        }
        openWhatsApp(context, phone, msg)
    }

    /**
     * Pre-built 1-Tap Attendance Punch-in / Punch-out Alert (for parents or student logs)
     */
    fun sendAttendancePunchAlert(
        context: Context,
        studentName: String,
        phone: String,
        libraryName: String,
        status: String, // "PUNCH_IN" or "PUNCH_OUT"
        time: String,
        seatNumber: String = ""
    ) {
        val isEntry = status.contains("IN", ignoreCase = true)
        val actionText = if (isEntry) "Punched IN (Entered)" else "Punched OUT (Left)"
        val icon = if (isEntry) "🟢" else "🔴"
        
        val msg = buildString {
            append("$icon *Attendance Alert - $libraryName*\n\n")
            append("Student: *$studentName*\n")
            append("Action: *$actionText*\n")
            append("⏰ Time: $time\n")
            if (seatNumber.isNotBlank()) {
                append("🪑 Seat: $seatNumber\n")
            }
            append("\nLogged securely via LibDesk Smart Attendance.")
        }
        openWhatsApp(context, phone, msg)
    }

    /**
     * Pre-built 1-Tap Admission & Welcome Message
     */
    fun sendWelcomeAdmissionAlert(
        context: Context,
        studentName: String,
        phone: String,
        libraryName: String,
        studentCode: String,
        seatNumber: String,
        shiftName: String,
        validUntil: String
    ) {
        val msg = buildString {
            append("🎉 *Welcome to $libraryName!*\n\n")
            append("Dear *$studentName*,\n")
            append("Your library admission has been confirmed successfully!\n\n")
            append("🆔 *Student ID:* $studentCode\n")
            append("🪑 *Allocated Seat:* $seatNumber\n")
            append("⏰ *Shift Timing:* $shiftName\n")
            append("📅 *Membership Valid Till:* $validUntil\n\n")
            append("⚡ *Library Rules:* Please maintain silence in reading halls and check in with your QR code daily.\n\n")
            append("We wish you great success in your preparation! 🎯\n")
            append("— Team $libraryName")
        }
        openWhatsApp(context, phone, msg)
    }

    /**
     * Pre-built 1-Tap Locker Allocation Alert
     */
    fun sendLockerAllocationAlert(
        context: Context,
        studentName: String,
        phone: String,
        libraryName: String,
        lockerNumber: String,
        keyNumber: String,
        monthlyRent: Double,
        expiryDate: String
    ) {
        val msg = buildString {
            append("🔐 *Locker Allocated - $libraryName*\n\n")
            append("Dear *$studentName*,\n")
            append("Your personal library locker has been assigned:\n\n")
            append("📦 *Locker No:* $lockerNumber\n")
            if (keyNumber.isNotBlank()) {
                append("🔑 *Key Reference:* $keyNumber\n")
            }
            append("💰 *Monthly Rent:* ₹${String.format("%.0f", monthlyRent)}\n")
            append("📅 *Valid Till:* $expiryDate\n\n")
            append("Please keep your locker key safe.\n")
            append("— $libraryName Desk")
        }
        openWhatsApp(context, phone, msg)
    }

    /**
     * Pre-built 1-Tap Visitor Trial Pass Alert
     */
    fun sendVisitorPassAlert(
        context: Context,
        visitorName: String,
        phone: String,
        libraryName: String,
        passNumber: String,
        timeSlot: String,
        assignedSeat: String,
        feeAmount: Double
    ) {
        val msg = buildString {
            append("🎟️ *1-Day Study Pass - $libraryName*\n\n")
            append("Welcome *$visitorName*!\n")
            append("Your 1-Day Visitor Pass details:\n\n")
            append("🔖 *Pass No:* $passNumber\n")
            append("🪑 *Assigned Desk:* $assignedSeat\n")
            append("⏰ *Valid Slot:* $timeSlot\n")
            append("💵 *Pass Fee:* ₹${String.format("%.0f", feeAmount)}\n\n")
            append("Enjoy uninterrupted high-speed Wi-Fi and silent reading atmosphere! 📚\n")
            append("— $libraryName")
        }
        openWhatsApp(context, phone, msg)
    }
}
