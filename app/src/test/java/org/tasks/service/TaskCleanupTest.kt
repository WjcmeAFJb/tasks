package org.tasks.service

import kotlinx.coroutines.test.runTest
import org.junit.Test

class TaskCleanupTest {

    private class TestCleanup : TaskCleanup

    private class TrackingCleanup : TaskCleanup {
        var cleanupCalled = false
        var cleanupTasks: List<Long> = emptyList()
        var onMarkedDeletedCalled = false

        override suspend fun cleanup(tasks: List<Long>) {
            cleanupCalled = true
            cleanupTasks = tasks
        }

        override suspend fun onMarkedDeleted() {
            onMarkedDeletedCalled = true
        }
    }

    @Test
    fun defaultCleanupDoesNotThrow() = runTest {
        val cleanup: TaskCleanup = TestCleanup()
        cleanup.cleanup(listOf(1L, 2L, 3L))
    }

    @Test
    fun defaultOnMarkedDeletedDoesNotThrow() = runTest {
        val cleanup: TaskCleanup = TestCleanup()
        cleanup.onMarkedDeleted()
    }

    @Test
    fun defaultCleanupAcceptsEmptyList() = runTest {
        val cleanup: TaskCleanup = TestCleanup()
        cleanup.cleanup(emptyList())
    }

    @Test
    fun overriddenCleanupReceivesTasks() = runTest {
        val cleanup = TrackingCleanup()
        val tasks = listOf(10L, 20L, 30L)
        cleanup.cleanup(tasks)
        assert(cleanup.cleanupCalled)
        assert(cleanup.cleanupTasks == tasks)
    }

    @Test
    fun overriddenOnMarkedDeletedIsCalled() = runTest {
        val cleanup = TrackingCleanup()
        cleanup.onMarkedDeleted()
        assert(cleanup.onMarkedDeletedCalled)
    }

    @Test
    fun defaultCleanupWithSingleTask() = runTest {
        val cleanup: TaskCleanup = TestCleanup()
        cleanup.cleanup(listOf(42L))
    }

    @Test
    fun defaultCleanupWithLargeList() = runTest {
        val cleanup: TaskCleanup = TestCleanup()
        cleanup.cleanup((1L..1000L).toList())
    }

    @Test
    fun overriddenCleanupReceivesSingleTask() = runTest {
        val cleanup = TrackingCleanup()
        cleanup.cleanup(listOf(99L))
        assert(cleanup.cleanupTasks == listOf(99L))
    }

    @Test
    fun overriddenMethodsStartUnCalled() = runTest {
        val cleanup = TrackingCleanup()
        assert(!cleanup.cleanupCalled)
        assert(!cleanup.onMarkedDeletedCalled)
        assert(cleanup.cleanupTasks.isEmpty())
    }

    @Test
    fun cleanupAndOnMarkedDeletedAreIndependent() = runTest {
        val cleanup = TrackingCleanup()
        cleanup.cleanup(listOf(1L))
        assert(cleanup.cleanupCalled)
        assert(!cleanup.onMarkedDeletedCalled)
    }

    @Test
    fun onMarkedDeletedDoesNotAffectCleanup() = runTest {
        val cleanup = TrackingCleanup()
        cleanup.onMarkedDeleted()
        assert(!cleanup.cleanupCalled)
        assert(cleanup.onMarkedDeletedCalled)
    }
}
