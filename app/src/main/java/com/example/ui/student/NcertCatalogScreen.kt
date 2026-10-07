package com.example.ui.student

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.download.DownloadState
import com.example.data.model.NcertBook
import com.example.data.repository.NcertRepository
import com.example.ui.common.NcertAttributionFooter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcertCatalogScreen(
    studentId: String,
    onNavigateBack: () -> Unit,
    onOpenBook: (NcertBook) -> Unit,
    modifier: Modifier = Modifier,
    repository: NcertRepository = NcertRepository(LocalContext.current)
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: In-App Textbooks, 1: Official Portal
    var searchQuery by remember { mutableStateOf("") }
    var selectedClass by remember { mutableStateOf<Int?>(10) } // Default Class 10
    var selectedMedium by remember { mutableStateOf("All") }
    var selectedSubject by remember { mutableStateOf("All") }

    val books by repository.filterCatalog(
        classLevel = selectedClass,
        subject = if (selectedSubject == "All") null else selectedSubject,
        medium = if (selectedMedium == "All") null else selectedMedium,
        query = searchQuery
    ).collectAsState(initial = emptyList())

    LaunchedEffect(Unit) {
        repository.refreshCatalog()
    }

    val classes = (1..12).toList()
    val mediums = listOf("All", "English", "Hindi", "Urdu")
    val quickSubjects = listOf(
        "All",
        "Science",
        "Mathematics",
        "Social Science",
        "Physics",
        "Chemistry",
        "Biology",
        "Hindi",
        "English",
        "Economics",
        "Commerce",
        "Sanskrit"
    )

    BackHandler {
        if (selectedTab == 1) {
            selectedTab = 0
        } else {
            onNavigateBack()
        }
    }

    if (selectedTab == 1) {
        NcertPortalWebViewScreen(
            onNavigateBack = { selectedTab = 0 },
            onOpenBookInReader = onOpenBook,
            modifier = modifier
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "NCERT Official Textbooks",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Class 1–12 • National Curriculum Framework (NEP 2020)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("ncert_catalog_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                repository.refreshCatalog()
                                Toast.makeText(context, "Textbook library synchronized.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Catalog")
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
            // Mode Tabs
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📚 In-App Textbooks (पढ़ें)", fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("🌐 Official Web Portal", fontWeight = FontWeight.Bold) }
                )
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isTabletOrLandscape = maxWidth >= 600.dp
                val gridColumns = when {
                    maxWidth >= 960.dp -> GridCells.Fixed(3)
                    maxWidth >= 600.dp -> GridCells.Fixed(2)
                    else -> GridCells.Fixed(1)
                }

                LazyVerticalGrid(
                    columns = gridColumns,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = if (isTabletOrLandscape) 24.dp else 16.dp),
                    contentPadding = PaddingValues(top = 12.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Box (Full Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear")
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ncert_search_input"),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp)
                        )
                    }

                    // Class Selector Chips (Full Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Select Class / Grade:",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (selectedClass != null) {
                                    TextButton(
                                        onClick = { selectedClass = null },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("All Classes", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(classes) { cls ->
                                    FilterChip(
                                        selected = selectedClass == cls,
                                        onClick = { selectedClass = if (selectedClass == cls) null else cls },
                                        label = { Text("Class $cls", fontWeight = if (selectedClass == cls) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Quick Subject Chips (Full Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Subject Filter:",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(quickSubjects) { subj ->
                                    FilterChip(
                                        selected = selectedSubject == subj,
                                        onClick = { selectedSubject = subj },
                                        label = { Text(subj, fontWeight = if (selectedSubject == subj) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Counter and Medium Filter Row (Full Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Showing ${books.size} Textbooks ${if (selectedClass != null) "for Class $selectedClass" else ""}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Medium Filter selector
                            var mediumMenuOpen by remember { mutableStateOf(false) }
                            Box {
                                FilledTonalButton(
                                    onClick = { mediumMenuOpen = true },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(selectedMedium, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                }
                                DropdownMenu(
                                    expanded = mediumMenuOpen,
                                    onDismissRequest = { mediumMenuOpen = false }
                                ) {
                                    mediums.forEach { med ->
                                        DropdownMenuItem(
                                            text = { Text(med) },
                                            onClick = {
                                                selectedMedium = med
                                                mediumMenuOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Empty state
                    if (books.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "No textbooks matching criteria",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Try clearing search or changing class/subject.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = {
                                            selectedClass = 10
                                            selectedSubject = "All"
                                            selectedMedium = "All"
                                            searchQuery = ""
                                        }
                                    ) {
                                        Text("Reset to Class 10")
                                    }
                                }
                            }
                        }
                    } else {
                        items(books, key = { it.id }) { book ->
                            CleanNcertBookCard(
                                book = book,
                                studentId = studentId,
                                repository = repository,
                                onOpen = { onOpenBook(book) }
                            )
                        }
                    }

                    // Attribution Footer (Full Span)
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        NcertAttributionFooter(modifier = Modifier.padding(top = 16.dp, bottom = 24.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun CleanNcertBookCard(
    book: NcertBook,
    studentId: String,
    repository: NcertRepository,
    onOpen: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var isDownloaded by remember { mutableStateOf(repository.isDownloaded(book)) }

    val subjectIcon: ImageVector = remember(book.subject) {
        when {
            book.subject.contains("Math", ignoreCase = true) -> Icons.Default.Calculate
            book.subject.contains("Science", ignoreCase = true) || book.subject.contains("Physics", ignoreCase = true) -> Icons.Default.Science
            book.subject.contains("Chemistry", ignoreCase = true) -> Icons.Default.Biotech
            book.subject.contains("Biology", ignoreCase = true) -> Icons.Default.Eco
            book.subject.contains("Social", ignoreCase = true) || book.subject.contains("History", ignoreCase = true) -> Icons.Default.Public
            book.subject.contains("Hindi", ignoreCase = true) || book.subject.contains("English", ignoreCase = true) || book.subject.contains("Sanskrit", ignoreCase = true) -> Icons.Default.Translate
            else -> Icons.Default.MenuBook
        }
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpen() }
            .testTag("ncert_book_card_${book.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = subjectIcon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = book.bookTitle,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Class ${book.classLevel} • ${book.subject} (${book.medium})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "NEP 2026",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { downloadProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Preparing in-app textbook: $downloadProgress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDownloaded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = if (isDownloaded) "Offline Ready" else "Instant Reader",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDownloaded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!isDownloaded) {
                        IconButton(
                            onClick = {
                                if (isDownloading) return@IconButton
                                isDownloading = true
                                downloadProgress = 0

                                coroutineScope.launch {
                                    repository.logNcertDownload(studentId, book.id)
                                    repository.downloadBook(book).collect { state ->
                                        when (state) {
                                            is DownloadState.Progress -> {
                                                downloadProgress = state.percentage
                                            }
                                            is DownloadState.Success -> {
                                                isDownloading = false
                                                isDownloaded = true
                                                repository.markBookDownloaded(book.id, state.file)
                                            }
                                            is DownloadState.Error -> {
                                                isDownloading = false
                                            }
                                        }
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = onOpen,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("open_book_button_${book.id}")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Read in App (पढ़ें)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
