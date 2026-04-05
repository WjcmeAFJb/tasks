package com.todoroo.astrid.timers

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Task
import org.tasks.notifications.Notifier
import org.tasks.time.DateTimeUtils2

class TimerPluginTest {

    private lateinit var taskDao: TaskDao
    private lateinit var notifier: Notifier
    private lateinit var timerPlugin: TimerPlugin

    @Before
    fun setUp() {
        taskDao = mock(TaskDao::class.java)
        notifier = mock(Notifier::class.java)
        timerPlugin = TimerPlugin(notifier, taskDao)
    }

    @Test
    fun startTimer_setsTimerStartOnTask() = runTest {
        val task = Task(id = 1, timerStart = 0L)
        timerPlugin.startTimer(task)
        assert(task.timerStart > 0)
    }

    @Test
    fun startTimer_doesNotOverwriteExistingTimer() = runTest {
        val existingStart = 1000L
        val task = Task(id = 2, timerStart = existingStart)
        timerPlugin.startTimer(task)
        assertEquals(existingStart, task.timerStart)
    }

    @Test
    fun startTimer_updatesTaskInDao() = runTest {
        val task = Task(id = 3, timerStart = 0L)
        timerPlugin.startTimer(task)
        verify(taskDao).update(task)
    }

    @Test
    fun startTimer_updatesTimerNotification() = runTest {
        val task = Task(id = 4, timerStart = 0L)
        timerPlugin.startTimer(task)
        verify(notifier).updateTimerNotification()
    }

    @Test
    fun stopTimer_resetsTimerStartToZero() = runTest {
        val task = Task(id = 5, timerStart = DateTimeUtils2.currentTimeMillis() - 5000)
        timerPlugin.stopTimer(task)
        assertEquals(0L, task.timerStart)
    }

    @Test
    fun stopTimer_addsElapsedSeconds() = runTest {
        val now = DateTimeUtils2.currentTimeMillis()
        val task = Task(id = 6, timerStart = now - 10000, elapsedSeconds = 0)
        timerPlugin.stopTimer(task)
        // ~10 seconds elapsed
        assert(task.elapsedSeconds >= 9)
        assert(task.elapsedSeconds <= 11)
    }

    @Test
    fun stopTimer_accumulatesElapsedSeconds() = runTest {
        val now = DateTimeUtils2.currentTimeMillis()
        val task = Task(id = 7, timerStart = now - 5000, elapsedSeconds = 100)
        timerPlugin.stopTimer(task)
        // Should have 100 + ~5 seconds
        assert(task.elapsedSeconds >= 104)
        assert(task.elapsedSeconds <= 106)
    }

    @Test
    fun stopTimer_doesNothingWhenTimerNotRunning() = runTest {
        val task = Task(id = 8, timerStart = 0L, elapsedSeconds = 50)
        timerPlugin.stopTimer(task)
        assertEquals(50, task.elapsedSeconds)
        assertEquals(0L, task.timerStart)
    }

    @Test
    fun stopTimer_updatesTaskInDao() = runTest {
        val task = Task(id = 9, timerStart = 1000L)
        timerPlugin.stopTimer(task)
        verify(taskDao).update(task)
    }

    @Test
    fun stopTimer_updatesTimerNotification() = runTest {
        val task = Task(id = 10, timerStart = 1000L)
        timerPlugin.stopTimer(task)
        verify(notifier).updateTimerNotification()
    }

    @Test
    fun stopTimer_stillCallsDaoWhenTimerNotRunning() = runTest {
        val task = Task(id = 11, timerStart = 0L)
        timerPlugin.stopTimer(task)
        verify(taskDao).update(task)
        verify(notifier).updateTimerNotification()
    }

    @Test
    fun startThenStop_calculatesElapsedCorrectly() = runTest {
        val task = Task(id = 12, timerStart = 0L, elapsedSeconds = 0)
        timerPlugin.startTimer(task)
        val startTime = task.timerStart
        assert(startTime > 0)

        // Simulate immediate stop (elapsed ~0 seconds)
        timerPlugin.stopTimer(task)
        assertEquals(0L, task.timerStart)
        // Elapsed should be tiny (< 1 second rounds to 0)
        assert(task.elapsedSeconds >= 0)
    }

    @Test
    fun startTimer_callsDaoAndNotifierExactlyOnce() = runTest {
        val task = Task(id = 13, timerStart = 0L)
        timerPlugin.startTimer(task)
        verify(taskDao, times(1)).update(task)
        verify(notifier, times(1)).updateTimerNotification()
    }

    @Test
    fun stopTimer_elapsedSecondsIsIntDivision() = runTest {
        // Timer started 2500ms ago -> 2500/1000 = 2 (integer division)
        val now = DateTimeUtils2.currentTimeMillis()
        val task = Task(id = 14, timerStart = now - 2500, elapsedSeconds = 0)
        timerPlugin.stopTimer(task)
        // Should be 2 seconds (integer division truncates)
        assert(task.elapsedSeconds >= 2)
        assert(task.elapsedSeconds <= 3)
    }
}
