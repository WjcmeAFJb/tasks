package org.tasks.coverage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * Debug-only receiver that dumps JaCoCo execution data to a file on the device.
 *
 * Trigger via adb:
 *   adb shell am broadcast -a org.tasks.DUMP_COVERAGE
 *
 * Pull the file:
 *   adb pull /data/data/org.tasks/files/coverage.ec
 *
 * This is only included in debug builds and is a no-op if JaCoCo is not active
 * (i.e. the APK was not built with -Pcoverage).
 */
class CoverageBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val outFile = File(context.filesDir, "coverage.ec")
        try {
            val agent = Class.forName("org.jacoco.agent.rt.RT")
                .getMethod("getAgent")
                .invoke(null)
            val data = agent.javaClass
                .getMethod("getExecutionData", Boolean::class.java)
                .invoke(agent, false) as ByteArray
            outFile.writeBytes(data)
            Log.i(TAG, "Coverage dumped to ${outFile.absolutePath} (${data.size} bytes)")
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "JaCoCo agent not present — was the APK built with -Pcoverage?")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dump coverage", e)
        }
    }

    companion object {
        private const val TAG = "CoverageDump"
    }
}
