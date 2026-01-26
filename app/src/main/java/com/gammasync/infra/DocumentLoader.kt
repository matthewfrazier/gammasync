package com.gammasync.infra

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.gammasync.domain.text.TextProcessor
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Loads and processes text documents for RSVP display.
 *
 * Supports:
 * - Plain text files (.txt)
 * - Markdown files (.md)
 *
 * Uses Android's ContentResolver to handle content:// URIs from file picker.
 */
class DocumentLoader(private val context: Context) {

    companion object {
        private const val TAG = "DocumentLoader"

        // Supported MIME types for file picker
        val SUPPORTED_MIME_TYPES = arrayOf(
            "text/plain",
            "text/markdown",
            "text/x-markdown"
        )

        // Max file size to prevent OOM (1 MB)
        private const val MAX_FILE_SIZE = 1024 * 1024
    }

    /**
     * Result of loading a document.
     */
    sealed class LoadResult {
        data class Success(
            val fileName: String,
            val words: List<String>,
            val isMarkdown: Boolean
        ) : LoadResult()

        data class Error(val message: String) : LoadResult()
    }

    /**
     * Load and process a document from a content URI.
     *
     * @param uri Content URI from file picker (ACTION_OPEN_DOCUMENT)
     * @return LoadResult with processed words or error
     */
    fun loadDocument(uri: Uri): LoadResult {
        return try {
            val contentResolver = context.contentResolver

            // Get file metadata
            val fileName = getFileName(contentResolver, uri)
            val fileSize = getFileSize(contentResolver, uri)

            Log.i(TAG, "Loading document: $fileName (${fileSize} bytes)")

            // Check file size
            if (fileSize > MAX_FILE_SIZE) {
                return LoadResult.Error("File too large. Maximum size is 1 MB.")
            }

            // Read file content
            val content = readContent(contentResolver, uri)
            if (content.isBlank()) {
                return LoadResult.Error("File is empty.")
            }

            // Determine if markdown
            val isMarkdown = fileName.endsWith(".md", ignoreCase = true) ||
                    contentResolver.getType(uri)?.contains("markdown") == true

            // Process text for RSVP
            val words = TextProcessor.processForRsvp(content, isMarkdown)

            if (words.isEmpty()) {
                return LoadResult.Error("No readable text found in file.")
            }

            Log.i(TAG, "Loaded $fileName: ${words.size} words, markdown=$isMarkdown")

            LoadResult.Success(
                fileName = fileName,
                words = words,
                isMarkdown = isMarkdown
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied reading document", e)
            LoadResult.Error("Permission denied. Please try again.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load document", e)
            LoadResult.Error("Failed to load file: ${e.message}")
        }
    }

    /**
     * Get the display name of a file from its URI.
     */
    private fun getFileName(resolver: ContentResolver, uri: Uri): String {
        var name = "document.txt"

        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: name
                }
            }
        }

        return name
    }

    /**
     * Get the size of a file from its URI.
     */
    private fun getFileSize(resolver: ContentResolver, uri: Uri): Long {
        var size = 0L

        resolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        return size
    }

    /**
     * Read the text content of a file.
     */
    private fun readContent(resolver: ContentResolver, uri: Uri): String {
        return resolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                reader.readText()
            }
        } ?: ""
    }
}
