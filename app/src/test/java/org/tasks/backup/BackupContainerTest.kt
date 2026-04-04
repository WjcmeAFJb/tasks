package org.tasks.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.GoogleTask
import org.tasks.data.entity.Alarm
import org.tasks.data.entity.Attachment
import org.tasks.data.entity.CaldavTask
import org.tasks.data.entity.Geofence
import org.tasks.data.entity.Tag
import org.tasks.data.entity.Task
import org.tasks.data.entity.UserActivity

class BackupContainerTest {

    @Test
    fun defaultAlarmsIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.alarms)
    }

    @Test
    fun defaultGeofencesIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.geofences)
    }

    @Test
    fun defaultTagsIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.tags)
    }

    @Test
    fun defaultCommentsIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.comments)
    }

    @Test
    fun defaultAttachmentsIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.attachments)
    }

    @Test
    fun defaultCaldavTasksIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.caldavTasks)
    }

    @Test
    fun defaultVtodoIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.vtodo)
    }

    @Test
    fun defaultGoogleIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.google)
    }

    @Test
    fun defaultLocationsIsNull() {
        val backup = TaskBackup(task = Task())
        assertNull(backup.locations)
    }

    @Test
    fun taskIsStored() {
        val task = Task(title = "My Task", notes = "Some notes")
        val backup = TaskBackup(task = task)
        assertEquals("My Task", backup.task.title)
        assertEquals("Some notes", backup.task.notes)
    }

    @Test
    fun taskReferenceIsPreserved() {
        val task = Task(title = "reference test")
        val backup = TaskBackup(task = task)
        assertSame(task, backup.task)
    }

    @Test
    fun alarmsAreStored() {
        val alarms = listOf(
            Alarm(time = 1000L, type = Alarm.TYPE_DATE_TIME),
            Alarm(time = 2000L, type = Alarm.TYPE_SNOOZE),
        )
        val backup = TaskBackup(task = Task(), alarms = alarms)
        assertNotNull(backup.alarms)
        assertEquals(2, backup.alarms!!.size)
        assertEquals(Alarm.TYPE_DATE_TIME, backup.alarms!![0].type)
        assertEquals(Alarm.TYPE_SNOOZE, backup.alarms!![1].type)
    }

    @Test
    fun alarmsTimeIsPreserved() {
        val alarms = listOf(Alarm(time = 1680537600000L, type = Alarm.TYPE_DATE_TIME))
        val backup = TaskBackup(task = Task(), alarms = alarms)
        assertEquals(1680537600000L, backup.alarms!![0].time)
    }

    @Test
    fun geofencesAreStored() {
        val geofences = listOf(
            Geofence(place = "place1", isArrival = true),
            Geofence(place = "place2", isDeparture = true),
        )
        val backup = TaskBackup(task = Task(), geofences = geofences)
        assertNotNull(backup.geofences)
        assertEquals(2, backup.geofences!!.size)
        assertEquals("place1", backup.geofences!![0].place)
        assertTrue(backup.geofences!![0].isArrival)
    }

    @Test
    fun geofenceDepartureIsStored() {
        val geofences = listOf(Geofence(place = "home", isDeparture = true))
        val backup = TaskBackup(task = Task(), geofences = geofences)
        assertTrue(backup.geofences!![0].isDeparture)
    }

    @Test
    fun geofenceArrivalDefaultIsFalse() {
        val geofences = listOf(Geofence(place = "work"))
        val backup = TaskBackup(task = Task(), geofences = geofences)
        assertFalse(backup.geofences!![0].isArrival)
    }

    @Test
    fun tagsAreStored() {
        val tags = listOf(
            Tag(name = "tag1", tagUid = "uid1"),
            Tag(name = "tag2", tagUid = "uid2"),
        )
        val backup = TaskBackup(task = Task(), tags = tags)
        assertNotNull(backup.tags)
        assertEquals(2, backup.tags!!.size)
        assertEquals("tag1", backup.tags!![0].name)
        assertEquals("tag2", backup.tags!![1].name)
    }

    @Test
    fun tagUidsAreStored() {
        val tags = listOf(Tag(name = "t", tagUid = "abc-123"))
        val backup = TaskBackup(task = Task(), tags = tags)
        assertEquals("abc-123", backup.tags!![0].tagUid)
    }

    @Test
    fun commentsAreStored() {
        val comments = listOf(
            UserActivity(message = "comment1"),
            UserActivity(message = "comment2"),
        )
        val backup = TaskBackup(task = Task(), comments = comments)
        assertNotNull(backup.comments)
        assertEquals(2, backup.comments!!.size)
        assertEquals("comment1", backup.comments!![0].message)
    }

    @Test
    fun attachmentsAreStored() {
        val attachments = listOf(
            Attachment(attachmentUid = "uid1"),
            Attachment(attachmentUid = "uid2"),
        )
        val backup = TaskBackup(task = Task(), attachments = attachments)
        assertNotNull(backup.attachments)
        assertEquals(2, backup.attachments!!.size)
        assertEquals("uid1", backup.attachments!![0].attachmentUid)
    }

    @Test
    fun caldavTasksAreStored() {
        val caldavTasks = listOf(
            CaldavTask(calendar = "cal1", remoteId = "remote1"),
        )
        val backup = TaskBackup(task = Task(), caldavTasks = caldavTasks)
        assertNotNull(backup.caldavTasks)
        assertEquals(1, backup.caldavTasks!!.size)
        assertEquals("cal1", backup.caldavTasks!![0].calendar)
        assertEquals("remote1", backup.caldavTasks!![0].remoteId)
    }

    @Test
    fun caldavTaskEtagIsStored() {
        val ct = CaldavTask(calendar = "cal", etag = "etag-abc")
        val backup = TaskBackup(task = Task(), caldavTasks = listOf(ct))
        assertEquals("etag-abc", backup.caldavTasks!![0].etag)
    }

    @Test
    fun vtodoIsStored() {
        val vtodo = "BEGIN:VTODO\nSUMMARY:Test\nEND:VTODO"
        val backup = TaskBackup(task = Task(), vtodo = vtodo)
        assertEquals(vtodo, backup.vtodo)
    }

    @Test
    fun emptyListsAreStored() {
        val backup = TaskBackup(
            task = Task(),
            alarms = emptyList(),
            geofences = emptyList(),
            tags = emptyList(),
            comments = emptyList(),
            attachments = emptyList(),
            caldavTasks = emptyList(),
        )
        assertNotNull(backup.alarms)
        assertTrue(backup.alarms!!.isEmpty())
        assertNotNull(backup.geofences)
        assertTrue(backup.geofences!!.isEmpty())
        assertNotNull(backup.tags)
        assertTrue(backup.tags!!.isEmpty())
        assertNotNull(backup.comments)
        assertTrue(backup.comments!!.isEmpty())
        assertNotNull(backup.attachments)
        assertTrue(backup.attachments!!.isEmpty())
        assertNotNull(backup.caldavTasks)
        assertTrue(backup.caldavTasks!!.isEmpty())
    }

    @Test
    fun taskWithAllFieldsPopulated() {
        val task = Task(
            title = "Full task",
            priority = Task.Priority.HIGH,
            dueDate = 1234567890L,
            notes = "detailed notes",
            completionDate = 9876543210L,
        )
        val backup = TaskBackup(
            task = task,
            alarms = listOf(Alarm(type = Alarm.TYPE_REL_START)),
            geofences = listOf(Geofence(place = "office", isArrival = true)),
            tags = listOf(Tag(name = "important")),
            comments = listOf(UserActivity(message = "done")),
            vtodo = "BEGIN:VTODO\nEND:VTODO",
        )
        assertEquals(Task.Priority.HIGH, backup.task.priority)
        assertEquals(1234567890L, backup.task.dueDate)
        assertEquals(9876543210L, backup.task.completionDate)
        assertEquals(1, backup.alarms!!.size)
        assertEquals(1, backup.geofences!!.size)
        assertEquals(1, backup.tags!!.size)
        assertEquals(1, backup.comments!!.size)
    }

    @Test
    fun googleTasksAreStored() {
        val google = listOf(
            GoogleTask(remoteId = "g1", listId = "list1"),
            GoogleTask(remoteId = "g2", listId = "list2"),
        )
        val backup = TaskBackup(task = Task(), google = google)
        assertNotNull(backup.google)
        assertEquals(2, backup.google!!.size)
        assertEquals("g1", backup.google!![0].remoteId)
        assertEquals("list1", backup.google!![0].listId)
    }

    @Test
    fun googleTaskDeletedTimestamp() {
        val google = listOf(GoogleTask(deleted = 1680000000L))
        val backup = TaskBackup(task = Task(), google = google)
        assertEquals(1680000000L, backup.google!![0].deleted)
    }

    @Test
    fun multipleAlarmsWithDifferentTypes() {
        val alarms = listOf(
            Alarm(type = Alarm.TYPE_DATE_TIME, time = 1000L),
            Alarm(type = Alarm.TYPE_REL_START, time = -300000L),
            Alarm(type = Alarm.TYPE_REL_END, time = 600000L),
            Alarm(type = Alarm.TYPE_RANDOM, time = 3600000L),
            Alarm(type = Alarm.TYPE_SNOOZE, time = 9999L),
        )
        val backup = TaskBackup(task = Task(), alarms = alarms)
        assertEquals(5, backup.alarms!!.size)
        assertEquals(Alarm.TYPE_DATE_TIME, backup.alarms!![0].type)
        assertEquals(Alarm.TYPE_REL_START, backup.alarms!![1].type)
        assertEquals(Alarm.TYPE_REL_END, backup.alarms!![2].type)
        assertEquals(Alarm.TYPE_RANDOM, backup.alarms!![3].type)
        assertEquals(Alarm.TYPE_SNOOZE, backup.alarms!![4].type)
    }

    @Test
    fun taskWithNullTitle() {
        val task = Task(title = null)
        val backup = TaskBackup(task = task)
        assertNull(backup.task.title)
    }

    @Test
    fun taskPriorityLevelsAreStored() {
        val highPriority = TaskBackup(task = Task(priority = Task.Priority.HIGH))
        val medPriority = TaskBackup(task = Task(priority = Task.Priority.MEDIUM))
        val lowPriority = TaskBackup(task = Task(priority = Task.Priority.LOW))
        val noPriority = TaskBackup(task = Task(priority = Task.Priority.NONE))
        assertEquals(Task.Priority.HIGH, highPriority.task.priority)
        assertEquals(Task.Priority.MEDIUM, medPriority.task.priority)
        assertEquals(Task.Priority.LOW, lowPriority.task.priority)
        assertEquals(Task.Priority.NONE, noPriority.task.priority)
    }

    @Test
    fun taskDeletionDateIsPreserved() {
        val task = Task(deletionDate = 555555L)
        val backup = TaskBackup(task = task)
        assertEquals(555555L, backup.task.deletionDate)
    }

    @Test
    fun multipleCaldavTasksAreStored() {
        val caldavTasks = listOf(
            CaldavTask(calendar = "cal1", remoteId = "id1"),
            CaldavTask(calendar = "cal2", remoteId = "id2"),
            CaldavTask(calendar = "cal3", remoteId = "id3"),
        )
        val backup = TaskBackup(task = Task(), caldavTasks = caldavTasks)
        assertEquals(3, backup.caldavTasks!!.size)
        assertEquals("cal3", backup.caldavTasks!![2].calendar)
    }

    @Test
    fun emptyVtodoString() {
        val backup = TaskBackup(task = Task(), vtodo = "")
        assertEquals("", backup.vtodo)
    }

    @Test
    fun userActivityCreatedTimestamp() {
        val comments = listOf(UserActivity(message = "note", created = 1700000000L))
        val backup = TaskBackup(task = Task(), comments = comments)
        assertEquals(1700000000L, backup.comments!![0].created)
    }
}
