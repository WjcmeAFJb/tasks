package org.tasks.timers

import android.app.Application
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SimpleTimeTrackerIntegrationTest {

    private lateinit var app: Application
    private lateinit var integration: SimpleTimeTrackerIntegration

    @Before
    fun setUp() {
        app = RuntimeEnvironment.getApplication()
        integration = SimpleTimeTrackerIntegration(app)
    }

    @Test
    fun startTracking_sendsBroadcastWithStartAction() = runTest {
        integration.startTracking("Coding")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals(SimpleTimeTrackerIntegration.ACTION_START_ACTIVITY, intent.action)
    }

    @Test
    fun startTracking_includesActivityNameExtra() = runTest {
        integration.startTracking("Coding")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals("Coding", intent.getStringExtra(SimpleTimeTrackerIntegration.EXTRA_ACTIVITY_NAME))
    }

    @Test
    fun stopTracking_sendsBroadcastWithStopAction() = runTest {
        integration.stopTracking("Coding")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals(SimpleTimeTrackerIntegration.ACTION_STOP_ACTIVITY, intent.action)
    }

    @Test
    fun stopTracking_includesActivityNameExtra() = runTest {
        integration.stopTracking("Reading")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals("Reading", intent.getStringExtra(SimpleTimeTrackerIntegration.EXTRA_ACTIVITY_NAME))
    }

    @Test
    fun startAction_usesCorrectSttPackage() {
        assertEquals(
            "com.razeeman.util.simpletimetracker.ACTION_START_ACTIVITY",
            SimpleTimeTrackerIntegration.ACTION_START_ACTIVITY
        )
    }

    @Test
    fun stopAction_usesCorrectSttPackage() {
        assertEquals(
            "com.razeeman.util.simpletimetracker.ACTION_STOP_ACTIVITY",
            SimpleTimeTrackerIntegration.ACTION_STOP_ACTIVITY
        )
    }

    @Test
    fun extraActivityName_usesCorrectKey() {
        assertEquals("extra_activity_name", SimpleTimeTrackerIntegration.EXTRA_ACTIVITY_NAME)
    }

    @Test
    fun startTracking_targetsCorrectPackage() = runTest {
        integration.startTracking("Coding")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals("com.razeeman.util.simpletimetracker", intent.`package`)
    }

    @Test
    fun stopTracking_targetsCorrectPackage() = runTest {
        integration.stopTracking("Coding")

        val intent = shadowOf(app).broadcastIntents.last()
        assertEquals("com.razeeman.util.simpletimetracker", intent.`package`)
    }

    @Test
    fun startTracking_sendsExactlyOneBroadcast() = runTest {
        val before = shadowOf(app).broadcastIntents.size
        integration.startTracking("Work")
        assertEquals(before + 1, shadowOf(app).broadcastIntents.size)
    }

    @Test
    fun stopTracking_sendsExactlyOneBroadcast() = runTest {
        val before = shadowOf(app).broadcastIntents.size
        integration.stopTracking("Work")
        assertEquals(before + 1, shadowOf(app).broadcastIntents.size)
    }
}
