package com.vyd.bastion

import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** In-memory log of every button press and its outcome, viewable/copyable from Privacy Center › Diagnostics. */
object Diag {
    private val lines = ArrayDeque<String>()
    private val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized fun log(msg: String) {
        lines.addLast("${fmt.format(Date())}  $msg")
        while (lines.size > 300) lines.removeFirst()
        android.util.Log.d("Bastion", msg)
    }

    @Synchronized fun text(version: String): String =
        "Bastion $version · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MODEL}\n\n" +
            lines.joinToString("\n")
}
