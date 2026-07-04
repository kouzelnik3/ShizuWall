package com.arslan.shizuwall.utils

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.util.Log

/**
 * Resolves the current foreground app package using UsageStatsManager.
 *
 * Queries UsageStatsManager.queryEvents over the last minute, scanning for
 * the most recent ACTIVITY_RESUMED event. This is the sole detection path
 *
 * Requires PACKAGE_USAGE_STATS permission, self-granted via the privileged
 * shell by ForegroundDetectionService on start.
 */
object ForegroundAppResolver {
    private const val TAG = "ForegroundAppResolver"
    private const val USAGE_EVENTS_LOOKBACK_MS = 60_000L

    fun hasUsageAccess(context: Context): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName
            ) == AppOpsManager.MODE_ALLOWED
        } catch (_: Throwable) {
            false
        }
    }

    /** Returns the current foreground package, or null if it cannot be determined. */
    fun getForegroundPackage(context: Context): String? {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null
            val end = System.currentTimeMillis()
            val events = usm.queryEvents(end - USAGE_EVENTS_LOOKBACK_MS, end)
            var lastPackage: String? = null
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                    lastPackage = event.packageName
                }
            }
            lastPackage
        } catch (t: Throwable) {
            Log.w(TAG, "UsageStats query failed", t)
            null
        }
    }
}
