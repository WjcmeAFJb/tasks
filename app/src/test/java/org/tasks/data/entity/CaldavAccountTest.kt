package org.tasks.data.entity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_PAYMENT_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_TOS_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OTHER
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OWNCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SABREDAV
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SYNOLOGY_CALENDAR
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_TASKS
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

class CaldavAccountTest {

    // --- isCaldavAccount ---

    @Test
    fun isCaldavAccountForTypeCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertTrue(account.isCaldavAccount)
    }

    @Test
    fun isNotCaldavAccountForOtherType() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertFalse(account.isCaldavAccount)
    }

    // --- isEtebaseAccount ---

    @Test
    fun isEtebaseAccountForTypeEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertTrue(account.isEtebaseAccount)
    }

    @Test
    fun isNotEtebaseAccountForOtherType() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isEtebaseAccount)
    }

    // --- isOpenTasks ---

    @Test
    fun isOpenTasksForTypeOpenTasks() {
        val account = CaldavAccount(accountType = TYPE_OPENTASKS)
        assertTrue(account.isOpenTasks)
    }

    @Test
    fun isNotOpenTasksForOtherType() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isOpenTasks)
    }

    // --- isTasksOrg ---

    @Test
    fun isTasksOrgForTypeTasks() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertTrue(account.isTasksOrg)
    }

    @Test
    fun isTasksOrgForCaldavWithTasksOrgUrl() {
        val account = CaldavAccount(
            accountType = TYPE_CALDAV,
            url = "https://caldav.tasks.org/user/calendars/"
        )
        assertTrue(account.isTasksOrg)
    }

    @Test
    fun isTasksOrgForCaldavWithStagingUrl() {
        val account = CaldavAccount(
            accountType = TYPE_CALDAV,
            url = "https://staging.tasks.org/user/calendars/"
        )
        assertTrue(account.isTasksOrg)
    }

    @Test
    fun isNotTasksOrgForCaldavWithOtherUrl() {
        val account = CaldavAccount(
            accountType = TYPE_CALDAV,
            url = "https://example.com/caldav/"
        )
        assertFalse(account.isTasksOrg)
    }

    @Test
    fun isNotTasksOrgForMicrosoftType() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertFalse(account.isTasksOrg)
    }

    // --- isMicrosoft ---

    @Test
    fun isMicrosoftForTypeMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertTrue(account.isMicrosoft)
    }

    @Test
    fun isNotMicrosoftForOtherType() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isMicrosoft)
    }

    // --- isGoogleTasks ---

    @Test
    fun isGoogleTasksForTypeGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertTrue(account.isGoogleTasks)
    }

    @Test
    fun isNotGoogleTasksForOtherType() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isGoogleTasks)
    }

    // --- isLocalList ---

    @Test
    fun isLocalListForTypeLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertTrue(account.isLocalList)
    }

    @Test
    fun isNotLocalListForOtherType() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertFalse(account.isLocalList)
    }

    // --- needsPro ---

    @Test
    fun needsProForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertTrue(account.needsPro)
    }

    @Test
    fun needsProForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertTrue(account.needsPro)
    }

    @Test
    fun needsProForOpenTasks() {
        val account = CaldavAccount(accountType = TYPE_OPENTASKS)
        assertTrue(account.needsPro)
    }

    @Test
    fun doesNotNeedProForTasks() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertFalse(account.needsPro)
    }

    @Test
    fun doesNotNeedProForMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertFalse(account.needsPro)
    }

    @Test
    fun doesNotNeedProForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertFalse(account.needsPro)
    }

    @Test
    fun doesNotNeedProForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertFalse(account.needsPro)
    }

    // --- isLoggedOut ---

    @Test
    fun isLoggedOutWhenUnauthorizedError() {
        val account = CaldavAccount(error = ERROR_UNAUTHORIZED)
        assertTrue(account.isLoggedOut())
    }

    @Test
    fun isLoggedOutWhenUnauthorizedErrorWithDetails() {
        val account = CaldavAccount(error = "$ERROR_UNAUTHORIZED Unauthorized")
        assertTrue(account.isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWhenNoError() {
        val account = CaldavAccount(error = "")
        assertFalse(account.isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWhenNullError() {
        val account = CaldavAccount(error = null)
        assertFalse(account.isLoggedOut())
    }

    @Test
    fun isNotLoggedOutWhenDifferentError() {
        val account = CaldavAccount(error = "HTTP 500")
        assertFalse(account.isLoggedOut())
    }

    // --- isPaymentRequired ---

    @Test
    fun isPaymentRequiredWhenPaymentError() {
        val account = CaldavAccount(error = ERROR_PAYMENT_REQUIRED)
        assertTrue(account.isPaymentRequired())
    }

    @Test
    fun isPaymentRequiredWithDetails() {
        val account = CaldavAccount(error = "$ERROR_PAYMENT_REQUIRED Payment Required")
        assertTrue(account.isPaymentRequired())
    }

    @Test
    fun isNotPaymentRequiredWhenNoError() {
        val account = CaldavAccount(error = "")
        assertFalse(account.isPaymentRequired())
    }

    @Test
    fun isNotPaymentRequiredWhenNull() {
        val account = CaldavAccount(error = null)
        assertFalse(account.isPaymentRequired())
    }

    // --- isTosRequired ---

    @Test
    fun isTosRequiredWhenTosError() {
        val account = CaldavAccount(error = ERROR_TOS_REQUIRED)
        assertTrue(account.isTosRequired())
    }

    @Test
    fun isTosRequiredWithDetails() {
        val account = CaldavAccount(error = "$ERROR_TOS_REQUIRED Terms of Service")
        assertTrue(account.isTosRequired())
    }

    @Test
    fun isNotTosRequiredWhenNoError() {
        val account = CaldavAccount(error = "")
        assertFalse(account.isTosRequired())
    }

    @Test
    fun isNotTosRequiredWhenNull() {
        val account = CaldavAccount(error = null)
        assertFalse(account.isTosRequired())
    }

    // --- hasError ---

    @Test
    fun hasErrorWhenErrorIsSet() {
        val account = CaldavAccount(error = "Something went wrong")
        assertTrue(account.hasError)
    }

    @Test
    fun hasNoErrorWhenErrorIsEmpty() {
        val account = CaldavAccount(error = "")
        assertFalse(account.hasError)
    }

    @Test
    fun hasNoErrorWhenErrorIsNull() {
        val account = CaldavAccount(error = null)
        assertFalse(account.hasError)
    }

    @Test
    fun hasNoErrorWhenErrorIsBlank() {
        val account = CaldavAccount(error = "   ")
        assertFalse(account.hasError)
    }

    // --- reminderSync ---

    @Test
    fun reminderSyncTrueForUnknownServer() {
        val account = CaldavAccount(serverType = SERVER_UNKNOWN)
        assertTrue(account.reminderSync)
    }

    @Test
    fun reminderSyncTrueForNextcloud() {
        val account = CaldavAccount(serverType = SERVER_NEXTCLOUD)
        assertTrue(account.reminderSync)
    }

    @Test
    fun reminderSyncTrueForOtherServer() {
        val account = CaldavAccount(serverType = SERVER_OTHER)
        assertTrue(account.reminderSync)
    }

    @Test
    fun reminderSyncFalseForSynologyCalendar() {
        val account = CaldavAccount(serverType = SERVER_SYNOLOGY_CALENDAR)
        assertFalse(account.reminderSync)
    }

    // --- isSuppressRepeatingTasks ---

    @Test
    fun suppressRepeatingTasksForOpenXchange() {
        val account = CaldavAccount(serverType = SERVER_OPEN_XCHANGE)
        assertTrue(account.isSuppressRepeatingTasks)
    }

    @Test
    fun suppressRepeatingTasksForMailboxOrg() {
        val account = CaldavAccount(serverType = SERVER_MAILBOX_ORG)
        assertTrue(account.isSuppressRepeatingTasks)
    }

    @Test
    fun doNotSuppressRepeatingTasksForNextcloud() {
        val account = CaldavAccount(serverType = SERVER_NEXTCLOUD)
        assertFalse(account.isSuppressRepeatingTasks)
    }

    @Test
    fun doNotSuppressRepeatingTasksForUnknown() {
        val account = CaldavAccount(serverType = SERVER_UNKNOWN)
        assertFalse(account.isSuppressRepeatingTasks)
    }

    @Test
    fun doNotSuppressRepeatingTasksForOther() {
        val account = CaldavAccount(serverType = SERVER_OTHER)
        assertFalse(account.isSuppressRepeatingTasks)
    }

    // --- TYPE_ constant values ---

    @Test
    fun typeCaldavIsZero() {
        assertEquals(0, TYPE_CALDAV)
    }

    @Test
    fun typeLocalIsTwo() {
        assertEquals(2, TYPE_LOCAL)
    }

    @Test
    fun typeOpenTasksIsThree() {
        assertEquals(3, TYPE_OPENTASKS)
    }

    @Test
    fun typeTasksIsFour() {
        assertEquals(4, TYPE_TASKS)
    }

    @Test
    fun typeEtebaseIsFive() {
        assertEquals(5, TYPE_ETEBASE)
    }

    @Test
    fun typeMicrosoftIsSix() {
        assertEquals(6, TYPE_MICROSOFT)
    }

    @Test
    fun typeGoogleTasksIsSeven() {
        assertEquals(7, TYPE_GOOGLE_TASKS)
    }

    // --- SERVER_ constant values ---

    @Test
    fun serverUnknownIsNegativeOne() {
        assertEquals(-1, SERVER_UNKNOWN)
    }

    @Test
    fun serverTasksIsZero() {
        assertEquals(0, SERVER_TASKS)
    }

    @Test
    fun serverOwncloudIsOne() {
        assertEquals(1, SERVER_OWNCLOUD)
    }

    @Test
    fun serverSabredavIsTwo() {
        assertEquals(2, SERVER_SABREDAV)
    }

    @Test
    fun serverOpenXchangeIsThree() {
        assertEquals(3, SERVER_OPEN_XCHANGE)
    }

    @Test
    fun serverNextcloudIsFour() {
        assertEquals(4, SERVER_NEXTCLOUD)
    }

    @Test
    fun serverSynologyCalendarIsFive() {
        assertEquals(5, SERVER_SYNOLOGY_CALENDAR)
    }

    @Test
    fun serverMailboxOrgIsSix() {
        assertEquals(6, SERVER_MAILBOX_ORG)
    }

    @Test
    fun serverOtherIs99() {
        assertEquals(99, SERVER_OTHER)
    }

    // --- ERROR_ constants ---

    @Test
    fun errorUnauthorizedConstant() {
        assertEquals("HTTP 401", ERROR_UNAUTHORIZED)
    }

    @Test
    fun errorPaymentRequiredConstant() {
        assertEquals("HTTP 402", ERROR_PAYMENT_REQUIRED)
    }

    @Test
    fun errorTosRequiredConstant() {
        assertEquals("HTTP 451", ERROR_TOS_REQUIRED)
    }

    // --- ACCOUNT_TYPE_ constants ---

    @Test
    fun accountTypeDavx5Constant() {
        assertEquals("bitfire.at.davdroid", ACCOUNT_TYPE_DAVX5)
    }

    @Test
    fun accountTypeDavx5ManagedConstant() {
        assertEquals("com.davdroid", ACCOUNT_TYPE_DAVX5_MANAGED)
    }

    @Test
    fun accountTypeEteSyncConstant() {
        assertEquals("com.etesync.syncadapter", ACCOUNT_TYPE_ETESYNC)
    }

    @Test
    fun accountTypeDecSyncConstant() {
        assertEquals("org.decsync.tasks", ACCOUNT_TYPE_DECSYNC)
    }

    // --- PACKAGE_ constants ---

    @Test
    fun packageDavx5Constant() {
        assertEquals("at.bitfire.davdroid", PACKAGE_DAVX5)
    }

    @Test
    fun packageDavx5ManagedConstant() {
        assertEquals("com.davdroid", PACKAGE_DAVX5_MANAGED)
    }

    @Test
    fun packageEteSyncConstant() {
        assertEquals("com.etesync.syncadapter", PACKAGE_ETESYNC)
    }

    @Test
    fun packageDecSyncConstant() {
        assertEquals("org.decsync.cc", PACKAGE_DECSYNC)
    }

    // --- Default construction values ---

    @Test
    fun defaultIdIsZero() {
        assertEquals(0L, CaldavAccount().id)
    }

    @Test
    fun defaultUuidIsNoUuid() {
        assertEquals(Task.NO_UUID, CaldavAccount().uuid)
    }

    @Test
    fun defaultNameIsEmpty() {
        assertEquals("", CaldavAccount().name)
    }

    @Test
    fun defaultUrlIsEmpty() {
        assertEquals("", CaldavAccount().url)
    }

    @Test
    fun defaultUsernameIsEmpty() {
        assertEquals("", CaldavAccount().username)
    }

    @Test
    fun defaultPasswordIsEmpty() {
        assertEquals("", CaldavAccount().password)
    }

    @Test
    fun defaultErrorIsEmpty() {
        assertEquals("", CaldavAccount().error)
    }

    @Test
    fun defaultAccountTypeIsCaldav() {
        assertEquals(TYPE_CALDAV, CaldavAccount().accountType)
    }

    @Test
    fun defaultIsCollapsedIsFalse() {
        assertFalse(CaldavAccount().isCollapsed)
    }

    @Test
    fun defaultServerTypeIsUnknown() {
        assertEquals(SERVER_UNKNOWN, CaldavAccount().serverType)
    }

    @Test
    fun defaultLastSyncIsZero() {
        assertEquals(0L, CaldavAccount().lastSync)
    }

    // --- Extension functions ---

    @Test
    fun isDavx5ReturnsTrueForDavx5AccountType() {
        assertTrue(ACCOUNT_TYPE_DAVX5.isDavx5())
    }

    @Test
    fun isDavx5ReturnsTrueForDavx5WithSuffix() {
        assertTrue("${ACCOUNT_TYPE_DAVX5}:extra".isDavx5())
    }

    @Test
    fun isDavx5ReturnsFalseForOtherType() {
        assertFalse("com.other.app".isDavx5())
    }

    @Test
    fun isDavx5ReturnsFalseForNull() {
        assertFalse((null as String?).isDavx5())
    }

    @Test
    fun isDavx5ManagedReturnsTrueForManagedAccountType() {
        assertTrue(ACCOUNT_TYPE_DAVX5_MANAGED.isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedReturnsFalseForNull() {
        assertFalse((null as String?).isDavx5Managed())
    }

    @Test
    fun isDavx5ManagedReturnsFalseForOtherType() {
        assertFalse("com.other.app".isDavx5Managed())
    }

    @Test
    fun isEteSyncReturnsTrueForEteSyncAccountType() {
        assertTrue(ACCOUNT_TYPE_ETESYNC.isEteSync())
    }

    @Test
    fun isEteSyncReturnsFalseForNull() {
        assertFalse((null as String?).isEteSync())
    }

    @Test
    fun isEteSyncReturnsFalseForOtherType() {
        assertFalse("com.other.app".isEteSync())
    }

    @Test
    fun isDecSyncReturnsTrueForDecSyncAccountType() {
        assertTrue(ACCOUNT_TYPE_DECSYNC.isDecSync())
    }

    @Test
    fun isDecSyncReturnsFalseForNull() {
        assertFalse((null as String?).isDecSync())
    }

    @Test
    fun isDecSyncReturnsFalseForOtherType() {
        assertFalse("com.other.app".isDecSync())
    }

    // --- Extension function: isPaymentRequired on String ---

    @Test
    fun stringIsPaymentRequiredTrue() {
        assertTrue(ERROR_PAYMENT_REQUIRED.isPaymentRequired())
    }

    @Test
    fun stringIsPaymentRequiredWithSuffix() {
        assertTrue("${ERROR_PAYMENT_REQUIRED} extra detail".isPaymentRequired())
    }

    @Test
    fun stringIsPaymentRequiredFalseForNull() {
        assertFalse((null as String?).isPaymentRequired())
    }

    @Test
    fun stringIsPaymentRequiredFalseForOtherError() {
        assertFalse("HTTP 500".isPaymentRequired())
    }

    // --- Extension function: isTosRequired on String ---

    @Test
    fun stringIsTosRequiredTrue() {
        assertTrue(ERROR_TOS_REQUIRED.isTosRequired())
    }

    @Test
    fun stringIsTosRequiredWithSuffix() {
        assertTrue("${ERROR_TOS_REQUIRED} Unavailable For Legal Reasons".isTosRequired())
    }

    @Test
    fun stringIsTosRequiredFalseForNull() {
        assertFalse((null as String?).isTosRequired())
    }

    @Test
    fun stringIsTosRequiredFalseForOtherError() {
        assertFalse("HTTP 500".isTosRequired())
    }

    // --- Extension function: openTaskType ---

    @Test
    fun openTaskTypeReturnsFirstPartBeforeColon() {
        assertEquals("davx5", "davx5:account".openTaskType())
    }

    @Test
    fun openTaskTypeReturnsWholeStringWithNoColon() {
        assertEquals("davx5", "davx5".openTaskType())
    }

    @Test
    fun openTaskTypeReturnsNullForNull() {
        assertEquals(null, (null as String?).openTaskType())
    }

    // --- Data class equality ---

    @Test
    fun dataClassEquality() {
        val a = CaldavAccount(id = 1, uuid = "abc", name = "Test", accountType = TYPE_CALDAV)
        val b = CaldavAccount(id = 1, uuid = "abc", name = "Test", accountType = TYPE_CALDAV)
        assertEquals(a, b)
    }

    @Test
    fun dataClassHashCodeConsistency() {
        val a = CaldavAccount(id = 1, uuid = "abc", name = "Test")
        val b = CaldavAccount(id = 1, uuid = "abc", name = "Test")
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun dataClassInequalityOnDifferentName() {
        val a = CaldavAccount(id = 1, uuid = "abc", name = "A")
        val b = CaldavAccount(id = 1, uuid = "abc", name = "B")
        assertNotEquals(a, b)
    }

    @Test
    fun dataClassInequalityOnDifferentAccountType() {
        val a = CaldavAccount(id = 1, uuid = "abc", accountType = TYPE_CALDAV)
        val b = CaldavAccount(id = 1, uuid = "abc", accountType = TYPE_MICROSOFT)
        assertNotEquals(a, b)
    }

    // --- isTasksOrg with null url ---

    @Test
    fun isNotTasksOrgWhenCaldavWithNullUrl() {
        val account = CaldavAccount(accountType = TYPE_CALDAV, url = null)
        assertFalse(account.isTasksOrg)
    }

    // --- reminderSync for additional server types ---

    @Test
    fun reminderSyncTrueForTasksServer() {
        val account = CaldavAccount(serverType = SERVER_TASKS)
        assertTrue(account.reminderSync)
    }

    @Test
    fun reminderSyncTrueForOpenXchange() {
        val account = CaldavAccount(serverType = SERVER_OPEN_XCHANGE)
        assertTrue(account.reminderSync)
    }

    @Test
    fun reminderSyncTrueForMailboxOrg() {
        val account = CaldavAccount(serverType = SERVER_MAILBOX_ORG)
        assertTrue(account.reminderSync)
    }
}
