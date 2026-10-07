package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entities.*
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility for 1-Click Excel / CSV & Financial Report Export for CA & Accounting.
 */
object ReportExportUtils {

    /**
     * Exports monthly fee collections & P&L to a formatted CSV file and opens the Android Share Sheet.
     */
    fun exportMonthlyFinancialCsv(
        context: Context,
        library: LibraryEntity?,
        payments: List<PaymentEntity>,
        expenses: List<ExpenseEntity>,
        monthPrefix: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ) {
        try {
            val libName = library?.name?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "LibDesk"
            val fileName = "${libName}_Financial_Report_${monthPrefix}.csv"
            val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(cacheDir, fileName)

            val writer = FileWriter(file)

            // Header Info
            writer.append("Library Name:,${escapeCsv(library?.name ?: "LibDesk Library")}\n")
            writer.append("Library Code:,${escapeCsv(library?.code ?: "LIB-01")}\n")
            writer.append("Report Period:,$monthPrefix\n")
            writer.append("Generated On:,${SimpleDateFormat("dd-MMM-yyyy hh:mm a", Locale.getDefault()).format(Date())}\n\n")

            // Summary Totals
            val filteredPayments = payments.filter { it.date.startsWith(monthPrefix) }
            val filteredExpenses = expenses.filter { it.date.startsWith(monthPrefix) }
            val totalInflow = filteredPayments.sumOf { it.amount }
            val totalOutflow = filteredExpenses.sumOf { it.amount }
            val netSurplus = totalInflow - totalOutflow

            writer.append("--- FINANCIAL SUMMARY ---\n")
            writer.append("Total Fee Collections (Inflow):,₹${String.format("%.2f", totalInflow)}\n")
            writer.append("Total Operating Expenses (Outflow):,₹${String.format("%.2f", totalOutflow)}\n")
            writer.append("Net Surplus / P&L:,₹${String.format("%.2f", netSurplus)}\n\n")

            // Fee Payments Section
            writer.append("--- DETAILED FEE COLLECTIONS ---\n")
            writer.append("Receipt No,Date,Student Name,Payment Mode,Amount Paid,Balance Due,Purpose\n")
            for (p in filteredPayments.sortedByDescending { it.date }) {
                writer.append("${escapeCsv(p.receiptNumber)},")
                writer.append("${escapeCsv(p.date)},")
                writer.append("${escapeCsv(p.studentName)},")
                writer.append("${escapeCsv(p.paymentMode)},")
                writer.append("₹${String.format("%.2f", p.amount)},")
                writer.append("₹${String.format("%.2f", p.dueBalance)},")
                writer.append("${escapeCsv(p.purpose)}\n")
            }

            // Expenses Section
            writer.append("\n--- DETAILED EXPENSES ---\n")
            writer.append("Expense Description,Category,Date,Payment Mode,Amount,Receipt Ref\n")
            for (e in filteredExpenses.sortedByDescending { it.date }) {
                writer.append("${escapeCsv(e.description)},")
                writer.append("${escapeCsv(e.category)},")
                writer.append("${escapeCsv(e.date)},")
                writer.append("${escapeCsv(e.paymentMode)},")
                writer.append("₹${String.format("%.2f", e.amount)},")
                writer.append("${escapeCsv(e.receiptRef)}\n")
            }

            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Financial Report ($monthPrefix)")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports Pending Student Dues CSV with mobile and seat info for debt collection.
     */
    fun exportStudentDuesCsv(
        context: Context,
        library: LibraryEntity?,
        students: List<StudentEntity>
    ) {
        try {
            val duesStudents = students.filter { it.dueAmount > 0 }
            val libName = library?.name?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "LibDesk"
            val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
            val fileName = "${libName}_Pending_Dues_${todayStr}.csv"
            val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(cacheDir, fileName)

            val writer = FileWriter(file)
            writer.append("Student Code,Full Name,Mobile,Parent Mobile,Seat No,Shift,Total Fee,Paid Amount,Pending Due,Expiry Date\n")

            for (s in duesStudents.sortedByDescending { it.dueAmount }) {
                writer.append("${escapeCsv(s.studentCode)},")
                writer.append("${escapeCsv(s.fullName)},")
                writer.append("${escapeCsv(s.mobile)},")
                writer.append("${escapeCsv(s.parentMobile)},")
                writer.append("${escapeCsv(s.seatNumber.ifBlank { "Unassigned" })},")
                writer.append("${escapeCsv(s.shiftName)},")
                writer.append("₹${String.format("%.2f", s.totalFee)},")
                writer.append("₹${String.format("%.2f", s.paidAmount)},")
                writer.append("₹${String.format("%.2f", s.dueAmount)},")
                writer.append("${escapeCsv(s.expiryDate)}\n")
            }

            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Student Dues List")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Exports Attendance Register CSV.
     */
    fun exportAttendanceRegisterCsv(
        context: Context,
        library: LibraryEntity?,
        attendance: List<AttendanceEntity>,
        datePrefix: String = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    ) {
        try {
            val libName = library?.name?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "LibDesk"
            val fileName = "${libName}_Attendance_${datePrefix}.csv"
            val cacheDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(cacheDir, fileName)

            val writer = FileWriter(file)
            writer.append("Date,Student Name,Seat No,Hall,Shift,Check-In Time,Check-Out Time,Duration (Mins),Status\n")

            val filtered = attendance.filter { it.date.startsWith(datePrefix) }
            for (a in filtered.sortedByDescending { it.date }) {
                writer.append("${escapeCsv(a.date)},")
                writer.append("${escapeCsv(a.studentName)},")
                writer.append("${escapeCsv(a.seatNumber)},")
                writer.append("${escapeCsv(a.hallName)},")
                writer.append("${escapeCsv(a.shiftName)},")
                writer.append("${escapeCsv(a.checkInTime)},")
                writer.append("${escapeCsv(a.checkOutTime)},")
                writer.append("${a.durationMinutes},")
                writer.append("${escapeCsv(a.status)}\n")
            }

            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Share Attendance Register ($datePrefix)")
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
