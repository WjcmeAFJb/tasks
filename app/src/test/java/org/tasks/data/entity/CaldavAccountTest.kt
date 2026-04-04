package org.tasks.data.entity

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_PAYMENT_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_TOS_REQUIRED
import org.tasks.data.entity.CaldavAccount.Companion.ERROR_UNAUTHORIZED
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_MAILBOX_ORG
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_NEXTCLOUD
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OPEN_XCHANGE
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_OTHER
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_SYNOLOGY_CALENDAR
import org.tasks.data.entity.CaldavAccount.Companion.SERVER_UNKNOWN
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS

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
}
