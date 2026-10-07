package com.example.util

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.PaymentEntity
import com.example.data.local.entities.StudentEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Generates 58mm & 80mm ESC/POS Monospaced Thermal Print Slips for Bluetooth Printers.
 */
object ThermalPrinterReceiptUtils {

    /**
     * Formats receipt text suitable for standard 58mm (32 chars per line) or 80mm (48 chars per line) thermal printers.
     */
    fun generateThermalSlipText(
        library: LibraryEntity?,
        student: StudentEntity?,
        payment: PaymentEntity,
        is80mm: Boolean = false
    ): String {
        val width = if (is80mm) 48 else 32
        val line = "=".repeat(width)
        val dash = "-".repeat(width)

        val libName = library?.name?.ifBlank { "LibDesk Smart Library" } ?: "LibDesk Smart Library"
        val libAddress = listOfNotNull(
            library?.address.takeIf { !it.isNullOrBlank() },
            library?.city.takeIf { !it.isNullOrBlank() }
        ).joinToString(", ")
        val libPhone = library?.phone?.takeIf { it.isNotBlank() } ?: library?.ownerPhone.orEmpty()

        val sb = StringBuilder()

        // 1. Center Aligned Header
        sb.append(centerText(libName.uppercase(), width)).append("\n")
        if (libAddress.isNotBlank()) {
            sb.append(centerText(libAddress, width)).append("\n")
        }
        if (libPhone.isNotBlank()) {
            sb.append(centerText("Tel: $libPhone", width)).append("\n")
        }
        sb.append(line).append("\n")
        sb.append(centerText("** FEE PAYMENT RECEIPT **", width)).append("\n")
        sb.append(line).append("\n")

        // 2. Receipt Meta
        sb.append(formatKeyValue("Receipt No:", payment.receiptNumber, width)).append("\n")
        val dateFormatted = try {
            val sdf = SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault())
            if (payment.createdAt > 0) sdf.format(Date(payment.createdAt))
            else payment.date
        } catch (_: Exception) { payment.date }
        sb.append(formatKeyValue("Date & Time:", dateFormatted, width)).append("\n")
        sb.append(formatKeyValue("Mode:", payment.paymentMode.uppercase(), width)).append("\n")
        if (payment.referenceNumber.isNotBlank()) {
            sb.append(formatKeyValue("Txn Ref:", payment.referenceNumber, width)).append("\n")
        }

        sb.append(dash).append("\n")

        // 3. Member Info
        val stName = payment.studentName.ifBlank { student?.fullName ?: "Student" }
        val stCode = student?.studentCode ?: "N/A"
        val seatNo = student?.seatNumber?.ifBlank { "Desk N/A" } ?: "Desk N/A"
        val shift = student?.shiftName?.ifBlank { "Regular" } ?: "Regular"

        sb.append(formatKeyValue("Student Name:", stName, width)).append("\n")
        sb.append(formatKeyValue("Student ID:", stCode, width)).append("\n")
        sb.append(formatKeyValue("Seat No:", seatNo, width)).append("\n")
        sb.append(formatKeyValue("Shift Timing:", shift, width)).append("\n")

        sb.append(dash).append("\n")

        // 4. Financial Breakdown
        val plan = payment.purpose.ifBlank { "Monthly Reading Pass" }
        sb.append(formatKeyValue("Plan:", plan, width)).append("\n")
        sb.append(formatKeyValue("Period:", payment.period.ifBlank { "Current Month" }, width)).append("\n")
        sb.append(formatKeyValue("Total Plan Fee:", "Rs. ${String.format("%.0f", payment.amount + payment.dueBalance)}", width)).append("\n")
        sb.append(formatKeyValue("AMOUNT PAID:", "Rs. ${String.format("%.0f", payment.amount)}", width)).append("\n")
        sb.append(formatKeyValue("Balance Due:", "Rs. ${String.format("%.0f", payment.dueBalance)}", width)).append("\n")

        sb.append(line).append("\n")

        // 5. Footer & Instructions
        val statusText = if (payment.dueBalance <= 0) "STATUS: FULL PAID (ZERO DUE)" else "STATUS: PARTIAL PAYMENT"
        sb.append(centerText(statusText, width)).append("\n")
        sb.append(line).append("\n")
        sb.append(centerText("Thank you for choosing $libName!", width)).append("\n")
        sb.append(centerText("Kindly maintain silence in reading hall.", width)).append("\n")
        sb.append(centerText("*** Keep This Slip For Records ***", width)).append("\n\n\n")

        return sb.toString()
    }

    /**
     * Triggers Android share intent so user can send slip to any Bluetooth Print app (e.g. RawBT, ESC POS Bluetooth Print Service) or share as text.
     */
    fun shareThermalSlip(
        context: Context,
        slipText: String,
        studentName: String,
        receiptNo: String
    ) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Thermal Receipt #$receiptNo - $studentName")
                putExtra(Intent.EXTRA_TEXT, slipText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Print / Share Receipt").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Toast.makeText(context, "Print error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun centerText(text: String, width: Int): String {
        if (text.length >= width) return text.take(width)
        val pad = (width - text.length) / 2
        return " ".repeat(pad) + text
    }

    private fun formatKeyValue(key: String, value: String, width: Int): String {
        val totalLen = key.length + value.length + 1
        if (totalLen >= width) {
            val maxVal = (width - key.length - 2).coerceAtLeast(4)
            return "$key " + value.take(maxVal)
        }
        val spaces = " ".repeat(width - key.length - value.length)
        return "$key$spaces$value"
    }
}
