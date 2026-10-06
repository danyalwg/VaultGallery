package com.danyal.vaultgallery.search

import kotlin.math.sqrt

data class VectorHit(val id: String, val score: Float)
interface VectorIndex {
    suspend fun upsert(id: String, vector: FloatArray)
    suspend fun remove(id: String)
    suspend fun contains(id: String): Boolean
    suspend fun nearest(vector: FloatArray, limit: Int): List<VectorHit>
}

class InMemoryVectorIndex : VectorIndex {
    private val vectors = linkedMapOf<String, FloatArray>()
    override suspend fun upsert(id: String, vector: FloatArray) { vectors[id] = vector.copyOf() }
    override suspend fun remove(id: String) { vectors.remove(id) }
    override suspend fun contains(id: String): Boolean = id in vectors
    override suspend fun nearest(vector: FloatArray, limit: Int): List<VectorHit> = vectors.mapNotNull { (id, candidate) ->
        if (candidate.size != vector.size) null else VectorHit(id, cosine(vector, candidate))
    }.sortedByDescending { it.score }.take(limit)

    private fun cosine(a: FloatArray, b: FloatArray): Float {
        var dot = 0.0; var aa = 0.0; var bb = 0.0
        for (i in a.indices) { dot += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i] }
        return if (aa == 0.0 || bb == 0.0) 0f else (dot / (sqrt(aa) * sqrt(bb))).toFloat()
    }
}
