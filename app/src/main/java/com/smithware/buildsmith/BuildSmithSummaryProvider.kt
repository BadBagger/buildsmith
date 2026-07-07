package com.smithware.buildsmith

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import kotlinx.coroutines.runBlocking

class BuildSmithSummaryProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (uri.authority != AUTHORITY || uri.lastPathSegment != PATH_SUMMARY) return null
        val appContext = context?.applicationContext ?: return MatrixCursor(COLUMNS)
        val dao = BuildSmithDatabase.get(appContext).dao()
        val summary = runBlocking {
            val active = dao.activeProjectCount()
            val open = dao.openChecklistCount()
            val prompts = dao.promptCount()
            val names = dao.latestProjectNames()
            val status = if (active == 0) "No active app projects" else "App build queue ready"
            val alert = if (open > 8) "$open launch checklist items still need review." else null
            val dueSoon = names.ifEmpty { listOf("No active projects") }.joinToString("|")
            Summary(status, "$active active projects, $prompts prompts saved", alert, "$active active|$open checklist open|$prompts prompts", dueSoon)
        }
        return MatrixCursor(COLUMNS).apply {
            addRow(arrayOf(APP_ID, summary.status, summary.keyInfo, summary.alert.orEmpty(), summary.counts, summary.dueSoon, "just now", "BuildSmith summary provider"))
        }
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private data class Summary(val status: String, val keyInfo: String, val alert: String?, val counts: String, val dueSoon: String)

    companion object {
        private const val AUTHORITY = "com.smithware.buildsmith.summary"
        private const val PATH_SUMMARY = "summary"
        private const val APP_ID = "buildsmith"
        private val COLUMNS = arrayOf("app_id", "status", "key_info", "alert", "counts", "due_soon", "last_updated", "source")
    }
}
