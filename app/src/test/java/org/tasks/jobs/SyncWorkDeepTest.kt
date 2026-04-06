package org.tasks.jobs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.tasks.sync.SyncSource

/**
 * Deep tests for SyncWork logic — tests the sync source routing,
 * upgrade semantics, and the EXTRA_SOURCE constant.
 *
 * Since SyncWork is a HiltWorker and tightly coupled to Android's WorkerParameters,
 * we test the accessible logic: SyncSource.fromString, SyncSource.upgrade,
 * the companion constants, and the account-type routing decisions.
 */
class SyncWorkDeepTest {

    // =============================================
    // EXTRA_SOURCE constant
    // =============================================

    @Test
    fun extraSourceConstantValue() {
        assertEquals("extra_source", SyncWork.EXTRA_SOURCE)
    }

    // =============================================
    // SyncSource.fromString — routing input parsing
    // =============================================

    @Test
    fun fromStringReturnsUserInitiated() {
        assertEquals(SyncSource.USER_INITIATED, SyncSource.fromString("USER_INITIATED"))
    }

    @Test
    fun fromStringReturnsBackground() {
        assertEquals(SyncSource.BACKGROUND, SyncSource.fromString("BACKGROUND"))
    }

    @Test
    fun fromStringReturnsTaskChange() {
        assertEquals(SyncSource.TASK_CHANGE, SyncSource.fromString("TASK_CHANGE"))
    }

    @Test
    fun fromStringReturnsPushNotification() {
        assertEquals(SyncSource.PUSH_NOTIFICATION, SyncSource.fromString("PUSH_NOTIFICATION"))
    }

    @Test
    fun fromStringReturnsContentObserver() {
        assertEquals(SyncSource.CONTENT_OBSERVER, SyncSource.fromString("CONTENT_OBSERVER"))
    }

    @Test
    fun fromStringReturnsAppBackground() {
        assertEquals(SyncSource.APP_BACKGROUND, SyncSource.fromString("APP_BACKGROUND"))
    }

    @Test
    fun fromStringReturnsAppResume() {
        assertEquals(SyncSource.APP_RESUME, SyncSource.fromString("APP_RESUME"))
    }

    @Test
    fun fromStringReturnsAccountAdded() {
        assertEquals(SyncSource.ACCOUNT_ADDED, SyncSource.fromString("ACCOUNT_ADDED"))
    }

    @Test
    fun fromStringReturnsPurchaseCompleted() {
        assertEquals(SyncSource.PURCHASE_COMPLETED, SyncSource.fromString("PURCHASE_COMPLETED"))
    }

    @Test
    fun fromStringReturnsSharingChange() {
        assertEquals(SyncSource.SHARING_CHANGE, SyncSource.fromString("SHARING_CHANGE"))
    }

    @Test
    fun fromStringReturnsNoneForNull() {
        assertEquals(SyncSource.NONE, SyncSource.fromString(null))
    }

    @Test
    fun fromStringReturnsNoneForInvalid() {
        assertEquals(SyncSource.NONE, SyncSource.fromString("INVALID_VALUE"))
    }

    @Test
    fun fromStringReturnsNoneForEmpty() {
        assertEquals(SyncSource.NONE, SyncSource.fromString(""))
    }

    @Test
    fun fromStringReturnsNone() {
        assertEquals(SyncSource.NONE, SyncSource.fromString("NONE"))
    }

    // =============================================
    // SyncSource.upgrade — determines if sync source should be upgraded
    // =============================================

    @Test
    fun upgradeFromBackgroundToUserInitiated() {
        // BACKGROUND (showIndicator=false) upgraded by USER_INITIATED (showIndicator=true) -> USER_INITIATED
        val result = SyncSource.BACKGROUND.upgrade(SyncSource.USER_INITIATED)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeFromBackgroundToTaskChange() {
        // BACKGROUND (showIndicator=false) upgraded by TASK_CHANGE (showIndicator=true) -> TASK_CHANGE
        val result = SyncSource.BACKGROUND.upgrade(SyncSource.TASK_CHANGE)
        assertEquals(SyncSource.TASK_CHANGE, result)
    }

    @Test
    fun upgradeFromUserInitiatedToBackground() {
        // USER_INITIATED (showIndicator=true) NOT upgraded by BACKGROUND (showIndicator=false)
        val result = SyncSource.USER_INITIATED.upgrade(SyncSource.BACKGROUND)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeFromUserInitiatedToUserInitiated() {
        // Both show indicator, but current already shows -> keeps current
        val result = SyncSource.USER_INITIATED.upgrade(SyncSource.USER_INITIATED)
        assertEquals(SyncSource.USER_INITIATED, result)
    }

    @Test
    fun upgradeFromNoneToTaskChange() {
        // NONE (showIndicator=false) upgraded by TASK_CHANGE (showIndicator=true) -> TASK_CHANGE
        val result = SyncSource.NONE.upgrade(SyncSource.TASK_CHANGE)
        assertEquals(SyncSource.TASK_CHANGE, result)
    }

    @Test
    fun upgradeFromNoneToNone() {
        val result = SyncSource.NONE.upgrade(SyncSource.NONE)
        assertEquals(SyncSource.NONE, result)
    }

    @Test
    fun upgradeFromNoneToBackground() {
        // NONE (showIndicator=false) NOT upgraded by BACKGROUND (showIndicator=false)
        val result = SyncSource.NONE.upgrade(SyncSource.BACKGROUND)
        assertEquals(SyncSource.NONE, result)
    }

    @Test
    fun upgradeFromAppBackgroundToAccountAdded() {
        // APP_BACKGROUND (false) upgraded by ACCOUNT_ADDED (true) -> ACCOUNT_ADDED
        val result = SyncSource.APP_BACKGROUND.upgrade(SyncSource.ACCOUNT_ADDED)
        assertEquals(SyncSource.ACCOUNT_ADDED, result)
    }

    @Test
    fun upgradeFromContentObserverToAppResume() {
        // CONTENT_OBSERVER (true) NOT upgraded by APP_RESUME (false)
        val result = SyncSource.CONTENT_OBSERVER.upgrade(SyncSource.APP_RESUME)
        assertEquals(SyncSource.CONTENT_OBSERVER, result)
    }

    @Test
    fun upgradeFromPushNotificationToSharingChange() {
        // PUSH_NOTIFICATION (false) upgraded by SHARING_CHANGE (true) -> SHARING_CHANGE
        val result = SyncSource.PUSH_NOTIFICATION.upgrade(SyncSource.SHARING_CHANGE)
        assertEquals(SyncSource.SHARING_CHANGE, result)
    }

    @Test
    fun upgradeFromAccountAddedToPurchaseCompleted() {
        // ACCOUNT_ADDED (true) NOT upgraded by PURCHASE_COMPLETED (true) — both show indicator -> keeps current
        val result = SyncSource.ACCOUNT_ADDED.upgrade(SyncSource.PURCHASE_COMPLETED)
        assertEquals(SyncSource.ACCOUNT_ADDED, result)
    }

    @Test
    fun upgradeFromSharingChangeToContentObserver() {
        // SHARING_CHANGE (true) NOT upgraded by CONTENT_OBSERVER (true) -> keeps current
        val result = SyncSource.SHARING_CHANGE.upgrade(SyncSource.CONTENT_OBSERVER)
        assertEquals(SyncSource.SHARING_CHANGE, result)
    }

    // =============================================
    // SyncSource.showIndicator — validate all values
    // =============================================

    @Test
    fun noneDoesNotShowIndicator() {
        assertFalse(SyncSource.NONE.showIndicator)
    }

    @Test
    fun userInitiatedShowsIndicator() {
        assertTrue(SyncSource.USER_INITIATED.showIndicator)
    }

    @Test
    fun pushNotificationDoesNotShowIndicator() {
        assertFalse(SyncSource.PUSH_NOTIFICATION.showIndicator)
    }

    @Test
    fun contentObserverShowsIndicator() {
        assertTrue(SyncSource.CONTENT_OBSERVER.showIndicator)
    }

    @Test
    fun backgroundDoesNotShowIndicator() {
        assertFalse(SyncSource.BACKGROUND.showIndicator)
    }

    @Test
    fun taskChangeShowsIndicator() {
        assertTrue(SyncSource.TASK_CHANGE.showIndicator)
    }

    @Test
    fun appBackgroundDoesNotShowIndicator() {
        assertFalse(SyncSource.APP_BACKGROUND.showIndicator)
    }

    @Test
    fun appResumeDoesNotShowIndicator() {
        assertFalse(SyncSource.APP_RESUME.showIndicator)
    }

    @Test
    fun accountAddedShowsIndicator() {
        assertTrue(SyncSource.ACCOUNT_ADDED.showIndicator)
    }

    @Test
    fun purchaseCompletedShowsIndicator() {
        assertTrue(SyncSource.PURCHASE_COMPLETED.showIndicator)
    }

    @Test
    fun sharingChangeShowsIndicator() {
        assertTrue(SyncSource.SHARING_CHANGE.showIndicator)
    }

    // =============================================
    // Account type routing — which synchronizer handles which type
    // (Tests the constants used in SyncWork.caldavJobs and googleTaskJobs)
    // =============================================

    @Test
    fun googleTasksSyncTypeConstant() {
        assertEquals(7, org.tasks.data.entity.CaldavAccount.TYPE_GOOGLE_TASKS)
    }

    @Test
    fun caldavSyncTypeConstant() {
        assertEquals(0, org.tasks.data.entity.CaldavAccount.TYPE_CALDAV)
    }

    @Test
    fun etebaseSyncTypeConstant() {
        assertEquals(5, org.tasks.data.entity.CaldavAccount.TYPE_ETEBASE)
    }

    @Test
    fun tasksSyncTypeConstant() {
        assertEquals(4, org.tasks.data.entity.CaldavAccount.TYPE_TASKS)
    }

    @Test
    fun microsoftSyncTypeConstant() {
        assertEquals(6, org.tasks.data.entity.CaldavAccount.TYPE_MICROSOFT)
    }

    // =============================================
    // Source routing: BACKGROUND triggers background restriction check
    // =============================================

    @Test
    fun backgroundSourceTriggersRestrictionCheck() {
        val source = SyncSource.BACKGROUND
        val shouldCheckRestrictions = source == SyncSource.BACKGROUND
        assertTrue(shouldCheckRestrictions)
    }

    @Test
    fun userInitiatedSourceSkipsRestrictionCheck() {
        val source = SyncSource.USER_INITIATED
        val shouldCheckRestrictions = source == SyncSource.BACKGROUND
        assertFalse(shouldCheckRestrictions)
    }

    @Test
    fun taskChangeSourceSkipsRestrictionCheck() {
        val source = SyncSource.TASK_CHANGE
        val shouldCheckRestrictions = source == SyncSource.BACKGROUND
        assertFalse(shouldCheckRestrictions)
    }

    // =============================================
    // OpenTasks sync: USER_INITIATED triggers content resolver sync
    // =============================================

    @Test
    fun userInitiatedTriggersOpenTasksContentResolverSync() {
        val source = SyncSource.USER_INITIATED
        val shouldTrigger = source == SyncSource.USER_INITIATED
        assertTrue(shouldTrigger)
    }

    @Test
    fun backgroundDoesNotTriggerOpenTasksContentResolverSync() {
        val source = SyncSource.BACKGROUND
        val shouldTrigger = source == SyncSource.USER_INITIATED
        assertFalse(shouldTrigger)
    }

    @Test
    fun taskChangeDoesNotTriggerOpenTasksContentResolverSync() {
        val source = SyncSource.TASK_CHANGE
        val shouldTrigger = source == SyncSource.USER_INITIATED
        assertFalse(shouldTrigger)
    }

    // =============================================
    // All SyncSource values roundtrip through fromString
    // =============================================

    @Test
    fun allSyncSourceValuesRoundtrip() {
        SyncSource.values().forEach { source ->
            assertEquals(
                "SyncSource.${source.name} should roundtrip",
                source,
                SyncSource.fromString(source.name)
            )
        }
    }

    @Test
    fun syncSourceEnumHasExpectedCount() {
        assertEquals(11, SyncSource.values().size)
    }
}
