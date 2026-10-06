package com.danyal.vaultgallery

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.danyal.vaultgallery.data.GalleryMedia

/**
 * Private, on-device metadata and FTS index. Source files are never modified. The ordinary
 * gallery can index names, folders, user tags and OCR text; Secure Gallery deliberately keeps
 * its metadata in the encrypted SQLCipher database instead of this public-library index.
 */
internal class GallerySearchIndex(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    "gallery-search.db",
    null,
    1,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE media_documents(
                rowid INTEGER PRIMARY KEY AUTOINCREMENT,
                media_key TEXT NOT NULL UNIQUE,
                display_name TEXT NOT NULL DEFAULT '',
                folder TEXT NOT NULL DEFAULT '',
                tags TEXT NOT NULL DEFAULT '',
                camera TEXT NOT NULL DEFAULT '',
                ocr_text TEXT NOT NULL DEFAULT '',
                transcript TEXT NOT NULL DEFAULT '',
                rating INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL DEFAULT 0
            )""".trimIndent(),
        )
        db.execSQL("CREATE VIRTUAL TABLE media_search USING fts4(display_name, folder, tags, camera, ocr_text, transcript, content='media_documents')")
        db.execSQL("""CREATE TRIGGER media_documents_ai AFTER INSERT ON media_documents BEGIN
            INSERT INTO media_search(docid, display_name, folder, tags, camera, ocr_text, transcript)
            VALUES(new.rowid, new.display_name, new.folder, new.tags, new.camera, new.ocr_text, new.transcript);
        END""")
        db.execSQL("""CREATE TRIGGER media_documents_ad AFTER DELETE ON media_documents BEGIN
            DELETE FROM media_search WHERE docid=old.rowid;
        END""")
        db.execSQL("""CREATE TRIGGER media_documents_au AFTER UPDATE ON media_documents BEGIN
            DELETE FROM media_search WHERE docid=old.rowid;
            INSERT INTO media_search(docid, display_name, folder, tags, camera, ocr_text, transcript)
            VALUES(new.rowid, new.display_name, new.folder, new.tags, new.camera, new.ocr_text, new.transcript);
        END""")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun indexMedia(media: List<GalleryMedia>) {
        val db = writableDatabase
        val activeKeys = media.mapTo(HashSet()) { publicKey(it.id) }
        db.beginTransaction()
        try {
            media.forEach { item ->
                val key = publicKey(item.id)
                db.execSQL("INSERT OR IGNORE INTO media_documents(media_key) VALUES(?)", arrayOf(key))
                db.update(
                    "media_documents",
                    ContentValues().apply {
                        put("display_name", item.name)
                        put("folder", item.bucketName)
                        put("updated_at", System.currentTimeMillis())
                    },
                    "media_key=?",
                    arrayOf(key),
                )
            }
            db.query("media_documents", arrayOf("media_key"), "media_key LIKE 'public:%'", null, null, null, null).use { cursor ->
                val stale = ArrayList<String>()
                while (cursor.moveToNext()) cursor.getString(0).takeIf { it !in activeKeys }?.let(stale::add)
                stale.forEach { db.delete("media_documents", "media_key=?", arrayOf(it)) }
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun updateOcr(mediaId: Long, text: String) = updateText(publicKey(mediaId), "ocr_text", text)
    fun updateTranscript(mediaId: Long, text: String) = updateText(publicKey(mediaId), "transcript", text)

    fun setTags(mediaId: Long, tags: String) = updateText(publicKey(mediaId), "tags", tags)

    fun tags(mediaId: Long): String = readableDatabase.query(
        "media_documents", arrayOf("tags"), "media_key=?", arrayOf(publicKey(mediaId)), null, null, null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0).orEmpty() else "" }

    fun setRating(mediaId: Long, rating: Int) {
        require(rating in 0..5)
        ensure(publicKey(mediaId))
        writableDatabase.update(
            "media_documents",
            ContentValues().apply { put("rating", rating); put("updated_at", System.currentTimeMillis()) },
            "media_key=?",
            arrayOf(publicKey(mediaId)),
        )
    }

    fun rating(mediaId: Long): Int = readableDatabase.query(
        "media_documents", arrayOf("rating"), "media_key=?", arrayOf(publicKey(mediaId)), null, null, null,
    ).use { cursor -> if (cursor.moveToFirst()) cursor.getInt(0) else 0 }

    fun searchPublicIds(query: String): Set<Long> {
        val match = query.trim().split(Regex("\\s+")).filter(String::isNotBlank)
            .joinToString(" ") { token -> "\"${token.replace("\"", "\"\"")}\"*" }
        if (match.isBlank()) return emptySet()
        return readableDatabase.rawQuery(
            "SELECT d.media_key FROM media_search JOIN media_documents d ON d.rowid=media_search.docid WHERE media_search MATCH ?",
            arrayOf(match),
        ).use { cursor ->
            buildSet {
                while (cursor.moveToNext()) cursor.getString(0).removePrefix("public:").toLongOrNull()?.let(::add)
            }
        }
    }

    private fun updateText(key: String, column: String, value: String) {
        require(column in setOf("ocr_text", "transcript", "tags"))
        ensure(key)
        writableDatabase.update(
            "media_documents",
            ContentValues().apply { put(column, value); put("updated_at", System.currentTimeMillis()) },
            "media_key=?",
            arrayOf(key),
        )
    }

    private fun ensure(key: String) {
        writableDatabase.execSQL("INSERT OR IGNORE INTO media_documents(media_key) VALUES(?)", arrayOf(key))
    }

    private fun publicKey(mediaId: Long) = "public:$mediaId"
}
