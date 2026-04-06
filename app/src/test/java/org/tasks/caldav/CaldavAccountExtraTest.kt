package org.tasks.caldav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_PAYMENT_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_TOS_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SYNOLOGY_CALENDAR
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5
import org.tasks.data.entity.CaldavAccount.Companion.isDavx5Managed
import org.tasks.data.entity.CaldavAccount.Companion.isDecSync
import org.tasks.data.entity.CaldavAccount.Companion.isEteSync
import org.tasks.data.entity.CaldavAccount.Companion.isPaymentRequired
import org.tasks.data.entity.CaldavAccount.Companion.isTosRequired
import org.tasks.data.entity.CaldavAccount.Companion.openTaskType

class CaldavAccountExtraTest {

    // ===== Account type checks =====

    @Test
    fun caldavAccountIsCaldav() {
        assertTrue(CaldavAccount(accountType = TYPE_CALDAV).isCaldavAccount)
    }

    @Test
    fun tasksAccountIsNotCaldav() {
        assertFalse(CaldavAccount(accountType = TYPE_TASKS).isCaldavAccount)
    }

    @Test
    fun etebaseAccountIsEtebase() {
        assertTrue(CaldavAccount(accountType = TYPE_ETEBASE).isEtebaseAccount)
    }

    @Test
    fun openTasksAccountIsOpenTasks() {
        assertTrue(CaldavAccount(accountType = TYPE_OPENTASKS).isOpenTasks)
    }

    @Test
    fun microsoftAccountIsMicrosoft() {
        assertTrue(CaldavAccount(accountType = TYPE_MICROSOFT).isMicrosoft)
    }

    @Test
    fun googleTasksAccountIsGoogleTasks() {
        assertTrue(CaldavAccount(accountType = TYPE_GOOGLE_TASKS).isGoogleTasks)
    }

    @Test
    fun localAccountIsLocal() {
        assertTrue(CaldavAccount(accountType = TYPE_LOCAL).isLocalList)
    }

    // ===== isTasksOrg =====

    @Test
    fun typeTasksIsTasksOrg() {
        assertTrue(CaldavAccount(accountType = TYPE_TASKS).isTasksOrg)
    }

    @Test
    fun caldavWithTasksOrgUrlIsTasksOrg() {
        assertTrue(
            CaldavAccount(
                accountType = TYPE_CALDAV,
                url = "https://caldav.tasks.org/user/"
            ).isTasksOrg
        )
    }

    @Test
    fun caldavWithStagingUrlIsTasksOrg() {
        assertTrue(
            CaldavAccount(
                accountType = TYPE_CALDAV,
                url = "https://staging.tasks.org/user/"
            ).isTasksOrg
        )
    }

    @Test
    fun caldavWithOtherUrlIsNotTasksOrg() {
        assertFalse(
            CaldavAccount(
                accountType = TYPE_CALDAV,
                url = "https://example.com/dav/"
            ).isTasksOrg
        )
    }

    // ===== needsPro =====

    @Test
    fun caldavNeedsPro() {
        assertTrue(CaldavAccount(accountType = TYPE_CALDAV).needsPro)
    }

    @Test
    fun etebaseNeedsPro() {
        assertTrue(CaldavAccount(accountType = TYPE_ETEBASE).needsPro)
    }

    @Test
    fun openTasksNeedsPro() {
        assertTrue(CaldavAccount(accountType = TYPE_OPENTASKS).needsPro)
    }

    @Test
    fun tasksDoesNotNeedPro() {
        assertFalse(CaldavAccount(accountType = TYPE_TASKS).needsPro)
    }

    @Test
    fun localDoesNotNeedPro() {
        assertFalse(CaldavAccount(accountType = TYPE_LOCAL).needsPro)
    }

    @Test
    fun microsoftDoesNotNeedPro() {
        assertFalse(CaldavAccount(accountType = TYPE_MICROSOFT).needsPro)
    }

    @Test
    fun googleTasksDoesNotNeedPro() {
        assertFalse(CaldavAccount(accountType = TYPE_GOOGLE_TASKS).needsPro)
    }

    // ===== isSuppressRepeatingTasks =====

    @Test
    fun openXchangeSuppressesRepeatingTasks() {
        assertTrue(CaldavAccount(serverType = SERVER_OPEN_XCHANGE).isSuppressRepeatingTasks)
    }

    @Test
    fun mailboxOrgSuppressesRepeatingTasks() {
        assertTrue(CaldavAccount(serverType = SERVER_MAILBOX_ORG).isSuppressRepeatingTasks)
    }

    @Test
    fun unknownServerDoesNotSuppressRepeatingTasks() {
        assertFalse(CaldavAccount(serverType = SERVER_UNKNOWN).isSuppressRepeatingTasks)
    }

    // ===== reminderSync =====

    @Test
    fun synologyCalendarDoesNotSyncReminders() {
        assertFalse(CaldavAccount(serverType = SERVER_SYNOLOGY_CALENDAR).reminderSync)
    }

    @Test
    fun unknownServerSyncsReminders() {
        assertTrue(CaldavAccount(serverType = SERVER_UNKNOWN).reminderSync)
    }

    // ===== error states =====

    @Test
    fun isLoggedOutWithUnauthorizedError() {
        assertTrue(CaldavAccount(error = ERROR_UNAUTHORIZED).isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWithNoError() {
        assertFalse(CaldavAccount(error = "").isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWithNullError() {
        assertFalse(CaldavAccount(error = null).isLoggedOut())
    }

    @Test
    fun isPaymentRequiredWithPaymentError() {
        assertTrue(CaldavAccount(error = ERROR_PAYMENT_REQUIRED).isPaymentRequired())
    }

    @Test
    fun isNotPaymentRequiredWithNoError() {
        assertFalse(CaldavAccount(error = "").isPaymentRequired())
    }

    @Test
    fun isTosRequiredWithTosError() {
        assertTrue(CaldavAccount(error = ERROR_TOS_REQUIRED).isTosRequired())
    }

    @Test
    fun isNotTosRequiredWithNoError() {
        assertFalse(CaldavAccount(error = "").isTosRequired())
    }

    @Test
    fun hasErrorWhenNotBlank() {
        assertTrue(CaldavAccount(error = "some error").hasError)
    }

    @Test
    fun noErrorWhenBlank() {
        assertFalse(CaldavAccount(error = "").hasError)
    }

    @Test
    fun noErrorWhenNull() {
        assertFalse(CaldavAccount(error = null).hasError)
    }

    // ===== String extension functions =====

    @Test
    fun openTaskTypeExtractsType() {
        assertEquals("davx5", "davx5:account_name".openTaskType())
    }

    @Test
    fun openTaskTypeReturnsFullStringWhenNoColon() {
        assertEquals("davx5", "davx5".openTaskType())
    }

    @Test
    fun openTaskTypeNullReturnsNull() {
        assertNull((null as String?).openTaskType())
    }

    @Test
    fun isDavx5Positive() {
        assertTrue("bitfire.at.davdroid".isDavx5())
    }

    @Test
    fun isDavx5NullReturnsFalse() {
        assertFalse((null as String?).isDavx5())
    }

    @Test
    fun isDavx5ManagedPositive() {
        assertTrue("com.davdroid".isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedNullReturnsFalse() {
        assertFalse((null as String?).isDavx5Managed())
    }

    @Test
    fun isEteSyncPositive() {
        assertTrue("com.etesync.syncadapter".isEteSync())
    }

    @Test
    fun isEteSyncNullReturnsFalse() {
        assertFalse((null as String?).isEteSync())
    }

    @Test
    fun isDecSyncPositive() {
        assertTrue("org.decsync.tasks".isDecSync())
    }

    @Test
    fun isDecSyncNullReturnsFalse() {
        assertFalse((null as String?).isDecSync())
    }

    @Test
    fun isPaymentRequiredStringPositive() {
        assertTrue(ERROR_PAYMENT_REQUIRED.isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredStringNullReturnsFalse() {
        assertFalse((null as String?).isPaymentRequired())
    }

    @Test
    fun isTosRequiredStringPositive() {
        assertTrue(ERROR_TOS_REQUIRED.isTosRequired())
    }

    @Test
    fun isTosRequiredStringNullReturnsFalse() {
        assertFalse((null as String?).isTosRequired())
    }

    private fun assertNull(value: Any?) {
        org.junit.Assert.assertNull(value)
    }
}
