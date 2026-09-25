package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.data.download.NcertDownloadManager
import com.example.data.local.NcertBookDao
import com.example.data.model.NcertBook
import com.example.data.repository.NcertRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class NcertRepositoryTest {

    private lateinit var context: Context
    private lateinit var dao: NcertBookDao
    private lateinit var repository: NcertRepository
    private lateinit var downloadManager: NcertDownloadManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        dao = NcertBookDao(context)
        downloadManager = NcertDownloadManager(context)
        repository = NcertRepository(context, dao = dao, downloadManager = downloadManager)
    }

    @Test
    fun testNcertBookComputedDisplayProperties() {
        val book10 = NcertBook(
            id = "ncert-10-sci",
            classLevel = 10,
            subject = "Science",
            bookTitle = "Science - Class X",
            medium = "english",
            editionYear = "2026-27"
        )

        assertEquals("Class 10 • Science • English", book10.displayLabel)
        assertEquals("Valid for 2026-27 • NEP Revised coming 2027-28", book10.editionBadge)

        val book9 = NcertBook(
            id = "ncert-9-sci",
            classLevel = 9,
            subject = "Science",
            bookTitle = "Science - Class IX",
            medium = "english",
            editionYear = "2026-27"
        )
        assertEquals("Updated for 2026-27", book9.editionBadge)
    }

    @Test
    fun testCatalogFilteringAndLocalCaching() = runBlocking {
        val books = listOf(
            NcertBook(id = "b1", classLevel = 10, subject = "Science", bookTitle = "Science 10", medium = "english"),
            NcertBook(id = "b2", classLevel = 10, subject = "Mathematics", bookTitle = "Maths 10", medium = "hindi"),
            NcertBook(id = "b3", classLevel = 12, subject = "Physics", bookTitle = "Physics 12", medium = "english")
        )

        dao.insertAll(books)

        val class10Books = dao.filterBooks(classLevel = 10).first()
        assertEquals(2, class10Books.size)

        val class10English = dao.filterBooks(classLevel = 10, medium = "english").first()
        assertEquals(1, class10English.size)
        assertEquals("Science 10", class10English[0].bookTitle)
    }

    @Test
    fun testDestinationFilePathFormat() {
        val book = NcertBook(
            id = "test-b",
            classLevel = 10,
            subject = "Science",
            bookTitle = "Science",
            medium = "english"
        )
        val file = downloadManager.getLocalFileForBook(book)
        assertTrue(file.absolutePath.contains("ncert/class_10/Science"))
        assertTrue(file.name.endsWith(".pdf"))
    }
}
