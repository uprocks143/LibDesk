package com.example.ui.manager

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entities.LibraryEntity
import com.example.data.local.entities.StudentEntity

/**
 * Batch Student ID Card Print & A4 Sheet Generator Modal for Library Managers.
 */
@Composable
fun BatchStudentIdCardModal(
    library: LibraryEntity?,
    students: List<StudentEntity>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedStudentIds by remember {
        mutableStateOf(students.filter { it.status == "ACTIVE" }.map { it.id }.toSet())
    }
    var searchQuery by remember { mutableStateOf("") }
    var selectedShiftFilter by remember { mutableStateOf("ALL") }

    val distinctShifts = remember(students) {
        listOf("ALL") + students.map { it.shiftName }.filter { it.isNotBlank() }.distinct()
    }

    val filteredStudents = remember(students, searchQuery, selectedShiftFilter) {
        students.filter { st ->
            (selectedShiftFilter == "ALL" || st.shiftName == selectedShiftFilter) &&
            (searchQuery.isBlank() || st.fullName.contains(searchQuery, ignoreCase = true) || st.studentCode.contains(searchQuery, ignoreCase = true))
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Batch Student ID Card Print",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${selectedStudentIds.size} of ${filteredStudents.size} Students Selected for A4 Print",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Search & Filter Row
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by student name or roll ID...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Select All / Deselect All Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = filteredStudents.isNotEmpty() && selectedStudentIds.containsAll(filteredStudents.map { it.id }),
                            onCheckedChange = { checked ->
                                selectedStudentIds = if (checked) {
                                    selectedStudentIds + filteredStudents.map { it.id }
                                } else {
                                    selectedStudentIds - filteredStudents.map { it.id }.toSet()
                                }
                            }
                        )
                        Text(
                            text = "Select All (${filteredStudents.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (distinctShifts.size > 2) {
                        Text(
                            text = "Shift: $selectedShiftFilter",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Student Selection List
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredStudents, key = { it.id }) { student ->
                        val isSelected = selectedStudentIds.contains(student.id)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedStudentIds = if (isSelected) {
                                        selectedStudentIds - student.id
                                    } else {
                                        selectedStudentIds + student.id
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = { checked ->
                                        selectedStudentIds = if (checked) selectedStudentIds + student.id else selectedStudentIds - student.id
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = student.fullName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "ID: ${student.studentCode} • Desk: ${student.seatNumber.ifBlank { "Unassigned" }} • ${student.shiftName}",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (student.status == "ACTIVE") Color(0xFF10B981).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        text = student.status,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (student.status == "ACTIVE") Color(0xFF047857) else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val toPrint = students.filter { selectedStudentIds.contains(it.id) }
                            if (toPrint.isEmpty()) {
                                Toast.makeText(context, "Please select at least 1 student", Toast.LENGTH_SHORT).show()
                            } else {
                                printBatchIdCardsHtml(context, library, toPrint)
                            }
                        },
                        enabled = selectedStudentIds.isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Print A4 Sheet (${selectedStudentIds.size})", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Generates ready-to-print A4 HTML document with multiple ID Cards formatted in a clean 2x4 grid
 * and sends it to the Android System PrintManager!
 */
private fun printBatchIdCardsHtml(
    context: Context,
    library: LibraryEntity?,
    students: List<StudentEntity>
) {
    val libName = library?.name?.ifBlank { "LibDesk Smart Library" } ?: "LibDesk Smart Library"
    val libAddress = listOfNotNull(library?.address, library?.city).filter { it.isNotBlank() }.joinToString(", ")
    val libPhone = library?.phone.orEmpty()

    val html = buildString {
        append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Batch ID Cards - $libName</title>")
        append("<style>")
        append("@page { size: A4 portrait; margin: 10mm; }")
        append("body { font-family: 'Helvetica Neue', Arial, sans-serif; margin: 0; padding: 0; background: #fff; color: #111; }")
        append(".grid { display: flex; flex-wrap: wrap; justify-content: space-between; }")
        append(".card { width: 48%; border: 1.5px solid #0284c7; border-radius: 8px; margin-bottom: 12mm; page-break-inside: avoid; box-sizing: border-box; overflow: hidden; }")
        append(".card-header { background: #0284c7; color: #fff; padding: 6px 10px; text-align: center; }")
        append(".card-header h2 { margin: 0; font-size: 13pt; text-transform: uppercase; letter-spacing: 0.5px; }")
        append(".card-header p { margin: 2px 0 0; font-size: 8pt; opacity: 0.9; }")
        append(".card-body { padding: 10px; display: flex; justify-content: space-between; align-items: center; }")
        append(".card-info { font-size: 9pt; line-height: 1.4; }")
        append(".card-info b { color: #0f172a; }")
        append(".badge { display: inline-block; background: #e0f2fe; color: #0369a1; padding: 2px 6px; border-radius: 4px; font-weight: bold; font-size: 8pt; margin-top: 4px; }")
        append(".card-qr { width: 55px; height: 55px; border: 1px dashed #94a3b8; display: flex; align-items: center; justify-content: center; font-size: 7pt; color: #64748b; text-align: center; border-radius: 4px; }")
        append(".card-footer { background: #f8fafc; border-top: 1px solid #e2e8f0; padding: 4px 10px; font-size: 7pt; color: #475569; display: flex; justify-content: space-between; }")
        append("</style></head><body>")
        append("<div class='grid'>")

        for (s in students) {
            append("<div class='card'>")
            append("<div class='card-header'><h2>$libName</h2><p>$libAddress</p></div>")
            append("<div class='card-body'>")
            append("<div class='card-info'>")
            append("<b>Name:</b> ${s.fullName}<br>")
            append("<b>ID:</b> ${s.studentCode} | <b>Seat:</b> ${s.seatNumber.ifBlank { "Desk N/A" }}<br>")
            append("<b>Shift:</b> ${s.shiftName}<br>")
            append("<b>Valid Till:</b> ${s.expiryDate.ifBlank { "Active" }}<br>")
            append("<div class='badge'>Target: ${s.targetExam}</div>")
            append("</div>")
            append("<div class='card-qr'>QR PASS<br>${s.studentCode}</div>")
            append("</div>")
            append("<div class='card-footer'><span>Ph: ${s.mobile}</span><span>Emergency: ${s.emergencyContact.ifBlank { libPhone }}</span></div>")
            append("</div>")
        }

        append("</div></body></html>")
    }

    try {
        val webView = WebView(context).apply {
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    val printAdapter = createPrintDocumentAdapter("Batch_ID_Cards")
                    printManager?.print("LibDesk_Student_ID_Cards", printAdapter, PrintAttributes.Builder().build())
                }
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    } catch (e: Exception) {
        Toast.makeText(context, "Printing error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
