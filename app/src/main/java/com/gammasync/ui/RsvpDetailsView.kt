package com.gammasync.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.gammasync.R
import com.gammasync.data.ColorScheme
import com.gammasync.data.SettingsRepository
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.textfield.TextInputEditText
import com.gammasync.domain.rsvp.ProcessedDocument
import com.gammasync.domain.rsvp.RsvpSettings
import com.gammasync.utils.TextProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

/**
 * RSVP details screen for document selection and settings.
 *
 * Supports three input modes:
 * - File: Select a local file via SAF
 * - URL: Fetch text content from a URL
 * - Paste: Paste text directly
 *
 * Shows a preview of the document with word count and time estimate,
 * and allows configuration of display settings.
 */
class RsvpDetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "RsvpDetailsView"
    }

    // Tabs
    private val tabFile: TextView
    private val tabUrl: TextView
    private val tabPaste: TextView

    // Input containers
    private val fileInputContainer: LinearLayout
    private val urlInputContainer: LinearLayout
    private val pasteInputContainer: LinearLayout

    // File input
    private val btnSelectFile: MaterialButton
    private val txtSelectedFile: TextView

    // URL input
    private val inputUrl: TextInputEditText
    private val btnFetchUrl: MaterialButton

    // Paste input
    private val inputPaste: TextInputEditText
    private val btnUsePaste: MaterialButton

    // Preview
    private val previewCard: CardView
    private val txtPreviewTitle: TextView
    private val txtPreviewStats: TextView
    private val txtPreviewText: TextView

    // RSVP Settings Link
    private val rsvpSettingsLink: TextView

    // Action
    private val btnBack: ImageButton
    private val btnUseDocument: MaterialButton

    // State
    private var currentTab = Tab.FILE
    private var processedDocument: ProcessedDocument? = null
    private var rawText: String? = null
    private var accentColor = ColorScheme.TEAL.accentColor

    // Coroutines for async URL fetching
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Callbacks
    var onBackPressed: (() -> Unit)? = null
    var onDocumentSelected: ((ProcessedDocument) -> Unit)? = null
    var onFileSelectRequested: (() -> Unit)? = null
    var onRsvpSettingsClicked: (() -> Unit)? = null

    enum class Tab { FILE, URL, PASTE }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_rsvp_details, this, true)

        // Find views
        tabFile = findViewById(R.id.tabFile)
        tabUrl = findViewById(R.id.tabUrl)
        tabPaste = findViewById(R.id.tabPaste)

        fileInputContainer = findViewById(R.id.fileInputContainer)
        urlInputContainer = findViewById(R.id.urlInputContainer)
        pasteInputContainer = findViewById(R.id.pasteInputContainer)

        btnSelectFile = findViewById(R.id.btnSelectFile)
        txtSelectedFile = findViewById(R.id.txtSelectedFile)

        inputUrl = findViewById(R.id.inputUrl)
        btnFetchUrl = findViewById(R.id.btnFetchUrl)

        inputPaste = findViewById(R.id.inputPaste)
        btnUsePaste = findViewById(R.id.btnUsePaste)

        previewCard = findViewById(R.id.previewCard)
        txtPreviewTitle = findViewById(R.id.txtPreviewTitle)
        txtPreviewStats = findViewById(R.id.txtPreviewStats)
        txtPreviewText = findViewById(R.id.txtPreviewText)

        rsvpSettingsLink = findViewById(R.id.rsvpSettingsLink)

        btnBack = findViewById(R.id.btnBack)
        btnUseDocument = findViewById(R.id.btnUseDocument)

        setupTabListeners()
        setupInputListeners()
        setupActionListeners()
        loadDefaultPasteText()
    }

    /**
     * Load default RSVP text (README) into paste field.
     */
    private fun loadDefaultPasteText() {
        try {
            val inputStream = resources.openRawResource(R.raw.default_rsvp_text)
            val defaultText = inputStream.bufferedReader().use { it.readText() }
            inputPaste.setText(defaultText)
        } catch (e: Exception) {
            Log.w("RsvpDetailsView", "Could not load default RSVP text", e)
        }
    }

    private fun setupTabListeners() {
        tabFile.setOnClickListener { selectTab(Tab.FILE) }
        tabUrl.setOnClickListener { selectTab(Tab.URL) }
        tabPaste.setOnClickListener { selectTab(Tab.PASTE) }
    }

    private fun selectTab(tab: Tab) {
        currentTab = tab

        // Get theme-aware colors
        val onSurfaceVariantColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant, 0)
        val onPrimaryColor = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())
        val density = resources.displayMetrics.density

        // Update tab appearance
        listOf(tabFile, tabUrl, tabPaste).forEach { tabView ->
            tabView.setTextColor(onSurfaceVariantColor)
            tabView.background = null
        }

        val selectedTab = when (tab) {
            Tab.FILE -> tabFile
            Tab.URL -> tabUrl
            Tab.PASTE -> tabPaste
        }
        selectedTab.setTextColor(onPrimaryColor) // Theme-aware text color on accent
        selectedTab.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 6f * density
            setColor(accentColor)
        }

        // Show/hide input containers
        fileInputContainer.visibility = if (tab == Tab.FILE) View.VISIBLE else View.GONE
        urlInputContainer.visibility = if (tab == Tab.URL) View.VISIBLE else View.GONE
        pasteInputContainer.visibility = if (tab == Tab.PASTE) View.VISIBLE else View.GONE
    }

    private fun setupInputListeners() {
        btnSelectFile.setOnClickListener {
            onFileSelectRequested?.invoke()
        }

        btnFetchUrl.setOnClickListener {
            val url = inputUrl.text.toString().trim()
            if (url.isNotEmpty()) {
                fetchUrl(url)
            }
        }

        btnUsePaste.setOnClickListener {
            val text = inputPaste.text.toString().trim()
            if (text.isNotEmpty()) {
                processText(text, "Pasted Text", "clipboard")
            }
        }
    }

    private fun setupActionListeners() {
        rsvpSettingsLink.setOnClickListener {
            onRsvpSettingsClicked?.invoke()
        }
        btnBack.setOnClickListener {
            onBackPressed?.invoke()
        }

        btnUseDocument.setOnClickListener {
            processedDocument?.let { doc ->
                onDocumentSelected?.invoke(doc)
            }
        }
    }

    /**
     * Load settings from repository and apply to UI.
     */
    fun loadSettings(settings: SettingsRepository) {
        // Apply accent color
        accentColor = settings.colorScheme.accentColor
        applyAccentColor()

        // Re-select current tab to apply accent color
        selectTab(currentTab)
    }

    /**
     * Apply accent color to relevant UI elements.
     * Material components handle most theming automatically.
     */
    private fun applyAccentColor() {
        val accentTint = ColorStateList.valueOf(accentColor)

        // Use Document button - apply accent color
        btnUseDocument.backgroundTintList = accentTint
    }

    /**
     * Get current RsvpSettings from settings repository.
     */
    fun getRsvpSettings(settings: SettingsRepository, baseWpm: Int): RsvpSettings {
        return RsvpSettings(
            baseWpm = baseWpm,
            textSizePercent = settings.rsvpTextSizePercent,
            orpHighlightEnabled = settings.rsvpOrpHighlightEnabled,
            hyphenationEnabled = settings.rsvpHyphenationEnabled
        )
    }

    /**
     * Called when a file has been selected via SAF.
     */
    fun onFileSelected(uri: Uri, displayName: String, content: String) {
        txtSelectedFile.text = displayName
        processText(content, displayName, uri.toString())
    }

    /**
     * Fetch content from a URL.
     */
    private fun fetchUrl(urlString: String) {
        // Validate URL format
        val url = try {
            val cleanUrl = if (!urlString.startsWith("http://") && !urlString.startsWith("https://")) {
                "https://$urlString"
            } else {
                urlString
            }
            cleanUrl
        } catch (e: Exception) {
            inputUrl.error = "Invalid URL format"
            return
        }

        // Show loading state
        btnFetchUrl.isEnabled = false
        btnFetchUrl.text = "Fetching..."
        inputUrl.error = null

        coroutineScope.launch {
            try {
                val content = withContext(Dispatchers.IO) {
                    fetchUrlContent(url)
                }

                // Success - process the fetched content
                post {
                    btnFetchUrl.isEnabled = true
                    btnFetchUrl.text = "Fetch"

                    val displayName = try {
                        android.net.Uri.parse(url).host ?: url
                    } catch (e: Exception) {
                        url
                    }

                    processText(content, displayName, url)

                    Log.i(TAG, "URL fetched successfully: $url (${content.length} chars)")
                }
            } catch (e: Exception) {
                // Error handling
                post {
                    btnFetchUrl.isEnabled = true
                    btnFetchUrl.text = "Fetch"

                    val errorMsg = when {
                        e is java.net.UnknownHostException -> "Could not reach URL. Check internet connection."
                        e is java.net.SocketTimeoutException -> "Request timed out. Try again."
                        e.message?.contains("429") == true -> "Too many requests. Wait and try again."
                        e.message?.contains("403") == true -> "Access denied by website."
                        e.message?.contains("404") == true -> "URL not found."
                        else -> "Failed to fetch URL: ${e.message}"
                    }

                    inputUrl.error = errorMsg
                    Log.w(TAG, "URL fetch failed: $url", e)
                }
            }
        }
    }

    /**
     * Fetch and parse content from URL (runs on IO thread).
     */
    private fun fetchUrlContent(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Android) CogniHertz/1.0")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            val body = response.body?.string() ?: throw Exception("Empty response")

            // Check size limit (5MB max)
            if (body.length > 5_000_000) {
                throw Exception("Content too large (${body.length / 1_000_000}MB). Max 5MB.")
            }

            return when {
                contentType.contains("text/html") -> {
                    // Parse HTML and extract text
                    val doc = Jsoup.parse(body)

                    // Remove script and style elements
                    doc.select("script, style, nav, footer, header").remove()

                    // Extract text from body
                    val text = doc.body()?.text() ?: doc.text()

                    if (text.isBlank()) {
                        throw Exception("No readable text found in HTML")
                    }

                    text
                }
                contentType.contains("text/plain") -> {
                    body
                }
                else -> {
                    // Try parsing as HTML anyway
                    try {
                        val doc = Jsoup.parse(body)
                        doc.body()?.text() ?: body
                    } catch (e: Exception) {
                        body
                    }
                }
            }
        }
    }

    /**
     * Process raw text into a document preview.
     */
    private fun processText(text: String, displayName: String, sourceId: String) {
        rawText = text

        // Sanitize text (strip markdown, formatting, etc.)
        val sanitized = TextProcessor.sanitize(text)
        val words = TextProcessor.getWords(sanitized)
        val wordCount = words.size

        // Calculate stats
        val estimatedMinutes = ProcessedDocument.estimateMinutes(wordCount, 300)
        val preview = ProcessedDocument.createPreview(sanitized)

        // Update UI - show full sanitized text (what will actually be used in RSVP)
        previewCard.visibility = View.VISIBLE
        txtPreviewTitle.text = displayName
        txtPreviewStats.text = "$wordCount words ~ $estimatedMinutes min"
        txtPreviewText.text = sanitized

        // Enable use button
        btnUseDocument.isEnabled = true

        // Create processed document (glimpses will be generated when used)
        val settings = RsvpSettings(baseWpm = 300) // Default for preview
        val glimpses = TextProcessor.processToGlimpses(sanitized, settings)

        processedDocument = ProcessedDocument(
            sourceId = sourceId,
            displayName = displayName,
            glimpses = glimpses,
            totalWords = wordCount,
            estimatedMinutes = estimatedMinutes,
            preview = preview
        )

        Log.i(TAG, "Processed document: $displayName, $wordCount words, ${glimpses.size} glimpses")
    }

    /**
     * Reprocess document with updated settings.
     */
    fun reprocessWithSettings(settingsRepo: SettingsRepository, baseWpm: Int): ProcessedDocument? {
        val text = rawText ?: return null
        val doc = processedDocument ?: return null

        val settings = getRsvpSettings(settingsRepo, baseWpm)
        val sanitized = TextProcessor.sanitize(text)
        val glimpses = TextProcessor.processToGlimpses(sanitized, settings)

        processedDocument = doc.copy(glimpses = glimpses)
        return processedDocument
    }

    /**
     * Clear current document and reset UI.
     */
    fun clearDocument() {
        rawText = null
        processedDocument = null
        previewCard.visibility = View.GONE
        btnUseDocument.isEnabled = false
        txtSelectedFile.text = "No file selected"
        inputUrl.text?.clear()
        inputPaste.text?.clear()
    }

    /**
     * Handle shared content from other apps.
     * Detects URLs vs plain text and auto-populates the appropriate tab.
     */
    fun handleSharedContent(sharedText: String) {
        // Check if shared text looks like a URL
        val isUrl = sharedText.startsWith("http://") ||
                    sharedText.startsWith("https://") ||
                    (sharedText.contains(".") && !sharedText.contains("\n") && sharedText.length < 500)

        if (isUrl) {
            // Switch to URL tab and populate
            selectTab(Tab.URL)
            inputUrl.setText(sharedText.trim())

            // Auto-trigger fetch
            post {
                fetchUrl(sharedText.trim())
            }

            Log.i(TAG, "Shared URL detected, auto-fetching: $sharedText")
        } else {
            // Switch to Paste tab and populate
            selectTab(Tab.PASTE)
            inputPaste.setText(sharedText)

            // Auto-process the pasted text
            post {
                processText(sharedText, "Shared Text", "clipboard")
            }

            Log.i(TAG, "Shared text detected (${sharedText.length} chars)")
        }
    }
}
