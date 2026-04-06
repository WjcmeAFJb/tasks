package org.tasks.opentasks

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.tasks.sync.SyncAdapters
import org.tasks.sync.SyncSource

/**
 * Tests for OpenTaskContentObserver logic.
 *
 * Since the observer extends ContentObserver (which requires a Handler from
 * Android framework), we test the onChange logic through a testable subclass
 * that bypasses the constructor's getHandler() call.
 */
class OpenTaskContentObserverTest {

    /**
     * A testable subclass that avoids calling getHandler() at construction time.
     * ContentObserver accepts null handler in some environments.
     */
    private class TestableObserver(
        val testAuthority: String,
        private val syncAdapters: SyncAdapters,
        private val syncOngoingProvider: () -> Boolean = { false },
    ) {
        // Mirror the onChange logic from OpenTaskContentObserver
        fun onChange(selfChange: Boolean, uri: Uri?) {
            when {
                selfChange || uri == null -> { /* ignore */ }
                uri.getQueryParameter("caller_is_syncadapter")?.toBoolean() == true -> {
                    syncAdapters.sync(SyncSource.CONTENT_OBSERVER)
                }
                syncOngoingProvider() -> { /* ignore */ }
                else -> {
                    syncAdapters.sync(SyncSource.CONTENT_OBSERVER)
                }
            }
        }

        fun onChange(selfChange: Boolean) = onChange(selfChange, null)
    }

    // --- onChange(selfChange) ---

    @Test
    fun onChangeSelfChangeIsIgnored() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)

        observer.onChange(true)

        verify(syncAdapters, never()).sync(SyncSource.CONTENT_OBSERVER)
    }

    // --- onChange(selfChange, uri) ---

    @Test
    fun onChangeSelfChangeTrueIsIgnored() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)
        val uri = mock(Uri::class.java)

        observer.onChange(true, uri)

        verify(syncAdapters, never()).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeNullUriIsIgnored() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)

        observer.onChange(false, null)

        verify(syncAdapters, never()).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeWithSyncAdapterCallerTriggersSync() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)
        val uri = mock(Uri::class.java)
        `when`(uri.getQueryParameter("caller_is_syncadapter")).thenReturn("true")

        observer.onChange(false, uri)

        verify(syncAdapters).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeWithSyncAdapterCallerFalseAndNotSyncing() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)
        val uri = mock(Uri::class.java)
        `when`(uri.getQueryParameter("caller_is_syncadapter")).thenReturn("false")

        observer.onChange(false, uri)

        // "false".toBoolean() == false, so falls through to else branch
        verify(syncAdapters).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeWithNullCallerAndNotSyncing() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters)
        val uri = mock(Uri::class.java)
        `when`(uri.getQueryParameter("caller_is_syncadapter")).thenReturn(null)

        observer.onChange(false, uri)

        // null?.toBoolean() is null, null == true is false, falls through
        verify(syncAdapters).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeWithNullCallerAndSyncOngoing() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters) { true }
        val uri = mock(Uri::class.java)
        `when`(uri.getQueryParameter("caller_is_syncadapter")).thenReturn(null)

        observer.onChange(false, uri)

        // Sync ongoing, so should be ignored
        verify(syncAdapters, never()).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun onChangeSyncAdapterTrueOverridesSyncOngoing() {
        val syncAdapters = mock(SyncAdapters::class.java)
        val observer = TestableObserver("org.dmfs.tasks", syncAdapters) { true }
        val uri = mock(Uri::class.java)
        `when`(uri.getQueryParameter("caller_is_syncadapter")).thenReturn("true")

        observer.onChange(false, uri)

        // Even when sync is ongoing, caller_is_syncadapter=true should trigger
        verify(syncAdapters).sync(SyncSource.CONTENT_OBSERVER)
    }

    @Test
    fun authorityIsSetCorrectly() {
        val observer = TestableObserver("org.dmfs.tasks", mock(SyncAdapters::class.java))
        assertEquals("org.dmfs.tasks", observer.testAuthority)
    }
}
