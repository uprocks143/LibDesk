package com.example.ui.manager

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.local.entities.DigitalMaterialEntity
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.pdf.PdfViewerDialog
import com.example.ui.theme.*

@Composable
fun ManagerDigitalLibraryScreen(
    materials: List<DigitalMaterialEntity>,
    onAddMaterial: (String, String, String, String, String, String, String, String, String) -> Unit,
    onToggleBookmark: (DigitalMaterialEntity) -> Unit,
    onAutoAddFreeMaterials: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedCategoryFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddMaterialModal by remember { mutableStateOf(false) }
    var selectedPdfToView by remember { mutableStateOf<DigitalMaterialEntity?>(null) }

    val filteredMaterials = remember(materials, selectedCategoryFilter, searchQuery) {
        materials.filter { mat ->
            val matchesCategory = when (selectedCategoryFilter) {
                "ALL" -> true
                "Class 1-5" -> mat.category in listOf("Class 1", "Class 2", "Class 3", "Class 4", "Class 5")
                "Class 6-8" -> mat.category in listOf("Class 6", "Class 7", "Class 8")
                "Class 9-10" -> mat.category in listOf("Class 9", "Class 10")
                "Class 11-12" -> mat.category in listOf("Class 11", "Class 12")
                "NCERT" -> mat.category.contains("Class", ignoreCase = true) || mat.category.equals("NCERT", ignoreCase = true) || mat.title.contains("NCERT", ignoreCase = true)
                else -> mat.category.equals(selectedCategoryFilter, ignoreCase = true)
            }
            val matchesSearch = searchQuery.isBlank() ||
                mat.title.contains(searchQuery, ignoreCase = true) ||
                mat.subject.contains(searchQuery, ignoreCase = true) ||
                mat.exam.contains(searchQuery, ignoreCase = true) ||
                mat.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMaterialModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Resource", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudUpload,
                                contentDescription = "Upload Study Material Icon",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Upload Study Material",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            )
                            Text(
                                text = "Publish PDFs, PYQs, exam guides & notes for students",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { showAddMaterialModal = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onAutoAddFreeMaterials() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add NCERT (1-12)", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search NCERT books, notes, subject, class...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryFilter == "ALL",
                    onClick = { selectedCategoryFilter = "ALL" },
                    label = { Text("All (${materials.size})", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "NCERT",
                    onClick = { selectedCategoryFilter = "NCERT" },
                    label = { Text("NCERT All", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "Class 1-5",
                    onClick = { selectedCategoryFilter = "Class 1-5" },
                    label = { Text("Class 1-5", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "Class 6-8",
                    onClick = { selectedCategoryFilter = "Class 6-8" },
                    label = { Text("Class 6-8", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "Class 9-10",
                    onClick = { selectedCategoryFilter = "Class 9-10" },
                    label = { Text("Class 9-10", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "Class 11-12",
                    onClick = { selectedCategoryFilter = "Class 11-12" },
                    label = { Text("Class 11-12", fontSize = 13.sp) }
                )
                FilterChip(
                    selected = selectedCategoryFilter == "UPSC CSE",
                    onClick = { selectedCategoryFilter = "UPSC CSE" },
                    label = { Text("UPSC / Govt", fontSize = 13.sp) }
                )
            }

            if (filteredMaterials.isEmpty()) {
                EmptyPlaceholder(
                    title = "No Study Materials Found",
                    description = "Publish digital PDFs, class notes, and PYQs for your students.",
                    icon = Icons.Default.FolderOpen,
                    actionLabel = "Upload PDF/Notes",
                    onAction = { showAddMaterialModal = true }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredMaterials, key = { it.id }) { item ->
                        DigitalMaterialCard(
                            material = item,
                            onBookmark = { onToggleBookmark(item) },
                            onDownload = {
                                selectedPdfToView = item
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddMaterialModal) {
        AddDigitalMaterialDialog(
            onClose = { showAddMaterialModal = false },
            onConfirm = { title, desc, cat, sub, exam, fType, fSize, fUrl, policy ->
                onAddMaterial(title, desc, cat, sub, exam, fType, fSize, fUrl, policy)
                showAddMaterialModal = false
            }
        )
    }

    selectedPdfToView?.let { pdf ->
        PdfViewerDialog(
            title = pdf.title,
            category = pdf.category,
            subject = pdf.subject,
            fileSize = pdf.fileSize,
            fileUriOrUrl = pdf.fileUrl,
            description = pdf.description,
            onDismiss = { selectedPdfToView = null }
        )
    }
}

@Composable
fun DigitalMaterialCard(
    material: DigitalMaterialEntity,
    onBookmark: () -> Unit,
    onDownload: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (material.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (material.fileType.equals("PDF", ignoreCase = true)) Icons.Default.PictureAsPdf else Icons.Default.Article,
                            contentDescription = null,
                            tint = if (material.fileType.equals("PDF", ignoreCase = true)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = material.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (material.isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (material.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(
                            text = material.category,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(
                            text = material.fileSize,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }

                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read PDF", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AddDigitalMaterialDialog(
    onClose: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("NCERT") }
    var subject by remember { mutableStateOf("General") }
    var exam by remember { mutableStateOf("CBSE / All Exams") }
    var fileSize by remember { mutableStateOf("Local PDF") }
    var fileUriOrUrl by remember { mutableStateOf("") }
    var fileNamePicked by remember { mutableStateOf<String?>(null) }
    var isEnteringWebUrl by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun cleanFileNameToTitle(rawName: String): String {
        return rawName
            .substringBeforeLast(".")
            .replace(Regex("[_\\-+]"), " ")
            .trim()
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }

    val openDocLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {
            }
            fileUriOrUrl = uri.toString()
            val resolvedName = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
            } ?: "Document.pdf"
            fileNamePicked = resolvedName
            if (title.isBlank()) {
                title = cleanFileNameToTitle(resolvedName)
            }
            if (description.isBlank()) {
                description = "Digital study textbook / notes ($resolvedName)"
            }
            fileSize = try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex >= 0) {
                        val bytes = cursor.getLong(sizeIndex)
                        "%.1f MB".format(bytes / (1024.0 * 1024.0))
                    } else "Local PDF"
                } ?: "Local PDF"
            } catch (_: Exception) {
                "Local PDF"
            }
        }
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            fileUriOrUrl = uri.toString()
            fileNamePicked = "Selected_PDF_File.pdf"
            if (title.isBlank()) {
                title = "Study Resource PDF"
            }
            if (description.isBlank()) {
                description = "Digital study textbook / notes"
            }
        }
    }

    BackHandler { onClose() }

    Dialog(
        onDismissRequest = onClose,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload Digital Study Material",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // PDF File Selection Box
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (fileNamePicked != null || fileUriOrUrl.isNotBlank()) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = fileNamePicked ?: fileUriOrUrl,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(text = "Size: $fileSize • Ready to Publish", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    try {
                                        openDocLauncher.launch(arrayOf("application/pdf", "*/*"))
                                    } catch (_: Exception) {
                                        getContentLauncher.launch("application/pdf")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (fileUriOrUrl.isBlank()) "Choose PDF File" else "Change File")
                            }

                            TextButton(
                                onClick = { isEnteringWebUrl = !isEnteringWebUrl }
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isEnteringWebUrl) "Use Device File" else "Paste Web URL")
                            }
                        }

                        if (isEnteringWebUrl) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = fileUriOrUrl,
                                onValueChange = {
                                    fileUriOrUrl = it
                                    if (fileNamePicked == null && it.isNotBlank()) {
                                        fileNamePicked = it.substringAfterLast("/").substringBefore("?")
                                        if (title.isBlank()) title = cleanFileNameToTitle(fileNamePicked!!)
                                    }
                                },
                                label = { Text("Direct PDF URL (https://...)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Resource Title * (e.g. Class 10 Science Chapter 1)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Summary / Highlights / Topics Covered") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category (e.g. Class 10, NCERT)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject (e.g. Maths, Science)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = exam,
                    onValueChange = { exam = it },
                    label = { Text("Target Board / Exam (e.g. CBSE, UPSC, SSC, NEET)") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Category Presets
                Text("Quick Category Presets", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("NCERT", "Class 10", "Class 12", "Class 9", "Class 11", "UPSC Notes", "PYQ 2024").forEach { preset ->
                        FilterChip(
                            selected = category == preset,
                            onClick = { category = preset },
                            label = { Text(preset, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val finalTitle = title.trim().ifBlank { fileNamePicked ?: "Study Resource PDF" }
                            val finalUrl = fileUriOrUrl.trim()
                            if (finalUrl.isNotBlank()) {
                                onConfirm(
                                    finalTitle,
                                    description.trim().ifBlank { "Study PDF resource for $finalTitle" },
                                    category.trim().ifBlank { "NCERT" },
                                    subject.trim().ifBlank { "General" },
                                    exam.trim().ifBlank { "All Exams" },
                                    "PDF",
                                    fileSize,
                                    finalUrl,
                                    "ALL_STUDENTS"
                                )
                            } else {
                                Toast.makeText(context, "Please choose a PDF file or enter a PDF URL first", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = fileUriOrUrl.isNotBlank() || title.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Publish PDF")
                    }
                }
            }
        }
    }
}

