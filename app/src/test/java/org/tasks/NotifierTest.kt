package org.tasks

import android.content.Context
import com.todoroo.astrid.voice.VoiceOutputAssistant
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.AudioManager
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences

class NotifierTest {

    private lateinit var context: Context
    private lateinit var taskDao: TaskDao
    private lateinit var notificationManager: NotificationManager
    private lateinit var telephonyManager: TelephonyManager
    private lateinit var audioManager: AudioManager
    private lateinit var voiceOutputAssistant: VoiceOutputAssistant
    private lateinit var preferences: Preferences
    private lateinit var notifier: Notifier

    @Before
    fun setUp() {
        context = mock()
        taskDao = mock()
        notificationManager = mock()
        telephonyManager = mock()
        audioManager = mock()
        voiceOutputAssistant = mock()
        preferences = mock()

        // Context needs to return a Resources mock for ColorProvider
        val resources = mock(android.content.res.Resources::class.java)
        `when`(context.resources).thenReturn(resources)
        `when`(context.applicationContext).thenReturn(context)
        `when`(context.packageName).thenReturn("org.tasks")

        notifier = Notifier(
            context = context,
            taskDao = taskDao,
            notificationManager = notificationManager,
            telephonyManager = telephonyManager,
            audioManager = audioManager,
            voiceOutputAssistant = voiceOutputAssistant,
            preferences = preferences,
        )
    }

    // --- triggerNotifications with empty list ---

    @Test
    fun triggerNotificationsWithEmptyListDoesNothing() = runTest {
        notifier.triggerNotifications(emptyList())

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
        verifyNoInteractions(voiceOutputAssistant)
    }

    // --- triggerNotifications filters deleted tasks ---

    @Test
    fun triggerNotificationsFiltersDeletedTasks() = runTest {
        val deletedTask = Task(id = 1, deletionDate = 1000L)
        `when`(taskDao.fetch(1L)).thenReturn(deletedTask)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(null)

        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(notification))

        // Deleted task returns null from getTaskNotification, so the notification is filtered out
        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    // --- triggerNotifications filters completed tasks ---

    @Test
    fun triggerNotificationsFiltersCompletedTasks() = runTest {
        val completedTask = Task(id = 2, completionDate = 2000L)
        `when`(taskDao.fetch(2L)).thenReturn(completedTask)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(null)

        val notification = Notification(taskId = 2, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(notification))

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    // --- triggerNotifications filters null (non-existent) tasks ---

    @Test
    fun triggerNotificationsFiltersNullTasks() = runTest {
        `when`(taskDao.fetch(999L)).thenReturn(null)

        val notification = Notification(taskId = 999, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(notification))

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    // --- voice output: no voice when disabled ---

    @Test
    fun noVoiceOutputWhenPreferenceDisabled() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        verifyNoInteractions(voiceOutputAssistant)
    }

    // --- voice output: no voice when audio muted ---

    @Test
    fun noVoiceOutputWhenAudioMuted() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(true)
        `when`(telephonyManager.callStateIdle()).thenReturn(true)

        notifier.triggerNotifications(listOf(notification))

        verify(voiceOutputAssistant, never()).speak(org.mockito.ArgumentMatchers.anyString())
    }

    // --- voice output: no voice when call active ---

    @Test
    fun noVoiceOutputWhenCallActive() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(false)
        `when`(telephonyManager.callStateIdle()).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        verify(voiceOutputAssistant, never()).speak(org.mockito.ArgumentMatchers.anyString())
    }

    // --- notifies tasks when notification is valid ---

    @Test
    fun notifiesTasksWhenNotificationValid() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    // --- nonstop mode from task ---

    @Test
    fun nonstopModeSetFromTask() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(true), eq(false))
    }

    // --- five times mode from task ---

    @Test
    fun fiveTimesModeSetFromTask() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_FIVE)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(true))
    }

    // --- no voice output when nonstop mode ---

    @Test
    fun noVoiceOutputWhenNonstopMode() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(false)
        `when`(telephonyManager.callStateIdle()).thenReturn(true)

        notifier.triggerNotifications(listOf(notification))

        verify(voiceOutputAssistant, never()).speak(org.mockito.ArgumentMatchers.anyString())
    }

    // --- random type does not set ring flags ---

    @Test
    fun randomTypeDoesNotSetRingFlags() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_FIVE)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val notification = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_RANDOM)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification))

        // TYPE_RANDOM should not set ringFiveTimes even though the task has NOTIFY_MODE_FIVE
        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    // --- multiple notifications, some invalid ---

    @Test
    fun multipleNotificationsMixedValidity() = runTest {
        val validTask = Task(id = 1)
        val deletedTask = Task(id = 2, deletionDate = 1000L)
        `when`(taskDao.fetch(1L)).thenReturn(validTask)
        `when`(taskDao.fetch(2L)).thenReturn(deletedTask)

        val notification1 = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val notification2 = Notification(taskId = 2, timestamp = 200L, type = Alarm.TYPE_SNOOZE)

        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        // Only return builder for valid task's notification, null for deleted
        `when`(notificationManager.getTaskNotification(notification1)).thenReturn(builder)
        `when`(notificationManager.getTaskNotification(notification2)).thenReturn(null)

        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(notification1, notification2))

        // Should still notify - the valid notification passes through
        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }
}
