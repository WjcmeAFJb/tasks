package org.tasks

import android.content.Context
import com.todoroo.astrid.voice.VoiceOutputAssistant
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.tasks.data.dao.TaskDao
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Notification
import org.tasks.data.entity.Task
import org.tasks.notifications.AudioManager
import org.tasks.notifications.NotificationManager
import org.tasks.notifications.TelephonyManager
import org.tasks.preferences.Preferences

class NotifierMaxCovTest {

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

        val resources = mock<android.content.res.Resources>()
        whenever(context.resources).thenReturn(resources)
        whenever(context.applicationContext).thenReturn(context)
        whenever(context.packageName).thenReturn("org.tasks")

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

    // ================================================================
    // triggerNotifications(place, geofences, arrival)
    // ================================================================

    @Test
    fun arrivalFiltersForArrivalGeofences() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = true, isDeparture = false),
            Geofence(task = 2, isArrival = false, isDeparture = true),
        )
        whenever(taskDao.fetch(1L)).thenReturn(null)
        whenever(taskDao.fetch(2L)).thenReturn(null)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)

        verify(taskDao).fetch(1L)
        verify(taskDao, never()).fetch(2L)
    }

    @Test
    fun departureFiltersForDepartureGeofences() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = true, isDeparture = false),
            Geofence(task = 2, isArrival = false, isDeparture = true),
        )
        whenever(taskDao.fetch(2L)).thenReturn(null)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = false)

        verify(taskDao, never()).fetch(1L)
        verify(taskDao).fetch(2L)
    }

    @Test
    fun emptyGeofencesDoesNothing() = runTest {
        notifier.triggerNotifications(place = 10L, geofences = emptyList(), arrival = true)
        verify(notificationManager, never()).notifyTasks(any(), any(), any(), any())
    }

    @Test
    fun noMatchingGeofencesDoesNothing() = runTest {
        val geofences = listOf(
            Geofence(task = 1, isArrival = false, isDeparture = true),
        )
        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)
        verify(notificationManager, never()).notifyTasks(any(), any(), any(), any())
    }

    @Test
    fun arrivalCreatesGeoEnterNotification() = runTest {
        val geofences = listOf(Geofence(task = 1, isArrival = true, isDeparture = false))
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(any<Notification>())).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = true)

        verify(notificationManager).notifyTasks(any(), eq(true), eq(false), eq(false))
    }

    @Test
    fun departureCreatesGeoExitNotification() = runTest {
        val geofences = listOf(Geofence(task = 1, isArrival = false, isDeparture = true))
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(any<Notification>())).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(place = 10L, geofences = geofences, arrival = false)

        verify(notificationManager).notifyTasks(any(), eq(true), eq(false), eq(false))
    }

    // ================================================================
    // triggerNotifications(entries)
    // ================================================================

    @Test
    fun emptyEntriesDoesNothing() = runTest {
        notifier.triggerNotifications(emptyList())
        verify(notificationManager, never()).notifyTasks(any(), any(), any(), any())
    }

    @Test
    fun entriesWithNullTasksFilteredOut() = runTest {
        whenever(taskDao.fetch(1L)).thenReturn(null)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(n))
        verify(notificationManager, never()).notifyTasks(any(), any(), any(), any())
    }

    @Test
    fun entriesWithNullNotificationFilteredOut() = runTest {
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        whenever(notificationManager.getTaskNotification(any<Notification>())).thenReturn(null)

        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        notifier.triggerNotifications(listOf(n))

        verify(notificationManager, never()).notifyTasks(any(), any(), any(), any())
    }

    @Test
    fun validNotificationTriggersNotify() = runTest {
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verify(notificationManager).notifyTasks(any(), eq(true), eq(false), eq(false))
    }

    @Test
    fun nonstopTaskSetsNonstopFlag() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verify(notificationManager).notifyTasks(any(), eq(true), eq(true), eq(false))
    }

    @Test
    fun fiveTimesTaskSetsFiveTimesFlag() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_FIVE)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verify(notificationManager).notifyTasks(any(), eq(true), eq(false), eq(true))
    }

    @Test
    fun randomTypeDoesNotSetRingFlags() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_RANDOM)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        // TYPE_RANDOM should not propagate ring flags
        verify(notificationManager).notifyTasks(any(), eq(true), eq(false), eq(false))
    }

    // ================================================================
    // Voice output conditions
    // ================================================================

    @Test
    fun voiceDisabledByPreference() = runTest {
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verifyNoInteractions(voiceOutputAssistant)
    }

    @Test
    fun voiceSilencedByNonstop() = runTest {
        val task = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        whenever(audioManager.notificationsMuted()).thenReturn(false)
        whenever(telephonyManager.callStateIdle()).thenReturn(true)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(any())
    }

    @Test
    fun voiceSilencedByMutedAudio() = runTest {
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        whenever(audioManager.notificationsMuted()).thenReturn(true)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(any())
    }

    @Test
    fun voiceSilencedByPhoneCall() = runTest {
        val task = Task(id = 1)
        whenever(taskDao.fetch(1L)).thenReturn(task)
        val n = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(true)
        whenever(audioManager.notificationsMuted()).thenReturn(false)
        whenever(telephonyManager.callStateIdle()).thenReturn(false)

        notifier.triggerNotifications(listOf(n))

        verify(voiceOutputAssistant, never()).speak(any())
    }

    @Test
    fun multipleNotificationsWithMixedFlags() = runTest {
        val nonstopTask = Task(id = 1, ringFlags = Task.NOTIFY_MODE_NONSTOP)
        val fiveTask = Task(id = 2, ringFlags = Task.NOTIFY_MODE_FIVE)
        whenever(taskDao.fetch(1L)).thenReturn(nonstopTask)
        whenever(taskDao.fetch(2L)).thenReturn(fiveTask)

        val n1 = Notification(taskId = 1, timestamp = 100L, type = Alarm.TYPE_SNOOZE)
        val n2 = Notification(taskId = 2, timestamp = 200L, type = Alarm.TYPE_SNOOZE)
        val builder = mock<androidx.core.app.NotificationCompat.Builder>()
        whenever(notificationManager.getTaskNotification(n1)).thenReturn(builder)
        whenever(notificationManager.getTaskNotification(n2)).thenReturn(builder)
        whenever(preferences.getBoolean(eq(R.string.p_voiceRemindersEnabled), eq(false))).thenReturn(false)

        notifier.triggerNotifications(listOf(n1, n2))

        verify(notificationManager).notifyTasks(any(), eq(true), eq(true), eq(true))
    }
}
