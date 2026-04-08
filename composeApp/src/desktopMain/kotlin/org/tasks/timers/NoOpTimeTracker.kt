package org.tasks.timers

class NoOpTimeTracker : TimeTracker {
    override suspend fun startTracking(activityName: String) {}
    override suspend fun stopTracking(activityName: String) {}
}
