package org.tasks.caldav

import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.tasks.data.dao.CaldavDao
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavCalendar
import org.tasks.data.entity.CaldavTask
import java.io.File

class VtodoCacheTest {

    private lateinit var caldavDao: CaldavDao
    private lateinit var tempDir: File
    private lateinit var fileStorage: FileStorage
    private lateinit var vtodoCache: VtodoCache

    @Before
    fun setUp() {
        caldavDao = mock()
        tempDir = File(System.getProperty("java.io.tmpdir"), "vtodoCacheTest_${System.nanoTime()}")
        tempDir.mkdirs()
        fileStorage = FileStorage(tempDir.absolutePath)
        vtodoCache = VtodoCache(caldavDao = caldavDao, fileStorage = fileStorage)
    }

    @After
    fun tearDown() { tempDir.deleteRecursively() }

    @Test fun getVtodoReturnsNullForNullTask() = runTest { assertNull(vtodoCache.getVtodo(null)) }

    @Test fun getVtodoReturnsNullWhenCalendarNotFound() = runTest {
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        `when`(caldavDao.getCalendar("cal1")).thenReturn(null)
        assertNull(vtodoCache.getVtodo(task))
    }

    @Test fun getVtodoReturnsContentWhenFound() = runTest {
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        `when`(caldavDao.getCalendar("cal1")).thenReturn(calendar)
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        File(dir, "task.ics").writeText("BEGIN:VCALENDAR")
        assertEquals("BEGIN:VCALENDAR", vtodoCache.getVtodo(task))
    }

    @Test fun getVtodoWithCalendarReturnsContent() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        File(dir, "task.ics").writeText("VTODO data")
        assertEquals("VTODO data", vtodoCache.getVtodo(calendar, task))
    }

    @Test fun getVtodoWithNullCalendarReturnsNull() = runTest {
        assertNull(vtodoCache.getVtodo(null, CaldavTask(calendar = "cal1", obj = "task.ics")))
    }

    @Test fun getVtodoWithNullTaskReturnsNull() = runTest {
        assertNull(vtodoCache.getVtodo(CaldavCalendar(account = "acc1", uuid = "cal1"), null))
    }

    @Test fun getVtodoReturnsNullWhenFileDoesNotExist() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "nonexistent.ics")
        assertNull(vtodoCache.getVtodo(calendar, task))
    }

    @Test fun putVtodoWritesFile() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        vtodoCache.putVtodo(calendar, task, "VCALENDAR content")
        val file = File(fileStorage.root, "acc1/cal1/task.ics")
        assertTrue(file.exists())
        assertEquals("VCALENDAR content", file.readText())
    }

    @Test fun putVtodoWithNullObjDoesNothing() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = null)
        vtodoCache.putVtodo(calendar, task, "data")
        assertFalse(File(fileStorage.root, "acc1/cal1").exists())
    }

    @Test fun putVtodoWithBlankObjDoesNothing() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "  ")
        vtodoCache.putVtodo(calendar, task, "data")
        assertFalse(File(fileStorage.root, "acc1/cal1").exists())
    }

    @Test fun putVtodoOverwritesExisting() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        vtodoCache.putVtodo(calendar, task, "old content")
        vtodoCache.putVtodo(calendar, task, "new content")
        assertEquals("new content", File(fileStorage.root, "acc1/cal1/task.ics").readText())
    }

    @Test fun putVtodoNullDeletesFile() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        vtodoCache.putVtodo(calendar, task, "content")
        val file = File(fileStorage.root, "acc1/cal1/task.ics")
        assertTrue(file.exists())
        vtodoCache.putVtodo(calendar, task, null)
        assertFalse(file.exists())
    }

    @Test fun deleteSingleTaskDeletesFile() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        val file = File(dir, "task.ics"); file.writeText("data")
        vtodoCache.delete(calendar, task)
        assertFalse(file.exists())
    }

    @Test fun deleteSingleTaskWithMissingFileDoesNotThrow() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        vtodoCache.delete(calendar, task)
    }

    @Test fun deleteMultipleTasksDeletesEachFile() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val task1 = CaldavTask(calendar = "cal1", obj = "task1.ics")
        val task2 = CaldavTask(calendar = "cal1", obj = "task2.ics")
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        File(dir, "task1.ics").writeText("d1"); File(dir, "task2.ics").writeText("d2")
        vtodoCache.delete(calendar, listOf(task1, task2))
        assertFalse(File(dir, "task1.ics").exists())
        assertFalse(File(dir, "task2.ics").exists())
    }

    @Test fun deleteEmptyTaskListDoesNothing() = runTest {
        vtodoCache.delete(CaldavCalendar(account = "acc1", uuid = "cal1"), emptyList())
    }

    @Test fun deleteCalendarDeletesDirectory() = runTest {
        val calendar = CaldavCalendar(account = "acc1", uuid = "cal1")
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        File(dir, "task.ics").writeText("data")
        vtodoCache.delete(calendar)
        assertFalse(dir.exists())
    }

    @Test fun deleteCalendarWithMissingDirDoesNotThrow() = runTest {
        vtodoCache.delete(CaldavCalendar(account = "acc1", uuid = "cal1"))
    }

    @Test fun deleteAccountDeletesDirectory() = runTest {
        val account = CaldavAccount(uuid = "acc1")
        val dir = File(fileStorage.root, "acc1"); dir.mkdirs()
        File(dir, "cal1").mkdirs()
        File(File(dir, "cal1"), "task.ics").writeText("data")
        vtodoCache.delete(account)
        assertFalse(dir.exists())
    }

    @Test fun deleteAccountWithMissingDirDoesNotThrow() = runTest {
        vtodoCache.delete(CaldavAccount(uuid = "acc1"))
    }

    @Test fun clearDeletesEverything() = runTest {
        val dir = File(fileStorage.root, "acc1/cal1"); dir.mkdirs()
        File(dir, "task.ics").writeText("data")
        vtodoCache.clear()
        assertFalse(fileStorage.root.exists())
    }

    @Test fun clearWithEmptyRootDoesNotThrow() = runTest { vtodoCache.clear() }

    @Test fun moveDoesNothingWhenSourceDoesNotExist() = runTest {
        val from = CaldavCalendar(account = "acc1", uuid = "cal1")
        val to = CaldavCalendar(account = "acc1", uuid = "cal2")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        vtodoCache.move(from, to, task)
        assertFalse(File(fileStorage.root, "acc1/cal2/task.ics").exists())
    }

    @Test fun moveCopiesFileAndDeletesSource() = runTest {
        val from = CaldavCalendar(account = "acc1", uuid = "cal1")
        val to = CaldavCalendar(account = "acc1", uuid = "cal2")
        val task = CaldavTask(calendar = "cal1", obj = "task.ics")
        val sourceDir = File(fileStorage.root, "acc1/cal1"); sourceDir.mkdirs()
        File(sourceDir, "task.ics").writeText("vtodo content")
        vtodoCache.move(from, to, task)
        assertFalse(File(sourceDir, "task.ics").exists())
        val target = File(fileStorage.root, "acc1/cal2/task.ics")
        assertTrue(target.exists())
        assertEquals("vtodo content", target.readText())
    }
}
