package org.tasks

import android.content.Context
import com.todoroo.astrid.voice.VoiceOutputAssistant
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.AudioManager
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences

class NotifierDeepTest {

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

    // ===== triggerNotifications(place, geofences, arrival) =====

    @Test
    fun triggerNotificationsArrivalFiltersForArrivalGeofences() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = true, isDeparture = false),
            Geofence(task = 2, isArrival = false, isDeparture = true),
        )
        // Neither task is in the DB, so both get filtered
        `when`(taskDao.fetch(1L)).thenReturn(null)
        `when`(taskDao.fetch(2L)).thenReturn(null)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)

        // Only task 1 should generate a notification (arrival geofence)
        verify(taskDao).fetch(1L)
        verify(taskDao, never()).fetch(2L)
    }

    @Test
    fun triggerNotificationsDepartureFiltersForDepartureGeofences() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = true, isDeparture = false),
            Geofence(task = 2, isArrival = false, isDeparture = true),
        )
        `when`(taskDao.fetch(1L)).thenReturn(null)
        `when`(taskDao.fetch(2L)).thenReturn(null)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = false)

        // Only task 2 should generate a notification (departure geofence)
        verify(taskDao, never()).fetch(1L)
        verify(taskDao).fetch(2L)
    }

    @Test
    fun triggerNotificationsArrivalSetsGeoEnterType() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = true, isDeparture = false),
        )
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    @Test
    fun triggerNotificationsDepartureSetsGeoExitType() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = false, isDeparture = true),
        )
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = false)

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    @Test
    fun triggerNotificationsEmptyGeofencesDoesNothing() = runTest {
        notifier.triggerNotifications(place = 10L, geofences = emptyList(), arrival = true)

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun triggerNotificationsNoMatchingGeofences() = runTest {
        // All departure geofences when looking for arrival
        val geofences = listOf(
            Geofence(task = 1, isArrival = false, isDeparture = true),
            Geofence(task = 2, isArrival = false, isDeparture = true),
        )

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    // ===== triggerNotifications(entries) deep tests =====

    @Test
    fun multipleNotificationsAllValid() = runTest {
        val task1 = Task(id = 1)
        val task2 = Task(id = 2)
        `when`(taskDao.fetch(1L)).thenReturn(task1)
        `when`(taskDao.fetch(2L)).thenReturn(task2)

        val n1 = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n1)).thenReturn(builder)
        `when`(notificationManager.getTaskNotification(n2)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n1, n2))

        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    @Test
    fun multipleNotificationsAllInvalid() = runTest {
        `when`(taskDao.fetch(1L)).thenReturn(null)
        `when`(taskDao.fetch(2L)).thenReturn(null)

        val n1 = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = Alarm.TYPE_SNOOZE)

        notifier.triggerNotifications(listOf(n1, n2))

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    @Test
    fun nonstopAndFiveTimesFromDifferentTasks() = runTest {
        val nonstopTask = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        val fiveTask = Task(id = 2, ringFlags = Task.NOTIFY_MODE_FIVE)
        `when`(taskDao.fetch(1L)).thenReturn(nonstopTask)
        `when`(taskDao.fetch(2L)).thenReturn(fiveTask)

        val n1 = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n1)).thenReturn(builder)
        `when`(notificationManager.getTaskNotification(n2)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n1, n2))

        // Both nonstop and fiveTimes should be true since different tasks set each
        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(true), eq(true))
    }

    @Test
    fun randomTypeDoesNotSetNonstop() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        `when`(taskDao.fetch(1L)).thenReturn(task)

        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_RANDOM)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        // TYPE_RANDOM should not propagate ring flags
        verify(notificationManager).notifyTasks(anyList(), eq(true), eq(false), eq(false))
    }

    @Test
    fun taskWithGetTaskNotificationReturningNullIsFiltered() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        `when`(notificationManager.getTaskNotification(
            org.mockito.ArgumentMatchers.any(Notification::class.java) ?: Notification()
        )).thenReturn(null)

        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(n))

        verify(notificationManager, never()).notifyTasks(
            anyList(), anyBoolean(), anyBoolean(), anyBoolean()
        )
    }

    // ===== Voice output =====

    @Test
    fun voiceOutputEnabledButNonstopSilences() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(false)
        `when`(telephonyManager.callStateIdle()).thenReturn(true)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(anyString())
    }

    @Test
    fun voiceDisabledByPreference() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verifyNoInteractions(voiceOutputAssistant)
    }

    @Test
    fun voiceDisabledByMutedAudio() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(true)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(anyString())
    }

    @Test
    fun voiceDisabledByPhoneCall() = runTest {
        val task = Task(id = 1)
        `when`(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock(androidx.core.app.NotificationCompat.Builder::class.java)
        `when`(notificationManager.getTaskNotification(n)).thenReturn(builder)
        `when`(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        `when`(audioManager.notificationsMuted()).thenReturn(false)
        `when`(telephonyManager.callStateIdle()).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(anyString())
    }
}
