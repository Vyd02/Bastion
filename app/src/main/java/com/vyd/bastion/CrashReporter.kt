package com.vyd.bastion

import android.content.Context
import android.os.Build
import java.io.File

/** Saves any crash to a file so the next launch can show it instead of looping. */
object CrashReporter {
    private var installed = false
    private fun file(c: Context) = File(c.filesDir, "last_crash.txt")

    fun install(ctx: Context) {
        if (installed) return
        installed = true
        val prev = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            runCatching {
                file(ctx).writeText(
                    "Bastion ${BuildConfigLite.VERSION} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MODEL}\n" +
                        "Thread: ${t.name}\n\n" + e.stackTraceToString()
                )
            }
            prev?.uncaughtException(t, e)
        }
    }

    fun pending(c: Context): String? = file(c).takeIf { it.exists() }?.readText()
    fun clear(c: Context) { file(c).delete() }
}

object BuildConfigLite { const val VERSION = "1.7" }
