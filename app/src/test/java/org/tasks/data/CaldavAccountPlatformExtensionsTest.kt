package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import org.tasks.R
import org.tasks.caldav.CaldavAccountSettingsActivity
import org.tasks.caldav.CaldavCalendarSettingsActivity
import org.tasks.caldav.LocalAccountSettingsActivity
import org.tasks.caldav.LocalListSettingsActivity
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_ETEBASE
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_LOCAL
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_MICROSOFT
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS
import org.tasks.etebase.EtebaseAccountSettingsActivity
import org.tasks.etebase.EtebaseCalendarSettingsActivity
import org.tasks.opentasks.OpenTaskAccountSettingsActivity
import org.tasks.opentasks.OpenTasksListSettingsActivity
import org.tasks.sync.microsoft.MicrosoftListSettingsActivity
import org.tasks.activities.GoogleTaskListSettingsActivity

class CaldavAccountPlatformExtensionsTest {

    // --- prefTitle ---

    @Test
    fun prefTitleForTasksOrg() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertEquals(R.string.tasks_org, account.prefTitle)
    }

    @Test
    fun prefTitleForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV, url = "https://example.com/dav/")
        assertEquals(R.string.caldav, account.prefTitle)
    }

    @Test
    fun prefTitleForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertEquals(R.string.etesync, account.prefTitle)
    }

    @Test
    fun prefTitleForDavx5() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5:account"
        )
        assertEquals(R.string.davx5, account.prefTitle)
    }

    @Test
    fun prefTitleForDavx5Managed() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5_MANAGED:account"
        )
        assertEquals(R.string.davx5, account.prefTitle)
    }

    @Test
    fun prefTitleForDecSync() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DECSYNC:account"
        )
        assertEquals(R.string.decsync, account.prefTitle)
    }

    @Test
    fun prefTitleForEteSyncUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_ETESYNC:account"
        )
        assertEquals(R.string.etesync, account.prefTitle)
    }

    @Test
    fun prefTitleForMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertEquals(R.string.microsoft, account.prefTitle)
    }

    @Test
    fun prefTitleForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertEquals(R.string.gtasks_GPr_header, account.prefTitle)
    }

    @Test
    fun prefTitleForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertEquals(R.string.local_lists, account.prefTitle)
    }

    // --- prefIcon ---

    @Test
    fun prefIconForTasksOrg() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertEquals(R.drawable.ic_round_icon, account.prefIcon)
    }

    @Test
    fun prefIconForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV, url = "https://example.com/dav/")
        assertEquals(R.drawable.ic_webdav_logo, account.prefIcon)
    }

    @Test
    fun prefIconForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertEquals(R.drawable.ic_etesync, account.prefIcon)
    }

    @Test
    fun prefIconForDavx5() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5:account"
        )
        assertEquals(R.drawable.ic_davx5_icon_green_bg, account.prefIcon)
    }

    @Test
    fun prefIconForDavx5Managed() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5_MANAGED:account"
        )
        assertEquals(R.drawable.ic_davx5_icon_blue_bg, account.prefIcon)
    }

    @Test
    fun prefIconForDecSync() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DECSYNC:account"
        )
        assertEquals(R.drawable.ic_decsync, account.prefIcon)
    }

    @Test
    fun prefIconForMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertEquals(R.drawable.ic_microsoft_tasks, account.prefIcon)
    }

    @Test
    fun prefIconForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertEquals(R.drawable.ic_google, account.prefIcon)
    }

    @Test
    fun prefIconForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertEquals(R.drawable.ic_outline_cloud_off_24px, account.prefIcon)
    }

    // --- listSettingsClass ---

    @Test
    fun listSettingsClassForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertEquals(LocalListSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForOpenTasks() {
        val account = CaldavAccount(accountType = TYPE_OPENTASKS)
        assertEquals(OpenTasksListSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertEquals(EtebaseCalendarSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForMicrosoft() {
        val account = CaldavAccount(accountType = TYPE_MICROSOFT)
        assertEquals(MicrosoftListSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertEquals(GoogleTaskListSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertEquals(CaldavCalendarSettingsActivity::class.java, account.listSettingsClass())
    }

    @Test
    fun listSettingsClassForTasks() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertEquals(CaldavCalendarSettingsActivity::class.java, account.listSettingsClass())
    }

    // --- accountSettingsClass ---

    @Test
    fun accountSettingsClassForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV, url = "https://example.com/dav/")
        assertEquals(CaldavAccountSettingsActivity::class.java, account.accountSettingsClass)
    }

    @Test
    fun accountSettingsClassForEtebase() {
        val account = CaldavAccount(accountType = TYPE_ETEBASE)
        assertEquals(EtebaseAccountSettingsActivity::class.java, account.accountSettingsClass)
    }

    @Test
    fun accountSettingsClassForOpenTasks() {
        val account = CaldavAccount(accountType = TYPE_OPENTASKS)
        assertEquals(OpenTaskAccountSettingsActivity::class.java, account.accountSettingsClass)
    }

    @Test
    fun accountSettingsClassForLocal() {
        val account = CaldavAccount(accountType = TYPE_LOCAL)
        assertEquals(LocalAccountSettingsActivity::class.java, account.accountSettingsClass)
    }

    @Test(expected = IllegalArgumentException::class)
    fun accountSettingsClassThrowsForUnsupportedType() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        account.accountSettingsClass
    }

    // --- isTasksSubscription ---
    // Note: isTasksSubscription requires Context, but we can test partial logic
    // through the underlying isTasksOrg + isPaymentRequired + isLoggedOut

    @Test
    fun isNotTasksSubscriptionWhenPaymentRequired() {
        val account = CaldavAccount(
            accountType = TYPE_TASKS,
            error = CaldavAccount.ERROR_PAYMENT_REQUIRED,
        )
        // isTasksOrg = true, isPaymentRequired = true => not a subscription
        assertEquals(true, account.isTasksOrg)
        assertEquals(true, account.isPaymentRequired())
    }

    @Test
    fun isNotTasksSubscriptionWhenLoggedOut() {
        val account = CaldavAccount(
            accountType = TYPE_TASKS,
            error = CaldavAccount.ERROR_UNAUTHORIZED,
        )
        assertEquals(true, account.isTasksOrg)
        assertEquals(true, account.isLoggedOut())
    }
}
