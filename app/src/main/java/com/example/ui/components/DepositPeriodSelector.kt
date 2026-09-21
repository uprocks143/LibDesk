package com.example.ui.components

import android.app.DatePickerDialog
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/**
 * Utility functions for date range manipulation in deposit periods.
 */
object DateRangeUtils {
    private val standardDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getTodayString(): String {
        return standardDateFormat.format(Date())
    }

    fun parseDate(dateStr: String): Date? {
        if (dateStr.isBlank()) return null
        val formats = arrayOf("yyyy-MM-dd", "dd-MM-yyyy", "yyyy/MM/dd", "dd/MM/yyyy")
        for (f in formats) {
            try {
                val sdf = SimpleDateFormat(f, Locale.getDefault())
                sdf.isLenient = false
                val parsed = sdf.parse(dateStr.trim())
                if (parsed != null) return parsed
            } catch (_: Exception) {}
        }
        return null
    }

    fun formatDate(date: Date): String {
        return standardDateFormat.format(date)
    }

    fun addDaysToDate(dateStr: String, daysToAdd: Int): String {
        val parsed = parseDate(dateStr) ?: Date()
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.DAY_OF_YEAR, daysToAdd)
        }
        return standardDateFormat.format(cal.time)
    }

    fun calculateDaysBetween(fromDateStr: String, toDateStr: String): Int {
        val d1 = parseDate(fromDateStr) ?: return 0
        val d2 = parseDate(toDateStr) ?: return 0
        val diffMs = d2.time - d1.time
        return (diffMs / (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(0)
    }

    fun showNativeDatePicker(
        context: Context,
        initialDateStr: String,
        onDateSelected: (String) -> Unit
    ) {
        val cal = Calendar.getInstance()
        val parsed = parseDate(initialDateStr)
        if (parsed != null) {
            cal.time = parsed
        }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedCal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }
                onDateSelected(standardDateFormat.format(selectedCal.time))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun isDateWithinRange(testDateStr: String, fromDateStr: String, toDateStr: String): Boolean {
        val test = parseDate(testDateStr) ?: return true
        val from = parseDate(fromDateStr)
        val to = parseDate(toDateStr)
        if (from != null) {
            val fromCal = Calendar.getInstance().apply {
                time = from
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (test.before(fromCal.time)) return false
        }
        if (to != null) {
            val toCal = Calendar.getInstance().apply {
                time = to
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            if (test.after(toCal.time)) return false
        }
        return true
    }

    fun getToday(): String = getTodayString()

    fun getDaysAgo(days: Int): String {
        val cal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, -days)
        }
        return standardDateFormat.format(cal.time)
    }

    fun getStartOfMonth(): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return standardDateFormat.format(cal.time)
    }
}

/**
 * Modern date-range selector for recording fee payments.
 * Replaces month-based drop-down with explicit "From Date" to "To Date" options,
 * with native calendar pickers, interactive date inputs, and day-based duration helpers.
 */
@Composable
fun DepositPeriodDateRangeSelector(
    fromDate: String,
    toDate: String,
    onFromDateChange: (String) -> Unit,
    onToDateChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val daysCount = remember(fromDate, toDate) {
        DateRangeUtils.calculateDaysBetween(fromDate, toDate)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("deposit_period_selector")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Deposit Period (From Date - To Date)",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Text(
                        text = if (daysCount > 0) "$daysCount Days" else "1 Day",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Date Input Fields (From Date & To Date)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // From Date Field
                OutlinedTextField(
                    value = fromDate,
                    onValueChange = { onFromDateChange(it) },
                    label = { Text("From Date", fontSize = 11.sp) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                DateRangeUtils.showNativeDatePicker(context, fromDate) { picked ->
                                    onFromDateChange(picked)
                                    // If toDate is before or equal to picked fromDate, adjust toDate
                                    if (DateRangeUtils.calculateDaysBetween(picked, toDate) <= 0) {
                                        onToDateChange(DateRangeUtils.addDaysToDate(picked, 30))
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Outlined.CalendarMonth,
                                contentDescription = "Pick From Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deposit_from_date_field")
                )

                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "to",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )

                // To Date Field
                OutlinedTextField(
                    value = toDate,
                    onValueChange = { onToDateChange(it) },
                    label = { Text("To Date", fontSize = 11.sp) },
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                DateRangeUtils.showNativeDatePicker(context, toDate) { picked ->
                                    onToDateChange(picked)
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Event,
                                contentDescription = "Pick To Date",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("deposit_to_date_field")
                )
            }

            // Summary Info
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🗓️ Validity Period: $fromDate to $toDate (${if (daysCount > 0) "$daysCount days" else "Same day"})",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
