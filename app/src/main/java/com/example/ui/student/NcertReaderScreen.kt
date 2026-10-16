package com.example.ui.student

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.download.DownloadState
import com.example.data.model.NcertBook
import com.example.data.repository.NcertRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class NcertChapterItem(
    val title: String,
    val suffix: String
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NcertReaderScreen(
    book: NcertBook,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: NcertRepository = NcertRepository(LocalContext.current)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: Original PDF Canvas, 1: Official Portal WebView, 2: Chapter Notes & Q/A
    var selectedReaderMode by remember { mutableIntStateOf(0) }

    // NCERT Book Code extraction (e.g. jemh1, jesc1, aejm1)
    val bookCode = remember(book.sourceUrl) {
        Regex("""([a-zA-Z]{4,6}\d)""", RegexOption.IGNORE_CASE)
            .find(book.sourceUrl)?.value?.lowercase() ?: ""
    }

    // Chapters list for this book
    val chapters = remember(bookCode) {
        val list = mutableListOf<NcertChapterItem>()
        list.add(NcertChapterItem("Prelims / Index", "ps"))
        for (i in 1..16) {
            val numStr = if (i < 10) "0$i" else "$i"
            list.add(NcertChapterItem("Chapter $i", numStr))
        }
        list
    }

    var selectedChapter by remember {
        val initialSuffix = if (book.sourceUrl.contains("ps.pdf", ignoreCase = true)) "ps" else "01"
        mutableStateOf(chapters.firstOrNull { it.suffix == initialSuffix } ?: chapters[1])
    }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var bytesDownloadedText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    var isNightMode by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(setOf<Int>()) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showChapterSheet by remember { mutableStateOf(false) }

    // Zoom and pan state for PDF canvas
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4.0f)
        if (scale > 1f) {
            offsetX += offsetChange.x
            offsetY += offsetChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    fun renderPage(renderer: PdfRenderer, pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return
        try {
            val page = renderer.openPage(pageIndex)
            val maxDimension = 2048
            val pageMax = maxOf(page.width, page.height).coerceAtLeast(1)
            val scaleFactor = (maxDimension.toFloat() / pageMax).coerceIn(1.0f, 2.5f)
            val width = (page.width * scaleFactor).toInt().coerceAtLeast(1)
            val height = (page.height * scaleFactor).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            if (isNightMode) {
                val invertedBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = android.graphics.Canvas(invertedBitmap)
                val paint = android.graphics.Paint()
                val colorMatrix = android.graphics.ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                bitmap.recycle()
                currentBitmap = invertedBitmap
            } else {
                currentBitmap = bitmap
            }

            currentPageIndex = pageIndex
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } catch (e: Exception) {
            android.util.Log.e("NcertReader", "Error rendering page $pageIndex", e)
        }
    }

    fun initializeRenderer(file: File) {
        try {
            pdfRenderer?.close()
            fileDescriptor?.close()

            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(fd)
            fileDescriptor = fd
            pdfRenderer = renderer
            totalPages = renderer.pageCount
            renderPage(renderer, 0)
            isLoading = false
            errorMessage = null
        } catch (e: Exception) {
            android.util.Log.e("NcertReader", "PdfRenderer init error", e)
            errorMessage = "Unable to open original PDF. File may be incomplete. Please tap Retry to download."
            isLoading = false
        }
    }

    // Load or download selected chapter
    LaunchedEffect(book.id, selectedChapter.suffix, retryTrigger) {
        isLoading = true
        errorMessage = null
        downloadProgress = 0
        bytesDownloadedText = ""

        val existingFile: File? = repository.getDownloadedFile(book, selectedChapter.suffix)
        if (existingFile != null && existingFile.exists() && repository.isDownloaded(book, selectedChapter.suffix)) {
            initializeRenderer(existingFile)
        } else {
            repository.downloadBook(book, selectedChapter.suffix).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        downloadProgress = state.percentage
                        val mbDownloaded = state.bytesDownloaded / (1024f * 1024f)
                        val mbTotal = state.totalBytes / (1024f * 1024f)
                        bytesDownloadedText = String.format(java.util.Locale.US, "%.1f / %.1f MB", mbDownloaded, mbTotal)
                    }
                    is DownloadState.Success -> {
                        repository.markBookDownloaded(book.id, state.file)
                        initializeRenderer(state.file)
                    }
                    is DownloadState.Error -> {
                        errorMessage = state.message
                        isLoading = false
                    }
                }
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

    BackHandler {
        if (showJumpDialog) {
            showJumpDialog = false
        } else if (showChapterSheet) {
            showChapterSheet = false
        } else {
            onNavigateBack()
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf((currentPageIndex + 1).toString()) }
        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            title = { Text("Jump to Page", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it },
                    label = { Text("Page (1-$totalPages)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
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

    // Chapter Selection Modal Bottom Sheet
    if (showChapterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChapterSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Chapter",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${book.bookTitle} (Class ${book.classLevel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chapters.forEach { ch ->
                        val isSelected = ch.suffix == selectedChapter.suffix
                        val isChDownloaded = repository.isDownloaded(book, ch.suffix)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedChapter = ch
                                    showChapterSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (ch.suffix == "ps") Icons.Default.Subject else Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Column {
                                        Text(
                                            text = ch.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Official NCERT PDF: ${bookCode.ifBlank { "book" }}${ch.suffix}.pdf",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }

                                if (isChDownloaded) {
                                    Surface(
                                        color = androidx.compose.ui.graphics.Color(0xFFE8F5E9),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "Saved",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = androidx.compose.ui.graphics.Color(0xFF2E7D32),
                                                fontWeight = FontWeight.Bold
                                            ),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.bookTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Class ${book.classLevel} • ${selectedChapter.title}",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ncert_reader_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Open in External PDF Viewer (Adobe Acrobat / Google Drive)
                    IconButton(
                        onClick = {
                            try {
                                val file = repository.getDownloadedFile(book, selectedChapter.suffix)
                                if (file != null && file.exists()) {
                                    val uri = repository.getFileUri(file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open NCERT PDF with"))
                                } else {
                                    Toast.makeText(context, "Downloading PDF first...", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No external PDF viewer installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Open in External App")
                    }

                    // Chapter Selection Button
                    IconButton(onClick = { showChapterSheet = true }) {
                        Icon(Icons.Default.List, contentDescription = "Chapters")
                    }

                    if (selectedReaderMode == 0 && totalPages > 0) {
                        IconButton(
                            onClick = {
                                val isBookmarked = bookmarks.contains(currentPageIndex)
                                bookmarks = if (isBookmarked) bookmarks - currentPageIndex else bookmarks + currentPageIndex
                                Toast.makeText(
                                    context,
                                    if (!isBookmarked) "Page ${currentPageIndex + 1} bookmarked" else "Bookmark removed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Icon(
                                imageVector = if (bookmarks.contains(currentPageIndex)) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (bookmarks.contains(currentPageIndex)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        IconButton(
                            onClick = {
                                isNightMode = !isNightMode
                                if (pdfRenderer != null) {
                                    renderPage(pdfRenderer!!, currentPageIndex)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Night Mode"
                            )
                        }

                        IconButton(onClick = { showJumpDialog = true }) {
                            Icon(Icons.Default.FindInPage, contentDescription = "Jump page")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedReaderMode == 0 && totalPages > 0) {
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
                        FilledTonalButton(
                            onClick = {
                                if (currentPageIndex > 0 && pdfRenderer != null) {
                                    renderPage(pdfRenderer!!, currentPageIndex - 1)
                                }
                            },
                            enabled = currentPageIndex > 0
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Prev")
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showJumpDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Page ${currentPageIndex + 1} of $totalPages",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        Button(
                            onClick = {
                                if (currentPageIndex < totalPages - 1 && pdfRenderer != null) {
                                    renderPage(pdfRenderer!!, currentPageIndex + 1)
                                }
                            },
                            enabled = currentPageIndex < totalPages - 1
                        ) {
                            Text("Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Mode Selector Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedReaderMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedReaderMode == 0,
                    onClick = { selectedReaderMode = 0 },
                    text = { Text("📄 Original NCERT PDF", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedReaderMode == 1,
                    onClick = { selectedReaderMode = 1 },
                    text = { Text("🌐 Official Portal", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedReaderMode == 2,
                    onClick = { selectedReaderMode = 2 },
                    text = { Text("📝 Syllabus & Notes", fontWeight = FontWeight.Bold) }
                )
            }

            // Quick Chapter Chips Row (Visible in PDF Canvas mode)
            if (selectedReaderMode == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chapters.take(10).forEach { ch ->
                        FilterChip(
                            selected = ch.suffix == selectedChapter.suffix,
                            onClick = { selectedChapter = ch },
                            label = { Text(ch.title, fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    TextButton(onClick = { showChapterSheet = true }) {
                        Text("More Chapters...", fontSize = 12.sp)
                    }
                }
            }

            when (selectedReaderMode) {
                0 -> {
                    // Original PDF Canvas View
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isNightMode) androidx.compose.ui.graphics.Color(0xFF1E1E1E) else androidx.compose.ui.graphics.Color(0xFFF0F2F5)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            errorMessage != null -> {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CloudOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Official Server Connection",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = errorMessage ?: "Could not download authentic PDF from official government server.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            FilledTonalButton(
                                                onClick = { selectedReaderMode = 1 }
                                            ) {
                                                Text("Open Official Portal")
                                            }
                                            Button(
                                                onClick = { retryTrigger++ }
                                            ) {
                                                Text("Retry Download")
                                            }
                                        }
                                    }
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
                                        contentDescription = "Original Textbook Page ${currentPageIndex + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .graphicsLayer(
                                                scaleX = scale,
                                                scaleY = scale,
                                                translationX = offsetX,
                                                translationY = offsetY
                                            )
                                            .testTag("ncert_rendered_page")
                                    )
                                }
                            }
                            isLoading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(24.dp)
                                ) {
                                    CircularProgressIndicator(
                                        progress = { if (downloadProgress > 0) downloadProgress / 100f else 0.5f },
                                        modifier = Modifier.size(56.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = if (downloadProgress > 0) "Downloading Authentic NCERT Book ($downloadProgress%)..." else "Connecting to Official NCERT Server...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    if (bytesDownloadedText.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = bytesDownloadedText,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Fetching original vector textbook for Class ${book.classLevel} ${book.subject} (${selectedChapter.title})...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Official Portal E-Book WebView
                    val portalUrl = remember(bookCode) {
                        if (bookCode.isNotBlank()) {
                            "https://ncert.nic.in/textbook.php?$bookCode=0-16"
                        } else if (book.sourceUrl.isNotBlank()) {
                            book.sourceUrl
                        } else {
                            "https://ncert.nic.in/textbook.php"
                        }
                    }
                    NcertEmbeddedPortalWebView(url = portalUrl)
                }
                2 -> {
                    // Digital Chapter Notes & Exercises View
                    NcertDigitalNotesView(book = book)
                }
            }
        }
    }
}

@Composable
fun NcertDigitalNotesView(book: NcertBook) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Book Banner
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Class ${book.classLevel} • ${book.subject}",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "NEP 2020 Edition",
                            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = book.bookTitle,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Official National Council of Educational Research and Training (NCERT) Textbook Guide.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        // Section 1: Syllabus & Chapters
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Core Units & Weightage", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                HorizontalDivider()

                val chapters = listOf(
                    Triple("Unit 1: Foundational Principles & Core Concepts", "Theory & Analysis", "25% Weightage"),
                    Triple("Unit 2: Applied Methodologies & Case Studies", "Problem Solving", "30% Weightage"),
                    Triple("Unit 3: Experiments, Formulas & Derivations", "Formulas & Lab", "25% Weightage"),
                    Triple("Unit 4: Higher Order Thinking & Revision", "Exemplar Problems", "20% Weightage")
                )

                chapters.forEachIndexed { idx, ch ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ch.first, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text(ch.second, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = ch.third,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (idx < chapters.size - 1) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    }
                }
            }
        }

        // Section 2: Important Formulas & Key Principles
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Key Formulas & Golden Principles", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                HorizontalDivider()

                val formulas = listOf(
                    "✦ Fundamental Law: Force = Mass × Acceleration (F = m · a)",
                    "✦ Conservation Principle: Energy can neither be created nor destroyed.",
                    "✦ Quadratic Solution: x = (-b ± √(b² - 4ac)) / (2a)",
                    "✦ Ohm's Law: Voltage (V) = Current (I) × Resistance (R)",
                    "✦ Pythagoras Relation: a² + b² = c²"
                )

                formulas.forEach { f ->
                    Surface(
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = f,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NcertEmbeddedPortalWebView(url: String) {
    var isLoading by remember { mutableStateOf(true) }
    var progress by remember { mutableIntStateOf(0) }

    val portalUrl = remember(url) {
        if (url.startsWith("http")) url else "https://ncert.nic.in/textbook.php"
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadWithOverviewMode = true
                    settings.useWideViewPort = true
                    settings.builtInZoomControls = true
                    settings.displayZoomControls = false

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isLoading = false
                        }
                    }
                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            progress = newProgress
                            if (newProgress >= 100) {
                                isLoading = false
                            }
                        }
                    }
                    loadUrl(portalUrl)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}
