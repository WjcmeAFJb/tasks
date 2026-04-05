package org.tasks.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSourceTest {
    @Test fun noneDoesNotShowIndicator() = assertFalse(SyncSource.NONE.showIndicator)
    @Test fun userInitiatedShowsIndicator() = assertTrue(SyncSource.USER_INITIATED.showIndicator)
    @Test fun pushNotificationDoesNotShow() = assertFalse(SyncSource.PUSH_NOTIFICATION.showIndicator)
    @Test fun contentObserverShows() = assertTrue(SyncSource.CONTENT_OBSERVER.showIndicator)
    @Test fun backgroundDoesNotShow() = assertFalse(SyncSource.BACKGROUND.showIndicator)
    @Test fun taskChangeShows() = assertTrue(SyncSource.TASK_CHANGE.showIndicator)
    @Test fun appBackgroundDoesNotShow() = assertFalse(SyncSource.APP_BACKGROUND.showIndicator)
    @Test fun appResumeDoesNotShow() = assertFalse(SyncSource.APP_RESUME.showIndicator)
    @Test fun accountAddedShows() = assertTrue(SyncSource.ACCOUNT_ADDED.showIndicator)
    @Test fun purchaseCompletedShows() = assertTrue(SyncSource.PURCHASE_COMPLETED.showIndicator)
    @Test fun sharingChangeShows() = assertTrue(SyncSource.SHARING_CHANGE.showIndicator)

    // upgrade logic
    @Test fun upgradeFromNoneToUserInitiated() =
        assertEquals(SyncSource.USER_INITIATED, SyncSource.NONE.upgrade(SyncSource.USER_INITIATED))

    @Test fun upgradeFromUserInitiatedToNone() =
        assertEquals(SyncSource.USER_INITIATED, SyncSource.USER_INITIATED.upgrade(SyncSource.NONE))

    @Test fun upgradeFromNoneToBackground() =
        assertEquals(SyncSource.NONE, SyncSource.NONE.upgrade(SyncSource.BACKGROUND))

    @Test fun upgradeFromBackgroundToTaskChange() =
        assertEquals(SyncSource.TASK_CHANGE, SyncSource.BACKGROUND.upgrade(SyncSource.TASK_CHANGE))

    @Test fun upgradeFromTaskChangeToBackground() =
        assertEquals(SyncSource.TASK_CHANGE, SyncSource.TASK_CHANGE.upgrade(SyncSource.BACKGROUND))

    @Test fun upgradeKeepsHigherIndicator() =
        assertEquals(SyncSource.USER_INITIATED, SyncSource.USER_INITIATED.upgrade(SyncSource.TASK_CHANGE))

    // fromString
    @Test fun fromStringValid() = assertEquals(SyncSource.USER_INITIATED, SyncSource.fromString("USER_INITIATED"))
    @Test fun fromStringNull() = assertEquals(SyncSource.NONE, SyncSource.fromString(null))
    @Test fun fromStringInvalid() = assertEquals(SyncSource.NONE, SyncSource.fromString("INVALID"))
    @Test fun fromStringEmpty() = assertEquals(SyncSource.NONE, SyncSource.fromString(""))
    @Test fun fromStringAllValues() {
        SyncSource.values().forEach {
            assertEquals(it, SyncSource.fromString(it.name))
        }
    }
}
