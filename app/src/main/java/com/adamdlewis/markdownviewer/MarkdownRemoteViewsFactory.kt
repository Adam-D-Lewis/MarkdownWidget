package com.adamdlewis.markdownviewer

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import android.text.Html
import java.io.BufferedReader
import java.io.InputStreamReader
import android.net.Uri
import com.adamdlewis.markdownviewer.R

class MarkdownRemoteViewsFactory(
    private val context: Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var pageSize = 5000 // Characters per page
    private var currentPage = 0
    private var totalPages = 0
    private var pages = mutableListOf<String>()

    override fun onCreate() {
        loadPages()
    }

    override fun onDataSetChanged() {
        loadPages()
    }

    private fun loadPages() {
        val prefs = context.getSharedPreferences(MarkdownWidgetConfigureActivity.PREFS_NAME, 0)
        val contentUriString = prefs.getString(MarkdownWidgetConfigureActivity.PREF_URI_KEY + appWidgetId, null)

        if (contentUriString != null) {
            val contentUri = Uri.parse(contentUriString)
            val inputStream = context.contentResolver.openInputStream(contentUri)
            val reader = BufferedReader(InputStreamReader(inputStream))

            pages.clear()
            var content = StringBuilder()
            var currentPageContent = StringBuilder()

            // Read file line by line
            reader.useLines { lines ->
                lines.forEach { line ->
                    currentPageContent.append(line).append("\n")

                    // When we reach pageSize, create a new page
                    if (currentPageContent.length >= pageSize) {
                        // Find the last paragraph break before pageSize
                        val lastBreak = findLastParagraphBreak(currentPageContent.toString())

                        if (lastBreak > 0) {
                            // Add content up to the last break to the current page
                            pages.add(currentPageContent.substring(0, lastBreak))
                            // Keep the remaining content for the next page
                            currentPageContent = StringBuilder(currentPageContent.substring(lastBreak))
                        } else {
                            // If no break found, just split at pageSize
                            pages.add(currentPageContent.toString())
                            currentPageContent = StringBuilder()
                        }
                    }
                }
            }

            // Add any remaining content as the last page
            if (currentPageContent.isNotEmpty()) {
                pages.add(currentPageContent.toString())
            }

            totalPages = pages.size
        } else {
            pages.clear()
            pages.add("Error: File not found")
        }
    }

    private fun findLastParagraphBreak(text: String): Int {
        // Look for the last occurrence of two newlines (paragraph break)
        val paragraphBreak = text.lastIndexOf("\n\n")
        if (paragraphBreak > 0) return paragraphBreak

        // If no paragraph break, look for the last single newline
        val lineBreak = text.lastIndexOf("\n")
        if (lineBreak > 0) return lineBreak

        // If no breaks found, split at a space to avoid breaking words
        val lastSpace = text.lastIndexOf(" ", pageSize)
        return if (lastSpace > 0) lastSpace else -1
    }

    override fun getCount(): Int = totalPages

    override fun getViewAt(position: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_item)

        if (position < pages.size) {
            // Parse and render markdown for this page
            val parser = Parser.builder().build()
            val document = parser.parse(pages[position])
            val renderer = HtmlRenderer.builder().build()
            val html = renderer.render(document)
            val spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)

            views.setTextViewText(R.id.widget_item_text, spanned)

            // Set up fill-in intent for click handling
            val fillInIntent = Intent()
            fillInIntent.putExtra("page", position)
            views.setOnClickFillInIntent(R.id.widget_item_text, fillInIntent)
        }

        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {}
}