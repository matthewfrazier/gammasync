package com.gammasync.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Document loading utility for RSVP text input.
 * Handles content:// URIs from the Android Storage Access Framework.
 */
object DocumentLoader {
    
    private const val TAG = "DocumentLoader"
    private const val MAX_FILE_SIZE = 1024 * 1024 // 1MB limit
    
    data class DocumentInfo(
        val filename: String,
        val text: String,
        val wordCount: Int
    )
    
    /**
     * Load and process text document from content URI.
     * Returns null if loading fails or file is too large.
     */
    suspend fun loadDocument(context: Context, uri: Uri): DocumentInfo? = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            
            // Get filename from URI
            val filename = getFilename(context, uri) ?: "Unknown Document"
            Log.d(TAG, "Loading document: $filename")
            
            // Check file size
            val cursor = contentResolver.query(uri, null, null, null, null)
            val fileSize = cursor?.use { c ->
                val sizeIndex = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (c.moveToFirst() && sizeIndex != -1) c.getLong(sizeIndex) else 0L
            } ?: 0L
            
            if (fileSize > MAX_FILE_SIZE) {
                Log.w(TAG, "File too large: ${fileSize / 1024}KB")
                return@withContext null
            }
            
            // Read file content
            val rawText = contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader().use { reader ->
                    reader.readText()
                }
            } ?: ""
            
            if (rawText.isEmpty()) {
                Log.w(TAG, "Empty file")
                return@withContext null
            }
            
            // Process text
            val cleanedText = TextProcessor.sanitize(rawText)
            val words = TextProcessor.getWords(cleanedText)
            
            Log.d(TAG, "Loaded ${words.size} words from $filename")
            
            DocumentInfo(
                filename = filename,
                text = cleanedText,
                wordCount = words.size
            )
            
        } catch (e: IOException) {
            Log.e(TAG, "Failed to load document", e)
            null
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission denied", e)
            null
        }
    }
    
    private fun getFilename(context: Context, uri: Uri): String? {
        return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex != -1) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        }
    }
}