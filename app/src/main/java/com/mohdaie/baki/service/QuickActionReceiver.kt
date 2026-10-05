package com.mohdaie.baki.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.mohdaie.baki.BakiApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Handles the "Save" / "Ignore" buttons on Baki's notification. */
class QuickActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id < 0) return
        val repo = (context.applicationContext as BakiApp).repository
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_SAVE -> repo.confirmPending(id)
                    ACTION_IGNORE -> repo.ignorePending(id)
                }
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        const val ACTION_SAVE = "com.mohdaie.baki.action.SAVE"
        const val ACTION_IGNORE = "com.mohdaie.baki.action.IGNORE"
        const val EXTRA_ID = "pending_id"
    }
}
