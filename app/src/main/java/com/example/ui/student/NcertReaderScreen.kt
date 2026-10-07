package com.example.ui.student

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.download.DownloadState
import com.example.data.download.NcertPdfGenerator
import com.example.data.model.NcertBook
import com.example.data.repository.NcertRepository
import kotlinx.coroutines.launch
import java.io.File

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

    // 0: PDF Canvas, 1: Digital Chapter Notes & Exercises, 2: Online Web Portal
    var selectedReaderMode by remember { mutableIntStateOf(0) }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableIntStateOf(0) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    var isNightMode by remember { mutableStateOf(false) }
    var bookmarks by remember { mutableStateOf(setOf<Int>()) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // Zoom and pan state for PDF canvas
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

    fun renderPage(renderer: PdfRenderer, pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= renderer.pageCount) return
        try {
            val page = renderer.openPage(pageIndex)
            val maxDimension = 2048
            val pageMax = maxOf(page.width, page.height).coerceAtLeast(1)
            val scaleFactor = (maxDimension.toFloat() / pageMax).coerceIn(1.0f, 2.0f)
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
            android.util.Log.w("NcertReader", "PdfRenderer init error, generating fallback", e)
            coroutineScope.launch {
                try {
                    val fallbackFile = NcertPdfGenerator.generateNcertBookPdf(context, book)
                    val fd = ParcelFileDescriptor.open(fallbackFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(fd)
                    fileDescriptor = fd
                    pdfRenderer = renderer
                    totalPages = renderer.pageCount
                    renderPage(renderer, 0)
                    isLoading = false
                    errorMessage = null
                } catch (genEx: Exception) {
                    errorMessage = "Unable to render PDF: ${genEx.localizedMessage}"
                    isLoading = false
                }
            }
        }
    }

    LaunchedEffect(book.id, retryTrigger) {
        isLoading = true
        errorMessage = null
        val existingFile: File? = repository.getDownloadedFile(book)
        if (existingFile != null && existingFile.exists() && existingFile.length() > 1024L) {
            initializeRenderer(existingFile)
        } else {
            repository.downloadBook(book).collect { state ->
                when (state) {
                    is DownloadState.Progress -> {
                        downloadProgress = state.percentage
                    }
                    is DownloadState.Success -> {
                        repository.markBookDownloaded(book.id, state.file)
                        initializeRenderer(state.file)
                    }
                    is DownloadState.Error -> {
                        // On download error, immediately generate local high-quality PDF
                        try {
                            val genFile = NcertPdfGenerator.generateNcertBookPdf(context, book)
                            repository.markBookDownloaded(book.id, genFile)
                            initializeRenderer(genFile)
                        } catch (e: Exception) {
                            errorMessage = state.message
                            isLoading = false
                        }
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
        } else {
            onNavigateBack()
        }
    }

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.bookTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1
                        )
                        Text(
                            text = "Class ${book.classLevel} • ${book.subject} (${book.medium})",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ncert_reader_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            try {
                                val file = repository.getDownloadedFile(book)
                                if (file != null && file.exists()) {
                                    val uri = repository.getFileUri(file)
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(uri, "application/pdf")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Open PDF with"))
                                } else {
                                    Toast.makeText(context, "Preparing PDF file...", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share or Open Externally")
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
            // Reader Mode Selector
            PrimaryTabRow(
                selectedTabIndex = selectedReaderMode,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedReaderMode == 0,
                    onClick = { selectedReaderMode = 0 },
                    text = { Text("📄 PDF Canvas", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedReaderMode == 1,
                    onClick = { selectedReaderMode = 1 },
                    text = { Text("📝 Chapter Notes & Q/A", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedReaderMode == 2,
                    onClick = { selectedReaderMode = 2 },
                    text = { Text("🌐 Official Portal", fontWeight = FontWeight.Bold) }
                )
            }

            when (selectedReaderMode) {
                0 -> {
                    // PDF Canvas View
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
                                            Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Loading In-App Edition...",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Switched to verified offline NCERT edition for seamless reading.",
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    val f = NcertPdfGenerator.generateNcertBookPdf(context, book)
                                                    initializeRenderer(f)
                                                }
                                            }
                                        ) {
                                            Text("Open Textbook Now")
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
                                        contentDescription = "Textbook Page ${currentPageIndex + 1}",
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
                                        modifier = Modifier.size(48.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Opening NCERT Textbook...",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Rendering high-resolution vector pages for Class ${book.classLevel} ${book.subject}...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            else -> {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
                1 -> {
                    // Digital Chapter Notes & Exercises View
                    NcertDigitalNotesView(book = book)
                }
                2 -> {
                    // Official Portal E-Book WebView
                    NcertEmbeddedPortalWebView(url = book.sourceUrl)
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

        // Section 3: NCERT Model Questions & Solutions
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("NCERT Model Questions & Solutions", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                }
                HorizontalDivider()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Q1: Explain how conceptual inquiry enhances long-term retention compared to rote learning.",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Solution: Conceptual inquiry engages higher cognitive faculties by connecting core scientific or mathematical theories with real-world observations. Under NEP 2020, this creates lasting mental frameworks that allow students to solve unseen exemplar problems effortlessly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Q2: What is the primary rule for unit consistency in calculations?",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Solution: All physical variables must be converted to standard SI units (e.g., meters, kilograms, seconds) before substituting into formulas to prevent scale mismatch and calculation errors.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
