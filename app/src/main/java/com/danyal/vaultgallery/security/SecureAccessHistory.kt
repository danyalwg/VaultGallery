package com.danyal.vaultgallery.security

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

internal data class SecureAccessEvent(
    val packageName: String,
    val itemId: String?,
    val allowed: Boolean,
    val timestampMs: Long,
)

/** Bounded local audit trail. It intentionally stores no filename, thumbnail, OCR text or path. */
internal class SecureAccessHistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("secure-access-history", Context.MODE_PRIVATE)

    @Synchronized fun record(packageName: String, itemId: String?, allowed: Boolean) {
        val current = list().toMutableList()
        current.add(0, SecureAccessEvent(packageName, itemId, allowed, System.currentTimeMillis()))
        val json = JSONArray()
        current.take(MAX_EVENTS).forEach { event ->
            json.put(JSONObject().apply {
                put("package", event.packageName)
                event.itemId?.let { put("item", it) }
                put("allowed", event.allowed)
                put("time", event.timestampMs)
            })
        }
        prefs.edit().putString("events", json.toString()).apply()
    }

    fun list(): List<SecureAccessEvent> = runCatching {
        val json = JSONArray(prefs.getString("events", "[]"))
        buildList {
            repeat(json.length()) { index ->
                val event = json.getJSONObject(index)
                add(SecureAccessEvent(
                    packageName = event.optString("package", "Unknown app"),
                    itemId = event.optString("item").takeIf(String::isNotBlank),
                    allowed = event.optBoolean("allowed"),
                    timestampMs = event.optLong("time"),
                ))
            }
        }
    }.getOrDefault(emptyList())

    fun clear() = prefs.edit().remove("events").apply()

    private companion object { const val MAX_EVENTS = 100 }
}
