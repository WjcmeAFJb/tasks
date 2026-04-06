package org.tasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.tasks.data.entity.CaldavAccount
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.ACCOUNT_TYPE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DAVX5_MANAGED
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_DECSYNC
import org.tasks.data.entity.CaldavAccount.Companion.PACKAGE_ETESYNC
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_CALDAV
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_GOOGLE_TASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_OPENTASKS
import org.tasks.data.entity.CaldavAccount.Companion.TYPE_TASKS

class CaldavAccountExtensionsTest {

    // --- openTaskApp: DAVx5 ---

    @Test
    fun openTaskAppForDavx5() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5:account"
        )
        val app = account.openTaskApp
        assertNotNull(app)
        assertEquals("DAVx\u2075", app!!.name)
        assertEquals(PACKAGE_DAVX5, app.packageName)
    }

    @Test
    fun openTaskAppForDavx5Managed() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5_MANAGED:account"
        )
        val app = account.openTaskApp
        assertNotNull(app)
        assertEquals("DAVx\u2075", app!!.name)
        assertEquals(PACKAGE_DAVX5_MANAGED, app.packageName)
    }

    // --- openTaskApp: EteSync ---

    @Test
    fun openTaskAppForEteSync() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_ETESYNC:account"
        )
        val app = account.openTaskApp
        assertNotNull(app)
        assertEquals("EteSync", app!!.name)
        assertEquals(PACKAGE_ETESYNC, app.packageName)
    }

    // --- openTaskApp: DecSync ---

    @Test
    fun openTaskAppForDecSync() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DECSYNC:account"
        )
        val app = account.openTaskApp
        assertNotNull(app)
        assertEquals("DecSync CC", app!!.name)
        assertEquals(PACKAGE_DECSYNC, app.packageName)
    }

    // --- openTaskApp: null cases ---

    @Test
    fun openTaskAppNullForTasksOrg() {
        val account = CaldavAccount(accountType = TYPE_TASKS)
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForCaldav() {
        val account = CaldavAccount(accountType = TYPE_CALDAV)
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForGoogleTasks() {
        val account = CaldavAccount(accountType = TYPE_GOOGLE_TASKS)
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForUnknownUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "unknown:account"
        )
        assertNull(account.openTaskApp)
    }

    @Test
    fun openTaskAppNullForNullUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = null
        )
        assertNull(account.openTaskApp)
    }

    // --- OpenTaskApp data class ---

    @Test
    fun openTaskAppEquality() {
        val app1 = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        val app2 = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        assertEquals(app1, app2)
    }

    @Test
    fun openTaskAppInequalityByName() {
        val app1 = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        val app2 = OpenTaskApp("EteSync", PACKAGE_DAVX5)
        assertNotEquals(app1, app2)
    }

    @Test
    fun openTaskAppInequalityByPackage() {
        val app1 = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        val app2 = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5_MANAGED)
        assertNotEquals(app1, app2)
    }

    @Test
    fun openTaskAppHashCodeConsistency() {
        val app1 = OpenTaskApp("EteSync", PACKAGE_ETESYNC)
        val app2 = OpenTaskApp("EteSync", PACKAGE_ETESYNC)
        assertEquals(app1.hashCode(), app2.hashCode())
    }

    @Test
    fun openTaskAppCopy() {
        val app = OpenTaskApp("DAVx\u2075", PACKAGE_DAVX5)
        val copy = app.copy(name = "Modified")
        assertEquals("Modified", copy.name)
        assertEquals(PACKAGE_DAVX5, copy.packageName)
    }

    // --- OpenTaskApp: destructuring ---

    @Test
    fun openTaskAppDestructuring() {
        val app = OpenTaskApp("EteSync", PACKAGE_ETESYNC)
        val (name, pkg) = app
        assertEquals("EteSync", name)
        assertEquals(PACKAGE_ETESYNC, pkg)
    }

    // --- openTaskApp: prefix matching with various suffixes ---

    @Test
    fun openTaskAppMatchesDavx5WithEmailSuffix() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5:some_user@example.com"
        )
        assertNotNull(account.openTaskApp)
        assertEquals(PACKAGE_DAVX5, account.openTaskApp!!.packageName)
    }

    @Test
    fun openTaskAppMatchesDavx5ManagedWithEmailSuffix() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DAVX5_MANAGED:some_user@example.com"
        )
        assertNotNull(account.openTaskApp)
        assertEquals(PACKAGE_DAVX5_MANAGED, account.openTaskApp!!.packageName)
    }

    @Test
    fun openTaskAppMatchesEteSyncWithEmailSuffix() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_ETESYNC:user@example.com"
        )
        assertNotNull(account.openTaskApp)
        assertEquals(PACKAGE_ETESYNC, account.openTaskApp!!.packageName)
    }

    @Test
    fun openTaskAppMatchesDecSyncWithCalendarSuffix() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = "$ACCOUNT_TYPE_DECSYNC:my_calendar"
        )
        assertNotNull(account.openTaskApp)
        assertEquals(PACKAGE_DECSYNC, account.openTaskApp!!.packageName)
    }

    @Test
    fun openTaskAppNullForEmptyUuid() {
        val account = CaldavAccount(
            accountType = TYPE_OPENTASKS,
            uuid = ""
        )
        assertNull(account.openTaskApp)
    }
}
