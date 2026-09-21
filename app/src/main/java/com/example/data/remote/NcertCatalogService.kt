package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class NcertBookMetadata(
    val title: String,
    val category: String,
    val subject: String,
    val fileUrl: String
)

/**
 * Real official NCERT textbook PDFs, fetched from the actual government
 * source (ncert.nic.in) rather than fabricated or embedded in the app.
 *
 * NCERT hosts each chapter as its own PDF at a stable, long-standing URL
 * pattern: https://ncert.nic.in/textbook/pdf/{bookCode}{chapterNo}.pdf
 * (e.g. jesc101.pdf = Class 10 ("j") Science ("esc") book 1, chapter 01).
 * "ps" = prelims/front matter.
 *
 * Chapter counts differ per book and change whenever NCERT revises the
 * syllabus (classes 6-8 were fully renamed under NEP 2020 — e.g. Class 6
 * Science is now "Curiosity", code "fecu1", not the old "esc" code). Rather
 * than hardcode a chapter count that can silently go stale, each chapter
 * number is verified live with a HEAD request before being added, so the
 * catalog only ever contains links that actually exist right now.
 *
 * NOTE: only the book codes below have been verified against the live
 * ncert.nic.in site. Extending this to more subjects/classes just means
 * adding another BookDef with its verified code — open the book's page on
 * ncert.nic.in, copy a chapter's PDF link, and the prefix before the
 * 2-digit chapter number is the code.
 */
object NcertCatalogService {

    private data class BookDef(val code: String, val title: String, val category: String, val subject: String)

    private val KNOWN_BOOKS = listOf(
        BookDef("jesc1", "Science", "Class 10", "Science"),
        BookDef("jemh1", "Mathematics", "Class 10", "Mathematics"),
        BookDef("jess3", "India and the Contemporary World II (History)", "Class 10", "Social Science"),
        BookDef("jess4", "Democratic Politics II (Political Science)", "Class 10", "Social Science"),
        BookDef("fecu1", "Curiosity (Science)", "Class 6", "Science")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private fun exists(url: String): Boolean {
        return try {
            val request = Request.Builder().url(url).head().build()
            client.newCall(request).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun fetchNcertCatalog(): List<NcertBookMetadata> = withContext(Dispatchers.IO) {
        val result = mutableListOf<NcertBookMetadata>()

        for (book in KNOWN_BOOKS) {
            val base = "https://ncert.nic.in/textbook/pdf/${book.code}"

            // Front matter (prelims) — most books have this.
            if (exists("${base}ps.pdf")) {
                result.add(
                    NcertBookMetadata(
                        "${book.title} \u2013 ${book.category} (Front Matter)",
                        book.category,
                        book.subject,
                        "${base}ps.pdf"
                    )
                )
            }

            // Probe chapters 01..20 in parallel, then walk the results in
            // order and stop once 2 consecutive chapters are missing — this
            // adapts automatically to however many chapters a book actually
            // has instead of guessing a fixed count.
            val chapterChecks = (1..20).map { num ->
                async {
                    val chNum = num.toString().padStart(2, '0')
                    num to exists("$base$chNum.pdf")
                }
            }
            val checked = chapterChecks.awaitAll().sortedBy { it.first }

            var misses = 0
            for ((num, ok) in checked) {
                if (misses >= 2) break
                if (ok) {
                    misses = 0
                    val chNum = num.toString().padStart(2, '0')
                    result.add(
                        NcertBookMetadata(
                            "${book.title} \u2013 Chapter $num",
                            book.category,
                            book.subject,
                            "$base$chNum.pdf"
                        )
                    )
                } else {
                    misses++
                }
            }
        }

        result
    }
}
