package com.danyal.vaultgallery.creation

import java.util.UUID

data class SceneTransform(val x: Float = 0f, val y: Float = 0f, val scale: Float = 1f, val rotation: Float = 0f)
data class SceneNode(
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val source: String? = null,
    val transform: SceneTransform = SceneTransform(),
    val opacity: Float = 1f,
    val cornerRadius: Float = 0f,
    val zIndex: Int = 0,
)
data class CollageProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val aspectWidth: Int,
    val aspectHeight: Int,
    val background: String,
    val nodes: List<SceneNode>,
)
