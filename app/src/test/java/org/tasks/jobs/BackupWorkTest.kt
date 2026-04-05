package org.tasks.jobs

import org.junit.Assert.*
import org.junit.Test
import org.mockito.Mockito
import java.io.File

class BackupWorkTest {
    @Test
    fun filterExcludesXmlFiles() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("/a/b/c/d/auto.180329-0001.xml")))
    }

    @Test
    fun filterIncludesJsonFiles() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("/a/b/c/d/auto.180329-0001.json")))
    }

    @Test
    fun getDeleteKeepAllFiles() {
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        val file3 = newFile("auto.180329-0000.json")
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 7))
    }

    @Test
    fun getDeleteFromNullFileList() {
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(null, 2))
    }

    @Test
    fun sortFiles() {
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        val file3 = newFile("auto.180329-0000.json")
        assertEquals(
                listOf(file1), BackupWork.getDeleteList(arrayOf(file2, file1, file3), 2))
    }

    // --- FILE_FILTER additional tests ---

    @Test
    fun filterExcludesUserBackups() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("/a/user.180329-0001.json")))
    }

    @Test
    fun filterExcludesRandomFiles() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("/a/notes.txt")))
    }

    @Test
    fun filterIncludesAutoBackupWithTSeparator() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("/a/auto.20230329T0001.json")))
    }

    @Test
    fun filterIncludesAutoBackupShortDate() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("/a/auto.230329-0001.json")))
    }

    @Test
    fun filterExcludesEmptyFilename() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("")))
    }

    // --- getDeleteList additional tests ---

    @Test
    fun getDeleteListKeepNewestOne() {
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        val file3 = newFile("auto.180329-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(file2, file1, file3), 1)
        assertEquals(2, deleted.size)
        // Newest is file3, so file2 and file1 should be deleted
        assertTrue(deleted.contains(file1))
        assertTrue(deleted.contains(file2))
    }

    @Test
    fun getDeleteListKeepZero() {
        val file1 = newFile("auto.180327-0000.json")
        val file2 = newFile("auto.180328-0000.json")
        assertEquals(2, BackupWork.getDeleteList(arrayOf(file1, file2), 0).size)
    }

    @Test
    fun getDeleteListFromEmptyArray() {
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(emptyArray(), 7))
    }

    @Test
    fun getDeleteListSingleFileKeepOne() {
        val file1 = newFile("auto.180327-0000.json")
        assertEquals(emptyList<Any>(), BackupWork.getDeleteList(arrayOf(file1), 1))
    }

    @Test
    fun getDeleteListSingleFileKeepZero() {
        val file1 = newFile("auto.180327-0000.json")
        assertEquals(listOf(file1), BackupWork.getDeleteList(arrayOf(file1), 0))
    }

    @Test
    fun getDeleteListPreservesNewest() {
        val file1 = newFile("auto.180101-0000.json")
        val file2 = newFile("auto.180601-0000.json")
        val file3 = newFile("auto.181201-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(file1, file3, file2), 2)
        assertEquals(1, deleted.size)
        // file1 has the oldest timestamp, should be deleted
        assertEquals(file1, deleted[0])
    }

    @Test
    fun getDeleteListWithYearFormat() {
        val file1 = newFile("auto.20180101-0000.json")
        val file2 = newFile("auto.20180601-0000.json")
        val file3 = newFile("auto.20181201-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(file1, file3, file2), 1)
        assertEquals(2, deleted.size)
    }

    // --- DAYS_TO_KEEP_BACKUP constant ---

    @Test
    fun daysToKeepBackupIsSeven() {
        assertEquals(7, BackupWork.DAYS_TO_KEEP_BACKUP)
    }

    companion object {
        private fun newFile(name: String): File {
            val result = Mockito.mock(File::class.java)
            Mockito.`when`(result.name).thenReturn(name)
            return result
        }
    }
}