package com.pocketdev.advance_erp.core.util

import android.content.Context
import org.json.JSONObject

/** Unposted editor buffers. Never update a purchase, shipment, journal or stock balance. */
class EditorDraftStore(context: Context) {
    private val preferences = context.getSharedPreferences("editor_drafts_v1", Context.MODE_PRIVATE)
    fun read(key: String): JSONObject? = preferences.getString(key, null)?.let {
        try { JSONObject(it) } catch (_: org.json.JSONException) { null }
    }
    fun save(key: String, snapshot: JSONObject) {
        check(preferences.edit().putString(key, snapshot.toString()).commit()) { "Unable to save draft" }
    }
    fun remove(key: String) { preferences.edit().remove(key).apply() }
}
