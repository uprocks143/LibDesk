package com.example.ui.student

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.NcertBook

data class OfficialPortalPreset(
    val name: String,
    val url: String,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun NcertPortalWebViewScreen(
    onNavigateBack: () -> Unit,
    onOpenBookInReader: (NcertBook) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val presets = remember {
        listOf(
            OfficialPortalPreset(
                name = "NCERT Textbooks",
                url = "https://ncert.nic.in/textbook.php",
                description = "Official NCERT e-Books Portal (Class 1–12)"
            ),
            OfficialPortalPreset(
                name = "ePathshala e-Books",
                url = "https://epathshala.nic.in//process.php?id=students&type=e-textbooks&ln=en",
                description = "Ministry of Education ePathshala Portal"
            ),
            OfficialPortalPreset(
                name = "DIKSHA Portal",
                url = "https://diksha.gov.in/explore",
                description = "National Digital Infrastructure for Teachers & Students"
            ),
            OfficialPortalPreset(
                name = "NCERT Exemplar",
                url = "https://ncert.nic.in/exemplar-problems.php",
                description = "NCERT Exemplar Problems & High-Order Thinking"
            ),
            OfficialPortalPreset(
                name = "CIET Digital Resources",
                url = "https://ciet.nic.in/",
                description = "Audio, Video & Interactive Digital Modules"
            )
        )
    }

    var selectedPresetIndex by remember { mutableIntStateOf(0) }
    var currentUrl by remember { mutableStateOf(presets[0].url) }
    var pageTitle by remember { mutableStateOf("NCERT Official Portal") }
    var isLoading by remember { mutableStateOf(true) }
    var progressPercent by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    var detectedPdfUrl by remember { mutableStateOf<String?>(null) }
    var detectedPdfTitle by remember { mutableStateOf("NCERT Textbook Chapter") }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Intercept hardware back button for WebView navigation
    BackHandler {
        if (webViewInstance?.canGoBack() == true) {
            webViewInstance?.goBack()
        } else {
            onNavigateBack()
        }
    }

    // PDF Interception Dialog
    if (detectedPdfUrl != null) {
        val pdfUrl = detectedPdfUrl!!
        AlertDialog(
            onDismissRequest = { detectedPdfUrl = null },
            icon = { Icon(Icons.Default.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Open Textbook PDF", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Official PDF detected: $detectedPdfTitle",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "You can read this chapter immediately in our fast In-App PDF Reader (with zoom & night mode) or download it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = pdfUrl,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = NcertBook(
                            id = "WEB-${System.currentTimeMillis()}",
                            classLevel = 10,
                            subject = "Official Portal",
                            bookTitle = detectedPdfTitle.ifBlank { "NCERT Official Chapter" },
                            sourceUrl = pdfUrl
                        )
                        detectedPdfUrl = null
                        onOpenBookInReader(book)
                    }
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Read in App")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(pdfUrl))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open external app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                        detectedPdfUrl = null
                    }
                ) {
                    Text("Open Externally")
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
                            text = pageTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = if (currentUrl.startsWith("https://")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = currentUrl,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Forward")
                    }
                    IconButton(
                        onClick = { webViewInstance?.reload() }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                    IconButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open browser: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Open in External Browser")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Portal Preset Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEachIndexed { index, preset ->
                    FilterChip(
                        selected = selectedPresetIndex == index,
                        onClick = {
                            selectedPresetIndex = index
                            currentUrl = preset.url
                            webViewInstance?.loadUrl(preset.url)
                        },
                        label = { Text(preset.name, fontSize = 13.sp) },
                        leadingIcon = {
                            if (selectedPresetIndex == index) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                }
            }

            // Real-time loading progress indicator
            if (isLoading && progressPercent < 100) {
                LinearProgressIndicator(
                    progress = { progressPercent / 100f },
                    modifier = Modifier.fillMaxWidth().height(3.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(3.dp))
            }

            // Embedded High-Performance WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )

                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                setSupportZoom(true)
                                builtInZoomControls = true
                                displayZoomControls = false
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                javaScriptCanOpenWindowsAutomatically = true
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                    url?.let { currentUrl = it }
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                    url?.let { currentUrl = it }
                                    pageTitle = view?.title ?: "NCERT Portal"
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val targetUrl = request?.url?.toString() ?: return false
                                    if (targetUrl.endsWith(".pdf", ignoreCase = true) || targetUrl.contains(".pdf?", ignoreCase = true)) {
                                        val filename = targetUrl.substringAfterLast("/").substringBefore("?")
                                        detectedPdfTitle = filename.ifBlank { "NCERT Official Chapter" }
                                        detectedPdfUrl = targetUrl
                                        return true
                                    }
                                    return false
                                }
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progressPercent = newProgress
                                    isLoading = newProgress < 100
                                    canGoBack = view?.canGoBack() == true
                                    canGoForward = view?.canGoForward() == true
                                }

                                override fun onReceivedTitle(view: WebView?, title: String?) {
                                    super.onReceivedTitle(view, title)
                                    title?.let { pageTitle = it }
                                }
                            }

                            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                                if (url.endsWith(".pdf", ignoreCase = true) || mimetype?.contains("pdf", ignoreCase = true) == true) {
                                    detectedPdfTitle = "NCERT PDF Textbook"
                                    detectedPdfUrl = url
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Download started: $url", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            loadUrl(currentUrl)
                            webViewInstance = this
                        }
                    },
                    update = { view ->
                        webViewInstance = view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
