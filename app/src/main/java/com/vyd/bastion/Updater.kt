package com.vyd.bastion

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** Checks update.json in the GitHub repo for a newer APK and installs it through Android's package installer. */
object Updater {
    const val REPO = "Vyd02/Bastion"

    sealed interface State {
        data object Idle : State
        data class Available(val version: String, val apkUrl: String, val notes: String) : State
        data class Downloading(val version: String, val progress: Float) : State
        data class Failed(val msg: String) : State
    }

    val state = MutableStateFlow<State>(State.Idle)
    private var lastCheck = 0L

    private fun parse(v: String) = v.trim().removePrefix("v").split('.', '-').mapNotNull { it.toIntOrNull() }
    private fun newer(remote: String, local: String): Boolean {
        val r = parse(remote); val l = parse(local)
        for (i in 0 until maxOf(r.size, l.size)) {
            val a = r.getOrElse(i) { 0 }; val b = l.getOrElse(i) { 0 }
            if (a != b) return a > b
        }
        return false
    }

    suspend fun check(ctx: Context, force: Boolean = false) {
        if (!force && System.currentTimeMillis() - lastCheck < 60 * 60 * 1000L) return
        if (state.value is State.Downloading) return
        lastCheck = System.currentTimeMillis()
        val local = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "0"
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val c = URL("https://raw.githubusercontent.com/$REPO/main/update.json?t=${System.currentTimeMillis()}")
                    .openConnection() as HttpURLConnection
                c.useCaches = false
                c.connectTimeout = 8000; c.readTimeout = 8000
                if (c.responseCode != 200) return@runCatching null
                val j = JSONObject(c.inputStream.bufferedReader().readText())
                val ver = j.getString("version")
                val apk = j.getString("apk")
                if (newer(ver, local)) State.Available(ver, apk, j.optString("notes")) else null
            }.getOrNull()
        }
        if (result != null) state.value = result
        else if (force) { state.value = State.Idle; toast(ctx, "You're on the latest version ($local)") }
    }

    private fun toast(ctx: Context, m: String) =
        android.widget.Toast.makeText(ctx, m, android.widget.Toast.LENGTH_SHORT).show()

    /** Returns false if the user first needs to allow Bastion to install apps. */
    fun canInstall(ctx: Context) = ctx.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(ctx: Context) {
        ctx.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${ctx.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    suspend fun downloadAndInstall(ctx: Context, a: State.Available) {
        state.value = State.Downloading(a.version, 0f)
        val file = File(ctx.cacheDir, "bastion-update.apk")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                var url = URL(a.apkUrl)
                var c: HttpURLConnection
                var hops = 0
                while (true) { // GitHub redirects to its CDN
                    c = url.openConnection() as HttpURLConnection
                    c.instanceFollowRedirects = false
                    c.connectTimeout = 15000; c.readTimeout = 30000
                    val code = c.responseCode
                    if (code in 300..399 && hops++ < 5) { url = URL(url, c.getHeaderField("Location")); continue }
                    break
                }
                val total = c.contentLengthLong.coerceAtLeast(1)
                c.inputStream.use { input ->
                    file.outputStream().use { out ->
                        val buf = ByteArray(64 * 1024); var done = 0L; var n: Int
                        while (input.read(buf).also { n = it } >= 0) {
                            out.write(buf, 0, n); done += n
                            state.value = State.Downloading(a.version, (done.toFloat() / total).coerceIn(0f, 1f))
                        }
                    }
                }
                true
            }.getOrDefault(false)
        }
        if (!ok) { state.value = State.Failed("Download failed — check your connection"); return }

        withContext(Dispatchers.IO) {
            val pi = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            params.setAppPackageName(ctx.packageName)
            val id = pi.createSession(params)
            pi.openSession(id).use { s ->
                s.openWrite("bastion.apk", 0, file.length()).use { out -> file.inputStream().use { it.copyTo(out) }; s.fsync(out) }
                val intent = Intent(ctx, InstallReceiver::class.java).setAction("com.vyd.bastion.INSTALL_STATUS")
                val pending = PendingIntent.getBroadcast(ctx, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
                s.commit(pending.intentSender)
            }
        }
        state.value = State.Downloading(a.version, 1f)
    }
}

class InstallReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { ctx.startActivity(it) }
            }
            PackageInstaller.STATUS_SUCCESS -> Updater.state.value = Updater.State.Idle
            else -> Updater.state.value = Updater.State.Failed(
                intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "Install cancelled"
            )
        }
    }
}
