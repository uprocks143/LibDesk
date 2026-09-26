package com.example.ui.owner

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.StudyMaterialRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialUploadScreen(
    orgId: String,
    uploadedBy: String,
    onNavigateBack: () -> Unit,
    onUploadSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    initialUri: Uri? = null,
    repository: StudyMaterialRepository = StudyMaterialRepository(LocalContext.current),
    onMaterialUploaded: ((title: String, desc: String, category: String, subject: String, exam: String, fileType: String, fileSize: String, fileUrl: String, accessPolicy: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    var selectedFileSize by remember { mutableLongStateOf(0L) }

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("notes") }
    var isFree by remember { mutableStateOf(true) }
    var selectedShift by remember { mutableStateOf("All Shifts") }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showWarning10Mb by remember { mutableStateOf(false) }

    val categories = listOf(
        "notes" to "Faculty / Study Notes",
        "mock-tests" to "Mock Tests & Test Series",
        "competitive" to "Competitive Exams (UPSC/SSC/NEET/JEE)",
        "ncert" to "NCERT Reference Materials",
        "other" to "General Reference / Syllabus"
    )

    fun handleSelectedPdf(uri: Uri) {
        var name = "Document.pdf"
        var size = 0L

        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) name = cursor.getString(nameIndex) ?: name
                    if (sizeIndex != -1) size = cursor.getLong(sizeIndex)
                }
            }
        } catch (_: Exception) {}

        if (size <= 0L) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    size = stream.available().toLong()
                }
            } catch (_: Exception) {}
        }

        val mimeType = context.contentResolver.getType(uri)
        val isPdf = name.endsWith(".pdf", ignoreCase = true) ||
            mimeType == "application/pdf" ||
            mimeType == "application/x-pdf" ||
            uri.toString().endsWith(".pdf", ignoreCase = true)

        if (!isPdf && mimeType != null && mimeType != "application/octet-stream" && mimeType != "*/*") {
            Toast.makeText(context, "Only PDF documents (.pdf) are permitted.", Toast.LENGTH_LONG).show()
            return
        }

        // Client-side 20 MB size validation
        if (size > 20 * 1024 * 1024L) {
            errorMessage = "Selected PDF exceeds the 20 MB maximum upload limit (${size / (1024 * 1024)} MB). Please select a compressed file."
            selectedUri = null
            selectedFileName = ""
            selectedFileSize = 0L
            return
        }

        selectedUri = uri
        selectedFileName = name
        selectedFileSize = size
        errorMessage = null

        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: Exception) {}

        if (title.isBlank()) {
            title = name.removeSuffix(".pdf").removeSuffix(".PDF").replace("_", " ").replace("-", " ")
        }

        showWarning10Mb = size > 10 * 1024 * 1024L
    }

    LaunchedEffect(initialUri) {
        if (initialUri != null) {
            handleSelectedPdf(initialUri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) handleSelectedPdf(uri)
    }

    val getContentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) handleSelectedPdf(uri)
    }

    val launchDevicePicker = {
        try {
            openDocumentLauncher.launch(arrayOf("application/pdf", "application/x-pdf", "*/*"))
        } catch (_: Exception) {
            try {
                getContentLauncher.launch("*/*")
            } catch (e: Exception) {
                Toast.makeText(context, "File picker could not be opened: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchAllFilesPicker = {
        try {
            getContentLauncher.launch("*/*")
        } catch (_: Exception) {
            try {
                openDocumentLauncher.launch(arrayOf("*/*"))
            } catch (e: Exception) {
                Toast.makeText(context, "Storage picker could not be opened: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upload Study Material") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("upload_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // PDF File Picker Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedUri != null) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (selectedUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(enabled = !isUploading) {
                        launchDevicePicker()
                    }
                    .testTag("pdf_picker_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selectedUri != null) Icons.Default.CheckCircle else Icons.Default.UploadFile,
                        contentDescription = "Pick PDF",
                        tint = if (selectedUri != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(46.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (selectedFileName.isNotBlank()) selectedFileName else "Select PDF from Internal Storage",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (selectedFileSize > 0) {
                            val mb = selectedFileSize / (1024.0 * 1024.0)
                            String.format(java.util.Locale.US, "Size: %.2f MB (Max 20 MB Limit)", mb)
                        } else {
                            "Pick file from internal storage, documents or downloads (PDF only, max 20 MB)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { launchDevicePicker() },
                            enabled = !isUploading,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selectedUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                if (selectedUri != null) Icons.Default.ChangeCircle else Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (selectedUri != null) "Change PDF" else "Internal Storage", maxLines = 1)
                        }

                        OutlinedButton(
                            onClick = { launchAllFilesPicker() },
                            enabled = !isUploading,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("All Storage / Files", maxLines = 1)
                        }
                    }
                }
            }

            // 10 MB Warning if applicable
            if (showWarning10Mb) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Large File Warning: This document is over 10 MB. Consider compressing the PDF to save your cloud storage quota.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Form Inputs
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Material Title *") },
                placeholder = { Text("e.g. Modern Indian History Notes Part 1") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("material_title_input"),
                singleLine = true,
                enabled = !isUploading
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description / Topics Covered") },
                placeholder = { Text("e.g. Comprehensive notes for UPSC Prelims 2026") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("material_description_input"),
                minLines = 3,
                maxLines = 5,
                enabled = !isUploading
            )

            // Category Selection Dropdown
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { if (!isUploading) categoryExpanded = !categoryExpanded }
            ) {
                OutlinedTextField(
                    value = categories.firstOrNull { it.first == category }?.second ?: "Select Category",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category *") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("material_category_dropdown"),
                    enabled = !isUploading
                )
                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { (catKey, catLabel) ->
                        DropdownMenuItem(
                            text = { Text(catLabel) },
                            onClick = {
                                category = catKey
                                categoryExpanded = false
                            }
                        )
                    }
                }
            }

            // Free Access Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Free for All Enrolled Students",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                    )
                    Text(
                        text = if (isFree) "Any active student can download" else "Restricted to paid members only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isFree,
                    onCheckedChange = { isFree = it },
                    enabled = !isUploading,
                    modifier = Modifier.testTag("material_free_switch")
                )
            }

            // Upload Progress Bar
            if (isUploading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uploadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .testTag("upload_progress_bar")
                    )
                    Text(
                        text = "Encrypting & Uploading to Supabase Storage: $uploadProgress%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Submit Button
            Button(
                onClick = {
                    if (selectedUri == null) {
                        errorMessage = "Please select a PDF document first."
                        return@Button
                    }
                    if (title.isBlank()) {
                        errorMessage = "Please provide a title for the material."
                        return@Button
                    }

                    isUploading = true
                    errorMessage = null

                    coroutineScope.launch {
                        val result = repository.uploadMaterial(
                            orgId = orgId,
                            title = title.trim(),
                            description = description.trim(),
                            category = category,
                            fileUri = selectedUri!!,
                            fileName = selectedFileName,
                            isFree = isFree,
                            uploadedBy = uploadedBy,
                            fileSizeBytes = selectedFileSize,
                            onProgress = { progress ->
                                uploadProgress = progress
                            }
                        )

                        isUploading = false

                        if (result.isSuccess) {
                            val material = result.getOrNull()
                            val mb = selectedFileSize / (1024.0 * 1024.0)
                            val formattedSize = if (mb >= 1.0) String.format(java.util.Locale.US, "%.1f MB", mb) else "${(selectedFileSize / 1024).coerceAtLeast(1)} KB"
                            val storagePath = material?.storagePath ?: "study-materials/$orgId/$category/$selectedFileName"
                            onMaterialUploaded?.invoke(
                                title.trim(),
                                description.trim().ifBlank { "Study notes and reference guide." },
                                category,
                                "General Study",
                                "All Competitive Exams",
                                "PDF",
                                formattedSize,
                                storagePath,
                                if (isFree) "ALL_STUDENTS" else "PAID_STUDENTS"
                            )
                            Toast.makeText(context, "Study material uploaded successfully!", Toast.LENGTH_SHORT).show()
                            onUploadSuccess()
                        } else {
                            errorMessage = result.exceptionOrNull()?.localizedMessage ?: "Upload failed. Enqueued for offline retry."
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_upload_button"),
                enabled = !isUploading && selectedUri != null && title.isNotBlank()
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Uploading...")
                } else {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload Study Material")
                }
            }
        }
    }
}
