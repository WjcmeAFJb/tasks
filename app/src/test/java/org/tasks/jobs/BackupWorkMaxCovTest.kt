package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.tasks.backup.BackupConstants
import java.io.File

class BackupWorkMaxCovTest {

    // ================================================================
    // FILE_FILTER — comprehensive filename matching
    // ================================================================

    @Test
    fun fileFilterAcceptsAutoShortDate() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("auto.230101-0000.json")))
    }

    @Test
    fun fileFilterAcceptsAutoLongDate() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("auto.20230101-0000.json")))
    }

    @Test
    fun fileFilterAcceptsAutoTSeparator() {
        assertTrue(BackupWork.FILE_FILTER.accept(File("auto.20230101T0000.json")))
    }

    @Test
    fun fileFilterRejectsUserBackup() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("user.230101-0000.json")))
    }

    @Test
    fun fileFilterRejectsXml() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("auto.230101-0000.xml")))
    }

    @Test
    fun fileFilterRejectsTxt() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("notes.txt")))
    }

    @Test
    fun fileFilterRejectsPartialMatch() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("auto.json")))
    }

    @Test
    fun fileFilterRejectsNoExtension() {
        assertFalse(BackupWork.FILE_FILTER.accept(File("auto.230101-0000")))
    }

    // ================================================================
    // getDeleteList — comprehensive sorting and trimming
    // ================================================================

    private fun mockFile(name: String): File {
        val file: File = mock()
        whenever(file.name).thenReturn(name)
        return file
    }

    @Test
    fun getDeleteListNullReturnsEmpty() {
        assertEquals(emptyList<File>(), BackupWork.getDeleteList(null, 7))
    }

    @Test
    fun getDeleteListEmptyReturnsEmpty() {
        assertEquals(emptyList<File>(), BackupWork.getDeleteList(emptyArray(), 7))
    }

    @Test
    fun getDeleteListKeepAllWhenFewer() {
        val f1 = mockFile("auto.230101-0000.json")
        val f2 = mockFile("auto.230102-0000.json")
        assertEquals(0, BackupWork.getDeleteList(arrayOf(f1, f2), 5).size)
    }

    @Test
    fun getDeleteListKeepExact() {
        val f1 = mockFile("auto.230101-0000.json")
        val f2 = mockFile("auto.230102-0000.json")
        assertEquals(0, BackupWork.getDeleteList(arrayOf(f1, f2), 2).size)
    }

    @Test
    fun getDeleteListDropOldest() {
        val f1 = mockFile("auto.230101-0000.json")
        val f2 = mockFile("auto.230601-0000.json")
        val f3 = mockFile("auto.231201-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(f1, f2, f3), 2)
        assertEquals(1, deleted.size)
        assertEquals(f1, deleted[0])
    }

    @Test
    fun getDeleteListDropAllButOne() {
        val f1 = mockFile("auto.230101-0000.json")
        val f2 = mockFile("auto.230601-0000.json")
        val f3 = mockFile("auto.231201-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(f1, f2, f3), 1)
        assertEquals(2, deleted.size)
    }

    @Test
    fun getDeleteListDropAllKeepZero() {
        val f1 = mockFile("auto.230101-0000.json")
        val f2 = mockFile("auto.230601-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(f1, f2), 0)
        assertEquals(2, deleted.size)
    }

    @Test
    fun getDeleteListLongDateFormat() {
        val f1 = mockFile("auto.20220101-0000.json")
        val f2 = mockFile("auto.20230601-0000.json")
        val f3 = mockFile("auto.20240101-0000.json")
        val deleted = BackupWork.getDeleteList(arrayOf(f1, f2, f3), 2)
        assertEquals(1, deleted.size)
        assertEquals(f1, deleted[0])
    }

    // ================================================================
    // BACKUP_CLEANUP_MATCHER — regex validation
    // ================================================================

    @Test
    fun backupCleanupMatcherMatchesAutoShortDate() {
        assertTrue("auto.230101-0000.json".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    @Test
    fun backupCleanupMatcherMatchesAutoLongDate() {
        assertTrue("auto.20230101-0000.json".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    @Test
    fun backupCleanupMatcherMatchesTSeparator() {
        assertTrue("auto.20230101T0000.json".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    @Test
    fun backupCleanupMatcherRejectsUserBackup() {
        assertFalse("user.230101-0000.json".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    @Test
    fun backupCleanupMatcherRejectsXml() {
        assertFalse("auto.230101-0000.xml".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    @Test
    fun backupCleanupMatcherRejectsShortNumbers() {
        assertFalse("auto.12345-0000.json".matches(BackupConstants.BACKUP_CLEANUP_MATCHER))
    }

    // ================================================================
    // BackupConstants.isBackupFile
    // ================================================================

    @Test
    fun isBackupFileAutoBackup() {
        assertTrue(BackupConstants.isBackupFile("auto.230101-0000.json"))
    }

    @Test
    fun isBackupFileUserExport() {
        assertTrue(BackupConstants.isBackupFile("user.230101-0000.json"))
    }

    @Test
    fun isBackupFileLongDate() {
        assertTrue(BackupConstants.isBackupFile("auto.20230101-0000.json"))
    }

    @Test
    fun isBackupFileTSeparator() {
        assertTrue(BackupConstants.isBackupFile("auto.20230101T0000.json"))
    }

    @Test
    fun isBackupFileNull() {
        assertFalse(BackupConstants.isBackupFile(null))
    }

    @Test
    fun isBackupFileRandom() {
        assertFalse(BackupConstants.isBackupFile("random.txt"))
    }

    // ================================================================
    // BackupConstants.getTimestampFromFilename
    // ================================================================

    @Test
    fun getTimestampFromAutoBackup() {
        val ts = BackupConstants.getTimestampFromFilename("auto.230101-0000.json")
        assertTrue(ts != null && ts > 0)
    }

    @Test
    fun getTimestampFromUserExport() {
        val ts = BackupConstants.getTimestampFromFilename("user.230601-1200.json")
        assertTrue(ts != null && ts > 0)
    }

    @Test
    fun getTimestampFromLongDate() {
        val ts = BackupConstants.getTimestampFromFilename("auto.20230101-0000.json")
        assertTrue(ts != null && ts > 0)
    }

    @Test
    fun getTimestampFromNonBackupReturnsNull() {
        val ts = BackupConstants.getTimestampFromFilename("random.txt")
        assertEquals(null, ts)
    }

    @Test
    fun getTimestampFromFileObject() {
        val file: File = mock()
        whenever(file.name).thenReturn("auto.230615-1430.json")
        whenever(file.lastModified()).thenReturn(0L)
        val ts = BackupConstants.getTimestamp(file)
        assertTrue(ts > 0)
    }

    @Test
    fun getTimestampFromFileObjectFallsBackToLastModified() {
        val file: File = mock()
        whenever(file.name).thenReturn("random.txt")
        whenever(file.lastModified()).thenReturn(12345L)
        val ts = BackupConstants.getTimestamp(file)
        assertEquals(12345L, ts)
    }

    // ================================================================
    // DAYS_TO_KEEP_BACKUP constant
    // ================================================================

    @Test
    fun daysToKeepBackup() {
        assertEquals(7, BackupWork.DAYS_TO_KEEP_BACKUP)
    }

    // ================================================================
    // BackupConstants file name templates
    // ================================================================

    @Test
    fun backupFileNameFormat() {
        val result = String.format(BackupConstants.BACKUP_FILE_NAME, "20240101T1200")
        assertEquals("auto.20240101T1200.json", result)
    }

    @Test
    fun exportFileNameFormat() {
        val result = String.format(BackupConstants.EXPORT_FILE_NAME, "20240101T1200")
        assertEquals("user.20240101T1200.json", result)
    }

    @Test
    fun internalBackupConstant() {
        assertEquals("backup.json", BackupConstants.INTERNAL_BACKUP)
    }
}
