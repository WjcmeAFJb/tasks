package org.tasks.timers

/**
 * Interface for external time tracking integration.
 * Implementations send start/stop signals to external time tracking apps
 * (e.g., SimpleTimeTracker) when task timers are toggled.
 */
interface TimeTracker {
    suspend fun startTracking(activityName: String)
    suspend fun stopTracking(activityName: String)
}
