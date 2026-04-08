/*
 * Copyright (c) 2012 Todoroo Inc
 *
 * See the file "LICENSE" for the full license governing this code.
 */
package com.todoroo.astrid.timers

import org.tasks.data.dao.TagDataDao
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.notifications.Notifier
import org.tasks.time.DateTimeUtils2.currentTimeMillis
import org.tasks.timers.TimeTracker

class TimerPlugin(
    private val notifier: Notifier,
    private val taskDao: TaskDao,
    private val tagDataDao: TagDataDao,
    private val timeTracker: TimeTracker,
) {
    suspend fun startTimer(task: Task) {
        updateTimer(task, true)
    }

    suspend fun stopTimer(task: Task) {
        updateTimer(task, false)
    }

    private suspend fun updateTimer(task: Task, start: Boolean) {
        if (start) {
            if (task.timerStart == 0L) {
                task.timerStart = currentTimeMillis()
            }
        } else {
            if (task.timerStart > 0) {
                val newElapsed = ((currentTimeMillis() - task.timerStart) / 1000L).toInt()
                task.timerStart = 0L
                task.elapsedSeconds += newElapsed
            }
        }
        taskDao.update(task)
        notifier.updateTimerNotification()
        notifyTimeTracker(task, start)
    }

    private suspend fun notifyTimeTracker(task: Task, start: Boolean) {
        // Check transient (unsaved) tags first, then fall back to database
        val activityName = task.tags.firstOrNull()
            ?: tagDataDao.getTagDataForTask(task.id).firstOrNull()?.name
            ?: return
        if (start) {
            timeTracker.startTracking(activityName)
        } else {
            timeTracker.stopTracking(activityName)
        }
    }
}
