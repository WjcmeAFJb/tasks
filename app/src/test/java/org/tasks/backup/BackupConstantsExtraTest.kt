package org.tasks.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.time.DateTime

class BackupConstantsExtraTest {

    // --- Constants ---

    @Test
    fun internalBackupValue() {
        assertEquals("backup.json", BackupConstants.INTERNAL_BACKUP)
    }

    @Test
    fun exportFileNameValue() {
        assertEquals("user.%s.json", BackupConstants.EXPORT_FILE_NAME)
    }

    @Test
    fun backupFileNameValue() {
        assertEquals("auto.%s.json", BackupConstants.BACKUP_FILE_NAME)
    }

    // --- isBackupFile ---

    @Test
    fun nullReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile(null))
    }

    @Test
    fun emptyStringReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile(""))
    }

    @Test
    fun randomStringReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("random_file.txt"))
    }

    @Test
    fun internalBackupDoesNotMatchPattern() {
        assertFalse(BackupConstants.isBackupFile("backup.json"))
    }

    @Test
    fun autoBackupWithDashSeparator() {
        assertTrue(BackupConstants.isBackupFile("auto.200909-0003.json"))
    }

    @Test
    fun userBackupWithDashSeparator() {
        assertTrue(BackupConstants.isBackupFile("user.200909-1503.json"))
    }

    @Test
    fun autoBackupWithTSeparator() {
        assertTrue(BackupConstants.isBackupFile("auto.20200910T1503.json"))
    }

    @Test
    fun userBackupWithTSeparator() {
        assertTrue(BackupConstants.isBackupFile("user.20200910T1503.json"))
    }

    @Test
    fun fourDigitYearWithDash() {
        assertTrue(BackupConstants.isBackupFile("auto.20200910-1503.json"))
    }

    @Test
    fun invalidPrefixReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("manual.200909-0003.json"))
    }

    @Test
    fun missingExtensionReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("auto.200909-0003"))
    }

    @Test
    fun wrongExtensionReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("auto.200909-0003.xml"))
    }

    @Test
    fun extraCharactersAfterMatchReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("auto.200909-0003.json.bak"))
    }

    @Test
    fun copiedFileReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("user.200909-1503 (1).json"))
    }

    @Test
    fun singleDigitDatePartsReturnFalse() {
        assertFalse(BackupConstants.isBackupFile("auto.2091-03.json"))
    }

    @Test
    fun tooFewDigitsReturnsFalse() {
        assertFalse(BackupConstants.isBackupFile("auto.20091-03.json"))
    }

    // --- BACKUP_CLEANUP_MATCHER ---

    @Test
    fun cleanupMatcherMatchesAutoBackupSixDigitDate() {
        assertTrue(BackupConstants.BACKUP_CLEANUP_MATCHER.containsMatchIn("auto.200910-1503.json"))
    }

    @Test
    fun cleanupMatcherMatchesAutoBackupEightDigitDate() {
        assertTrue(BackupConstants.BACKUP_CLEANUP_MATCHER.containsMatchIn("auto.20200910T1503.json"))
    }

    @Test
    fun cleanupMatcherDoesNotMatchUserBackup() {
        assertFalse(BackupConstants.BACKUP_CLEANUP_MATCHER.containsMatchIn("user.200910-1503.json"))
    }

    @Test
    fun cleanupMatcherDoesNotMatchRandomString() {
        assertFalse(BackupConstants.BACKUP_CLEANUP_MATCHER.containsMatchIn("random.json"))
    }

    @Test
    fun cleanupMatcherDoesNotMatchInternalBackup() {
        assertFalse(BackupConstants.BACKUP_CLEANUP_MATCHER.containsMatchIn("backup.json"))
    }

    // --- getTimestampFromFilename ---

    @Test
    fun timestampFromAutoBackupTwoDigitYear() {
        assertEquals(
            DateTime(2020, 9, 10, 15, 3).millis,
            BackupConstants.getTimestampFromFilename("auto.200910-1503.json")
        )
    }

    @Test
    fun timestampFromAutoBackupFourDigitYear() {
        assertEquals(
            DateTime(2020, 9, 10, 15, 3).millis,
            BackupConstants.getTimestampFromFilename("auto.20200910T1503.json")
        )
    }

    @Test
    fun timestampFromUserBackupTwoDigitYear() {
        assertEquals(
            DateTime(2020, 9, 10, 15, 3).millis,
            BackupConstants.getTimestampFromFilename("user.200910-1503.json")
        )
    }

    @Test
    fun timestampFromUserBackupFourDigitYear() {
        assertEquals(
            DateTime(2020, 9, 10, 15, 3).millis,
            BackupConstants.getTimestampFromFilename("user.20200910T1503.json")
        )
    }

    @Test
    fun timestampFromInvalidNameReturnsNull() {
        assertNull(BackupConstants.getTimestampFromFilename("invalid.json"))
    }

    @Test
    fun timestampFromEmptyStringReturnsNull() {
        assertNull(BackupConstants.getTimestampFromFilename(""))
    }

    @Test
    fun timestampFromBackupJsonReturnsNull() {
        assertNull(BackupConstants.getTimestampFromFilename("backup.json"))
    }

    @Test
    fun timestampFromFourDigitYearWithDash() {
        assertEquals(
            DateTime(2021, 1, 15, 8, 30).millis,
            BackupConstants.getTimestampFromFilename("auto.20210115-0830.json")
        )
    }

    @Test
    fun timestampFromTwoDigitYearAdds2000() {
        // 21 -> 2021
        assertEquals(
            DateTime(2021, 1, 15, 8, 30).millis,
            BackupConstants.getTimestampFromFilename("auto.210115-0830.json")
        )
    }

    @Test
    fun timestampIsConsistentBetweenTAndDashSeparators() {
        val withT = BackupConstants.getTimestampFromFilename("auto.20200910T1503.json")
        val withDash = BackupConstants.getTimestampFromFilename("auto.20200910-1503.json")
        assertNotNull(withT)
        assertNotNull(withDash)
        assertEquals(withT, withDash)
    }

    @Test
    fun timestampFromPartialMatchReturnsNull() {
        assertNull(BackupConstants.getTimestampFromFilename("auto.2009-03.json"))
    }

    @Test
    fun timestampFromCopiedFileReturnsNull() {
        assertNull(BackupConstants.getTimestampFromFilename("user.200910-1503 (1).json"))
    }
}
