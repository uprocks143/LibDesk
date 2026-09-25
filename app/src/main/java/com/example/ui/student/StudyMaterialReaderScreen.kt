package com.example.ui.student

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyMaterial
import com.example.data.repository.StudyMaterialRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialReaderScreen(
    material: StudyMaterial,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: StudyMaterialRepository = StudyMaterialRepository(LocalContext.current)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(true) }
    var downloadError by remember { mutableStateOf<String?>(null) }
    var localPdfFile by remember { mutableStateOf<File?>(null) }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isNightMode by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(setOf<Int>()) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 3.5f)
        if (scale > 1f) {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    // Function to render a specific page index
    fun renderPage(renderer: PdfRenderer, pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return
        try {
            val page = renderer.openPage(pageIndex)
            val width = page.width * 2
            val height = page.height * 2
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(if (isNightMode) Color.BLACK else Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            currentBitmap = bitmap
            currentPageIndex = pageIndex
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Fetch and download PDF to cache
    LaunchedEffect(material.id) {
        isDownloading = true
        downloadError = null

        withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "study_materials")
                if (!cacheDir.exists()) cacheDir.mkdirs()
                val targetFile = File(cacheDir, "${material.id}.pdf")

                if (!targetFile.exists() || targetFile.length() < 1024L) {
                    val urlResult = repository.getDownloadUrl(material.storagePath)
                    if (urlResult.isFailure) {
                        downloadError = urlResult.exceptionOrNull()?.localizedMessage ?: "Failed to generate download link."
                        isDownloading = false
                        return@withContext
                    }

                    val signedUrl = urlResult.getOrThrow()
                    val client = OkHttpClient()
                    val req = Request.Builder().url(signedUrl).build()
                    val response = client.newCall(req).execute()

                    if (!response.isSuccessful) {
                        downloadError = "Server returned HTTP ${response.code}"
                        isDownloading = false
                        return@withContext
                    }

                    val inputStream = response.body?.byteStream() ?: throw Exception("Empty response body")
                    val outputStream = FileOutputStream(targetFile)
                    inputStream.use { input ->
                        outputStream.use { output ->
                            input.copyTo(output)
                        }
                    }
                }

                localPdfFile = targetFile
                val fd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fd)
                fileDescriptor = fd
                pdfRenderer = renderer
                totalPages = renderer.pageCount
                isDownloading = false

                withContext(Dispatchers.Main) {
                    renderPage(renderer, 0)
                }
            } catch (e: Exception) {
                downloadError = e.localizedMessage ?: "Failed to load PDF document."
                isDownloading = false
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (_: Exception) {}
        }
    }

    // Page jump dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page") },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Page number (1 to $totalPages)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val p = pageInput.toIntOrNull()
                        if (p != null && p in 1..totalPages && pdfRenderer != null) {
                            renderPage(pdfRenderer!!, p - 1)
                        }
                        showJumpDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = material.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (totalPages > 0) {
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("reader_back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Bookmark Button
                    IconButton(
                        onClick = {
                            val isBookmarked = bookmarks.contains(currentPageIndex)
                            bookmarks = if (isBookmarked) bookmarks - currentPageIndex else bookmarks + currentPageIndex
                            Toast.makeText(
                                context,
                                if (!isBookmarked) "Page ${currentPageIndex + 1} bookmarked" else "Bookmark removed",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.testTag("reader_bookmark_button")
                    ) {
                        Icon(
                            imageVector = if (bookmarks.contains(currentPageIndex)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (bookmarks.contains(currentPageIndex)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Night Mode Toggle
                    IconButton(
                        onClick = {
                            isNightMode = !isNightMode
                            if (pdfRenderer != null) {
                                renderPage(pdfRenderer!!, currentPageIndex)
                            }
                        },
                        modifier = Modifier.testTag("reader_night_mode_button")
                    ) {
                        Icon(
                            imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Night Mode"
                        )
                    }

                    // Jump Page
                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(Icons.Default.FindInPage, contentDescription = "Jump to page")
                    }
                }
            )
        },
        bottomBar = {
            if (totalPages > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                if (currentPageIndex > 0 && pdfRenderer != null) {
                                    renderPage(pdfRenderer!!, currentPageIndex - 1)
                                }
                            },
                            enabled = currentPageIndex > 0,
                            modifier = Modifier.testTag("reader_prev_page")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        Text(
                            text = "${currentPageIndex + 1} / $totalPages",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.clickable { showJumpDialog = true }
                        )

                        Button(
                            onClick = {
                                if (currentPageIndex < totalPages - 1 && pdfRenderer != null) {
                                    renderPage(pdfRenderer!!, currentPageIndex + 1)
                                }
                            },
                            enabled = currentPageIndex < totalPages - 1,
                            modifier = Modifier.testTag("reader_next_page")
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(if (isNightMode) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDownloading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Secure PDF from Cloud...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                downloadError != null -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Unable to Open Document",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = downloadError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                currentBitmap != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = transformState)
                            .clip(RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = currentBitmap!!.asImageBitmap(),
                            contentDescription = "PDF Page ${currentPageIndex + 1}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .testTag("pdf_rendered_page")
                        )
                    }
                }
            }
        }
    }
}
