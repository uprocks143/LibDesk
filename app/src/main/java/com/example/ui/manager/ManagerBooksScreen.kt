package com.example.ui.manager

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.local.entities.BookIssueEntity
import com.example.data.local.entities.PhysicalBookEntity
import com.example.data.local.entities.StudentEntity
import com.example.ui.components.EmptyPlaceholder
import com.example.ui.theme.*

@Composable
fun ManagerBooksScreen(
    books: List<PhysicalBookEntity>,
    bookIssues: List<BookIssueEntity>,
    students: List<StudentEntity>,
    onAddBook: (String, String, String, String, String, String, String, Int) -> Unit,
    onIssueBook: (PhysicalBookEntity, StudentEntity) -> Unit,
    onReturnBook: (BookIssueEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } 
    var searchQuery by remember { mutableStateOf("") }
    var showAddBookModal by remember { mutableStateOf(false) }
    var selectedBookToIssue by remember { mutableStateOf<PhysicalBookEntity?>(null) }

    val filteredBooks = remember(books, searchQuery) {
        if (searchQuery.isBlank()) books
        else books.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.author.contains(searchQuery, ignoreCase = true) ||
            it.category.contains(searchQuery, ignoreCase = true) ||
            it.rack.contains(searchQuery, ignoreCase = true)
        }
    }

    val activeIssues = remember(bookIssues) {
        bookIssues.filter { it.status == "ISSUED" }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddBookModal = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Book", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Book Inventory (${books.size})") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Issued / Loans (${activeIssues.size})") }
                )
            }

            if (selectedTab == 0) {

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search catalog by title, author, subject, rack...") },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (filteredBooks.isEmpty()) {
                    EmptyPlaceholder(
                        title = "No Books In Inventory",
                        description = "Add your library physical books to track circulation, racks, and accession.",
                        icon = Icons.Default.MenuBook,
                        actionLabel = "Add Book",
                        onAction = { showAddBookModal = true }
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredBooks, key = { it.id }) { book ->
                            BookInventoryCard(
                                book = book,
                                onIssue = { selectedBookToIssue = book }
                            )
                        }
                    }
                }
            } else {

                if (activeIssues.isEmpty()) {
                    EmptyPlaceholder(
                        title = "No Books Currently Issued",
                        description = "All physical library books are available in rack shelves.",
                        icon = Icons.Default.LibraryBooks
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeIssues, key = { it.id }) { issue ->
                            IssuedBookCard(
                                issue = issue,
                                onReturn = { onReturnBook(issue) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddBookModal) {
        AddBookDialog(
            onClose = { showAddBookModal = false },
            onConfirm = { title, author, isbn, cat, sub, rack, shelf, copies ->
                onAddBook(title, author, isbn, cat, sub, rack, shelf, copies)
                showAddBookModal = false
            }
        )
    }

    if (selectedBookToIssue != null) {
        IssueBookDialog(
            book = selectedBookToIssue!!,
            students = students,
            onClose = { selectedBookToIssue = null },
            onConfirm = { student ->
                onIssueBook(selectedBookToIssue!!, student)
                selectedBookToIssue = null
            }
        )
    }
}

@Composable
fun BookInventoryCard(
    book: PhysicalBookEntity,
    onIssue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.Book, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "By ${book.author}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                        Text(
                            text = "Category: ${book.category} • ${book.subject}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (book.availableCopies > 0) LibDeskColors.successSoft else MaterialTheme.colorScheme.errorContainer
                ) {
                    Text(
                        text = "${book.availableCopies}/${book.totalCopies} Available",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (book.availableCopies > 0) LibDeskColors.success else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Location: ${book.rack}, ${book.shelf} • Acc: ${book.accessionNumber}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Button(
                    onClick = onIssue,
                    enabled = book.availableCopies > 0,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(imageVector = Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Issue Book", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun IssuedBookCard(
    issue: BookIssueEntity,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = issue.bookTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Issued to: ${issue.studentName}",
                        style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    )
                }
                Button(
                    onClick = onReturn,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Return", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Issued: ${issue.issueDate}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Due: ${issue.dueDate}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun AddBookDialog(
    onClose: () -> Unit,
    onConfirm: (String, String, String, String, String, String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var isbn by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("UPSC / State PSC") }
    var subject by remember { mutableStateOf("General Studies") }
    var rack by remember { mutableStateOf("Rack A") }
    var shelf by remember { mutableStateOf("Shelf 1") }
    var copiesText by remember { mutableStateOf("2") }

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
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Add Book to Inventory",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book Title *") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author *") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = copiesText,
                        onValueChange = { copiesText = it },
                        label = { Text("Copies") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rack,
                        onValueChange = { rack = it },
                        label = { Text("Rack") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = shelf,
                        onValueChange = { shelf = it },
                        label = { Text("Shelf") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                            if (title.isNotBlank() && author.isNotBlank()) {
                                val copies = copiesText.toIntOrNull() ?: 1
                                onConfirm(title, author, isbn, category, subject, rack, shelf, copies)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        enabled = title.isNotBlank() && author.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Save Book")
                    }
                }
            }
        }
    }
}

@Composable
fun IssueBookDialog(
    book: PhysicalBookEntity,
    students: List<StudentEntity>,
    onClose: () -> Unit,
    onConfirm: (StudentEntity) -> Unit
) {
    var selectedStudentId by remember { mutableStateOf(students.firstOrNull()?.id ?: "") }

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
                    .padding(20.dp)
            ) {
                Text(
                    text = "Issue Book: ${book.title}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Text(
                    text = "Loan period: 14 days • Overdue fine: ₹5/day",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text("Select Student Borrower:", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

                students.forEach { st ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStudentId = st.id }
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = selectedStudentId == st.id,
                            onClick = { selectedStudentId = st.id }
                        )
                        Text("${st.fullName} (${st.studentCode})", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            val student = students.find { it.id == selectedStudentId }
                            if (student != null) onConfirm(student)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Issue Now")
                    }
                }
            }
        }
    }
}
