package com.droidspaces.app.util

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.topjohnwu.superuser.Shell
import java.io.File

/** The Anland 5.x consumer app, which shows a container's Wayland desktop. */
object AnlandUtils {

    private const val CONSUMER_PACKAGE = "com.anland.consumer"
    private const val CONSUMER_ACTIVITY = "com.anland.consumer.SecondaryActivity"
    // Matches MainActivity.EXTRA_SOCKET_PATH / EXTRA_WINDOW_NAME in the consumer app.
    private const val EXTRA_SOCKET_PATH = "socket_path"
    private const val EXTRA_WINDOW_NAME = "window_name"

    /** Reads the broker socket recorded at Pids/<name>.asock. */
    fun findSocketPath(containerName: String): String? {
        val file = File("/data/local/Droidspaces/Pids/$containerName.asock")
        if (file.exists()) {
            try {
                val sock = file.readText().trim()
                if (sock.isNotEmpty()) return sock
            } catch (_: Exception) {}
        }
        return try {
            val out = Shell.cmd("cat /data/local/Droidspaces/Pids/${containerName}.asock").exec().out
            out.firstOrNull()?.trim()?.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Opens the consumer's window for this container's display socket, titled with
     * the container name. Every launch is its own task; the consumer dedups by
     * socket path on its side. Returns false when the consumer app is missing or
     * refuses the intent, so the caller can say so.
     */
    fun launchWindow(context: Context, containerName: String, socketPath: String): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            component = ComponentName(CONSUMER_PACKAGE, CONSUMER_ACTIVITY)
            putExtra(EXTRA_SOCKET_PATH, socketPath)
            putExtra(EXTRA_WINDOW_NAME, containerName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }
}
