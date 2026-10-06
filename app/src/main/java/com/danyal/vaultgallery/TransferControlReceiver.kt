package com.danyal.vaultgallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.util.UUID

internal object TransferControl {
    const val ACTION_PAUSE = "com.danyal.vaultgallery.transfer.PAUSE"
    const val ACTION_RESUME = "com.danyal.vaultgallery.transfer.RESUME"
    const val ACTION_CANCEL = "com.danyal.vaultgallery.transfer.CANCEL"
    const val EXTRA_WORK_ID = "work_id"
    private const val PREFS = "media-transfer-control"

    fun setPaused(context: Context, id: UUID, paused: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(id.toString(), paused).apply()
    }

    fun isPaused(context: Context, id: UUID): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(id.toString(), false)

    fun setCancelled(context: Context, id: UUID) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("cancel-$id", true).apply()
    }

    fun isCancelled(context: Context, id: UUID): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("cancel-$id", false)

    fun clear(context: Context, id: UUID) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(id.toString()).remove("cancel-$id").apply()
    }
}

class TransferControlReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(TransferControl.EXTRA_WORK_ID)?.let { value ->
            runCatching { UUID.fromString(value) }.getOrNull()
        } ?: return
        when (intent.action) {
            TransferControl.ACTION_PAUSE -> TransferControl.setPaused(context, id, true)
            TransferControl.ACTION_RESUME -> TransferControl.setPaused(context, id, false)
            TransferControl.ACTION_CANCEL -> TransferControl.setCancelled(context, id)
        }
    }
}
