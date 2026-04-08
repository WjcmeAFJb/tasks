package org.tasks.timers

import android.content.Context
import android.content.Intent

class SimpleTimeTrackerIntegration(
    private val context: Context,
) : TimeTracker {

    override suspend fun startTracking(activityName: String) {
        sendBroadcast(ACTION_START_ACTIVITY, activityName)
    }

    override suspend fun stopTracking(activityName: String) {
        sendBroadcast(ACTION_STOP_ACTIVITY, activityName)
    }

    private fun sendBroadcast(action: String, activityName: String) {
        val intent = Intent(action).apply {
            setPackage(PACKAGE)
            putExtra(EXTRA_ACTIVITY_NAME, activityName)
        }
        context.sendBroadcast(intent)
    }

    companion object {
        private const val PACKAGE = "com.razeeman.util.simpletimetracker"
        const val ACTION_START_ACTIVITY = "$PACKAGE.ACTION_START_ACTIVITY"
        const val ACTION_STOP_ACTIVITY = "$PACKAGE.ACTION_STOP_ACTIVITY"
        const val EXTRA_ACTIVITY_NAME = "extra_activity_name"
    }
}
