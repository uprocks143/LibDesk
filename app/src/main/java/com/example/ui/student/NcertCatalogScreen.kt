package com.example.ui.student

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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

    // Initial background catalog refresh
    LaunchedEffect(Unit) {
        repository.refreshCatalog()
    }

    val classes = (1..12).toList()
    val mediums = listOf("All", "English", "Hindi", "Urdu")
    val subjects = listOf("All", "Science", "Mathematics", "Social Science", "Physics", "Chemistry", "Biology", "English", "Hindi")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NCERT Official Textbooks") },
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
                                Toast.makeText(context, "Catalog refreshed from official repository.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search textbook title or subject...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ncert_search_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Class Filter Chips
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Select Class:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(classes) { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = { selectedClass = if (selectedClass == cls) null else cls },
                                label = { Text("Class $cls") }
                            )
                        }
                    }
                }
            }

            // Medium & Subject Dropdowns Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Medium Dropdown
                    var mediumExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedMedium,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Medium") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { mediumExpanded = !mediumExpanded }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { mediumExpanded = true }
                        )
                        DropdownMenu(
                            expanded = mediumExpanded,
                            onDismissRequest = { mediumExpanded = false }
                        ) {
                            mediums.forEach { med ->
                                DropdownMenuItem(
                                    text = { Text(med) },
                                    onClick = {
                                        selectedMedium = med
                                        mediumExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Subject Dropdown
                    var subjectExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = selectedSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Subject") },
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.clickable { subjectExpanded = !subjectExpanded }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().clickable { subjectExpanded = true }
                        )
                        DropdownMenu(
                            expanded = subjectExpanded,
                            onDismissRequest = { subjectExpanded = false }
                        ) {
                            subjects.forEach { subj ->
                                DropdownMenuItem(
                                    text = { Text(subj) },
                                    onClick = {
                                        selectedSubject = subj
                                        subjectExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (books.isEmpty()) {
                item {
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
                                text = "Try changing Class, Medium, or clearing filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            } else {
                items(books, key = { it.id }) { book ->
                    NcertBookCard(
                        book = book,
                        studentId = studentId,
                        repository = repository,
                        onOpen = { onOpenBook(book) }
                    )
                }
            }

            // Task 2: Official Attribution Footer
            item {
                NcertAttributionFooter(modifier = Modifier.padding(top = 10.dp, bottom = 20.dp))
            }
        }
    }
}

@Composable
fun NcertBookCard(
    book: NcertBook,
    studentId: String,
    repository: NcertRepository,
    onOpen: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableIntStateOf(0) }
    var isAlreadyDownloaded by remember { mutableStateOf(repository.isDownloaded(book)) }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("ncert_book_card_${book.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.bookTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = book.displayLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // NEP 2020 Edition Awareness Badge
                val isNew = book.isNewEdition2026
                Surface(
                    color = if (isNew) Color(0xFFE8F5E9) else Color(0xFFFFF9C4),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = book.editionBadge,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isNew) Color(0xFF2E7D32) else Color(0xFFF57F17),
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download Progress Bar if actively downloading
            if (isDownloading) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LinearProgressIndicator(
                        progress = { downloadProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Downloading from official NCERT server: $downloadProgress%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

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
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = book.formattedSize,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                if (isAlreadyDownloaded) {
                    Button(
                        onClick = onOpen,
                        modifier = Modifier.testTag("open_book_button_${book.id}")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Read Textbook")
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            if (isDownloading) return@FilledTonalButton
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
                                            isAlreadyDownloaded = true
                                            repository.markBookDownloaded(book.id, state.file)
                                        }
                                        is DownloadState.Error -> {
                                            isDownloading = false
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.testTag("download_book_button_${book.id}")
                    ) {
                        if (isDownloading) {
                            Text("Downloading...")
                        } else {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download PDF")
                        }
                    }
                }
            }
        }
    }
}
