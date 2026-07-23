package com.bytethrux.loadr.data.sim

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Listens for system SIM state changes (insertion, removal, swaps).
 */
class SimChangeReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        // android.intent.action.SIM_STATE_CHANGED is a sticky broadcast on many devices
        // and is sent whenever any SIM slot status changes.
        if (intent.action == "android.intent.action.SIM_STATE_CHANGED") {
            val pendingResult = goAsync()
            scope.launch {
                try {
                    SimSyncManager(context).syncSlotsWithSubscriptions()
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
