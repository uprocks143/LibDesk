package com.example.ui.superadmin

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NcertBook
import com.example.data.remote.NcertCatalogDataSource
import com.example.data.repository.NcertRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NcertCatalogManagerScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: NcertRepository = NcertRepository(LocalContext.current),
    catalogDataSource: NcertCatalogDataSource = NcertCatalogDataSource()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isSyncing by remember { mutableStateOf(false) }
    var books by remember { mutableStateOf<List<NcertBook>>(emptyList()) }
    var bookToEditUrl by remember { mutableStateOf<NcertBook?>(null) }
    var newUrlInput by remember { mutableStateOf("") }

    fun reloadCatalog() {
        coroutineScope.launch {
            val res = catalogDataSource.fetchCatalog(includeInactive = true)
            if (res.isSuccess) {
                books = res.getOrThrow()
            }
        }
    }

    LaunchedEffect(Unit) {
        reloadCatalog()
    }

    // Edit URL Dialog
    if (bookToEditUrl != null) {
        AlertDialog(
            onDismissRequest = { bookToEditUrl = null },
            title = { Text("Update Official Source URL") },
            text = {
                Column {
                    Text(
                        text = "${bookToEditUrl?.bookTitle} (${bookToEditUrl?.displayLabel})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newUrlInput,
                        onValueChange = { newUrlInput = it },
                        label = { Text("Official Direct URL") },
                        placeholder = { Text("https://ncert.nic.in/textbook/pdf/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val book = bookToEditUrl!!
                        val url = newUrlInput.trim()
                        bookToEditUrl = null
                        if (url.isNotBlank()) {
                            coroutineScope.launch {
                                val res = repository.updateSourceUrl(book.id, url)
                                if (res.isSuccess) {
                                    Toast.makeText(context, "Official URL updated.", Toast.LENGTH_SHORT).show()
                                    reloadCatalog()
                                } else {
                                    Toast.makeText(context, "Failed to update URL.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Save URL")
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToEditUrl = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NCERT Catalog Management") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("superadmin_ncert_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Manual "Sync Now" button that triggers Supabase Edge Function
                    Button(
                        onClick = {
                            isSyncing = true
                            coroutineScope.launch {
                                val syncResult = repository.triggerSyncFromOfficial()
                                isSyncing = false
                                if (syncResult.isSuccess) {
                                    Toast.makeText(context, "Edge Function sync complete.", Toast.LENGTH_LONG).show()
                                    reloadCatalog()
                                } else {
                                    Toast.makeText(context, "Sync error: ${syncResult.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isSyncing,
                        modifier = Modifier.padding(end = 8.dp).testTag("sync_ncert_edge_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Syncing...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync Now", fontSize = 12.sp)
                        }
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Super Admin Legal & Catalog Governance",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Textbooks are strictly NEVER hosted on Supabase Storage. You can audit live official URLs, trigger the Supabase Edge Function 'sync-ncert-catalog', or toggle active listings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(books, key = { it.id }) { book ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth().testTag("manager_book_${book.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.bookTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = "${book.displayLabel} • Edition: ${book.editionYear}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (book.lastVerified.isNotBlank()) {
                                    Text(
                                        text = "Last verified: ${book.lastVerified.take(10)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            // Active / Inactive Toggle Switch
                            Switch(
                                checked = book.isActive,
                                onCheckedChange = { activeState ->
                                    coroutineScope.launch {
                                        repository.toggleActive(book.id, activeState)
                                        books = books.map { if (it.id == book.id) it.copy(isActive = activeState) else it }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Source: ${book.sourceUrl}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val isLive = repository.verifySourceUrl(book.sourceUrl)
                                        Toast.makeText(
                                            context,
                                            if (isLive) "✓ Official URL is live (HEAD 200 OK)" else "⚠️ Warning: URL returned error",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Check Link", fontSize = 12.sp)
                            }

                            FilledTonalButton(
                                onClick = {
                                    bookToEditUrl = book
                                    newUrlInput = book.sourceUrl
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Edit URL", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
