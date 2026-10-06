package com.danyal.vaultgallery.search

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

/** Small replaceable local vector store. Search semantics do not depend on this implementation. */
class SqliteVectorIndex(context: Context, private val namespace: String) : VectorIndex, AutoCloseable {
    private val helper = Helper(context.applicationContext)

    override suspend fun upsert(id: String, vector: FloatArray) = withContext(Dispatchers.IO) {
        require(vector.isNotEmpty()) { "Vector cannot be empty" }
        val bytes = ByteBuffer.allocate(vector.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        vector.forEach(bytes::putFloat)
        helper.writableDatabase.execSQL(
            "INSERT OR REPLACE INTO vectors(namespace,id,dimension,value) VALUES(?,?,?,?)",
            arrayOf(namespace, id, vector.size, bytes.array()),
        )
    }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        helper.writableDatabase.delete("vectors", "namespace=? AND id=?", arrayOf(namespace, id))
        Unit
    }

    override suspend fun contains(id: String): Boolean = withContext(Dispatchers.IO) {
        helper.readableDatabase.rawQuery(
            "SELECT 1 FROM vectors WHERE namespace=? AND id=? LIMIT 1",
            arrayOf(namespace, id),
        ).use { it.moveToFirst() }
    }

    override suspend fun nearest(vector: FloatArray, limit: Int): List<VectorHit> = withContext(Dispatchers.IO) {
        val result = ArrayList<VectorHit>()
        helper.readableDatabase.rawQuery(
            "SELECT id,dimension,value FROM vectors WHERE namespace=?",
            arrayOf(namespace),
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val dimension = cursor.getInt(1)
                if (dimension != vector.size) continue
                val buffer = ByteBuffer.wrap(cursor.getBlob(2)).order(ByteOrder.LITTLE_ENDIAN)
                var dot = 0.0; var aa = 0.0; var bb = 0.0
                for (index in vector.indices) {
                    val candidate = buffer.float
                    dot += vector[index] * candidate
                    aa += vector[index] * vector[index]
                    bb += candidate * candidate
                }
                val score = if (aa == 0.0 || bb == 0.0) 0f else (dot / (sqrt(aa) * sqrt(bb))).toFloat()
                result += VectorHit(cursor.getString(0), score)
            }
        }
        result.sortedByDescending(VectorHit::score).take(limit.coerceAtLeast(0))
    }

    override fun close() = helper.close()

    private class Helper(context: Context) : SQLiteOpenHelper(context, "vector-index.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE vectors(namespace TEXT NOT NULL,id TEXT NOT NULL,dimension INTEGER NOT NULL,value BLOB NOT NULL,PRIMARY KEY(namespace,id))")
            db.execSQL("CREATE INDEX vectors_namespace ON vectors(namespace)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
}
