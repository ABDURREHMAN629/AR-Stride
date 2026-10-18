package com.stride.fit.sensors

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Brings the pedometer back after a reboot or an app update.
 *
 * The hardware step counter restarts from zero on boot, so the service also clears
 * its stored baseline the first time it sees a lower reading than it remembers.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                context.getSharedPreferences("stride_step_service", Context.MODE_PRIVATE)
                    .edit()
                    .remove("last_raw_counter")
                    .apply()
                runCatching { StepService.start(context) }
            }
        }
    }
}
