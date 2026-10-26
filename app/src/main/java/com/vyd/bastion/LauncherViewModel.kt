package com.vyd.bastion

import android.app.Application
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.UserHandle
import android.os.UserManager
import android.provider.Settings as SysSettings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class LauncherViewModel(private val app: Application) : AndroidViewModel(app) {
    val repo = AppRepository(app, viewModelScope)
    val prefs = Prefs(app)

    /** Bumped whenever the Home button is pressed while we're already in front. */
    val homeSignal = MutableStateFlow(0)
    val vaultOpen = MutableStateFlow(false)
    val privateOpen = MutableStateFlow(false)
    val isDefaultHome = MutableStateFlow(false)

    private val la = app.getSystemService(LauncherApps::class.java)
    private val um = app.getSystemService(UserManager::class.java)

    init { repo.refresh() }
    override fun onCleared() { repo.close() }

    fun onResume() {
        repo.refresh()
        isDefaultHome.value = runCatching {
            app.getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_HOME)
        }.getOrDefault(false)
    }

    fun goHome() { homeSignal.value++ }

    fun onScreenOff() {
        vaultOpen.value = false
        privateOpen.value = false
        if (prefs.settings.value.autoLock) setPrivateLocked(true, silent = true)
    }

    fun toast(msg: String) = Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()

    // ---------------- Authentication ----------------
    /** System fingerprint / PIN prompt (framework API — no fragments involved). */
    fun authenticate(act: FragmentActivity, title: String, onOk: () -> Unit) {
        val auth = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
            android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val bm = act.getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        if (bm == null || bm.canAuthenticate(auth) != android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS) {
            toast("No screen lock set — add a PIN or fingerprint in Settings to protect this")
            onOk(); return
        }
        runCatching {
            android.hardware.biometrics.BiometricPrompt.Builder(act)
                .setTitle(title)
                .setSubtitle("Bastion privacy check")
                .setAllowedAuthenticators(auth)
                .build()
                .authenticate(android.os.CancellationSignal(), act.mainExecutor,
                    object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                        override fun onAuthenticationSucceeded(r: android.hardware.biometrics.BiometricPrompt.AuthenticationResult?) = onOk()
                    })
        }.onFailure { toast("Couldn't show the unlock prompt: ${it.message}") }
    }

    // ---------------- Launching ----------------
    fun launch(act: FragmentActivity, e: AppEntry, bounds: Rect? = null) {
        val go = {
            runCatching { la.startMainActivity(e.component, e.user, bounds, null) }
                .onFailure { toast("Couldn't open ${e.label}") }
            Unit
        }
        if (e.key in prefs.settings.value.locked) authenticate(act, "Unlock ${e.label}", go) else go()
    }

    fun appInfo(e: AppEntry) = runCatching { la.startAppDetailsActivity(e.component, e.user, null, null) }

    fun uninstall(act: FragmentActivity, e: AppEntry) {
        if (e.profile == ProfileKind.MAIN) {
            runCatching {
                act.startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${e.packageName}")))
            }.onFailure { appInfo(e) }
        } else appInfo(e)
    }

    fun openStore(act: FragmentActivity, inPrivate: Boolean) {
        if (inPrivate) {
            val store = repo.state.value.privateSpace?.apps?.firstOrNull { it.packageName == "com.android.vending" }
            if (store != null) { launch(act, store); return }
            toast("Play Store isn't in your Private Space")
            return
        }
        val i = act.packageManager.getLaunchIntentForPackage("com.android.vending")
        try { act.startActivity(i ?: Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q="))) }
        catch (_: ActivityNotFoundException) { toast("Play Store not found") }
    }

    fun openHomeSettings(act: FragmentActivity) {
        runCatching { act.startActivity(Intent(SysSettings.ACTION_HOME_SETTINGS)) }
            .onFailure { act.startActivity(Intent(SysSettings.ACTION_SETTINGS)) }
    }

    fun expandNotifications() {
        runCatching {
            val sb = app.getSystemService("statusbar")
            sb.javaClass.getMethod("expandNotificationsPanel").invoke(sb)
        }
    }

    // ---------------- Private Space ----------------
    private var pollJob: Job? = null

    fun setPrivateLocked(lock: Boolean, silent: Boolean = false) {
        val user: UserHandle = repo.state.value.privateSpace?.user ?: return
        try {
            um.requestQuietModeEnabled(lock, user)
        } catch (_: SecurityException) {
            if (!silent) toast("Make Bastion your default home app to control Private Space")
            return
        } catch (_: Exception) { return }
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            repeat(20) { repo.refresh(); delay(400) }
        }
    }

    fun privateSpaceSettings(act: FragmentActivity) {
        if (Build.VERSION.SDK_INT >= 35) {
            val sender = runCatching { la.privateSpaceSettingsIntent }.getOrNull()
            if (sender != null) {
                runCatching { act.startIntentSender(sender, null, 0, 0, 0); return }
            }
        }
        runCatching { act.startActivity(Intent(SysSettings.ACTION_SECURITY_SETTINGS)) }
    }

    fun lockEverything(silent: Boolean = false) {
        vaultOpen.value = false
        privateOpen.value = false
        setPrivateLocked(true, silent = true)
        if (!silent) toast("Everything locked down")
    }

    // ---------------- Settings helpers ----------------
    private fun Set<String>.flip(k: String) = if (k in this) this - k else this + k

    /** Number of home-grid cells currently on screen (set by the UI). */
    var homeCells = 20

    private fun Map<Int, String>.firstEmpty(range: IntRange) = range.firstOrNull { it !in this }
    private val dockRange get() = Settings.DOCK_START until Settings.DOCK_START + Settings.DOCK_SIZE

    fun toggleHidden(k: String) = prefs.update {
        if (k in it.hidden) it.copy(hidden = it.hidden - k)
        else it.copy(hidden = it.hidden + k, layout = it.layout.filterValues { v -> v != k })
    }
    fun toggleLocked(k: String) = prefs.update { it.copy(locked = it.locked.flip(k)) }
    fun toggleVault(k: String) = prefs.update {
        it.copy(vault = it.vault.flip(k), layout = it.layout.filterValues { v -> v != k })
    }
    fun toggleDock(k: String) = toggleZone(k, dockRange, "Dock is full")
    fun toggleHome(k: String) = toggleZone(k, 0 until homeCells, "Home screen is full")

    private fun toggleZone(k: String, range: IntRange, fullMsg: String) = prefs.update { s ->
        val there = s.layout.entries.firstOrNull { it.value == k && it.key in range }
        if (there != null) return@update s.copy(layout = s.layout - there.key)
        val slot = s.layout.firstEmpty(range) ?: run { toast(fullMsg); return@update s }
        s.copy(layout = s.layout.filterValues { it != k } + (slot to k))
    }

    fun removeSlot(slot: Int) = prefs.update { it.copy(layout = it.layout - slot) }

    /** Drag-and-drop: put [key] (coming from [from], or from the drawer when null) into slot [to]. */
    fun drop(key: String, from: Int?, to: Int) = prefs.update { s ->
        val m = s.layout.toMutableMap()
        val occupant = m[to]
        val origin = from ?: m.entries.firstOrNull { it.value == key }?.key
        if (origin == to) return@update s
        if (origin != null) {
            m.remove(origin)
            if (occupant != null) m[origin] = occupant            // swap places
        } else if (occupant != null) {                            // new app lands on an occupied cell
            val zone = if (to >= Settings.DOCK_START) dockRange else 0 until homeCells
            (m.firstEmpty(zone) ?: m.firstEmpty(0 until homeCells))?.let { m[it] = occupant }
        }
        m[to] = key
        s.copy(layout = m)
    }

    /** Dropped from the drawer onto open space: first empty home cell. */
    fun dropAnywhere(key: String) = prefs.update { s ->
        if (s.layout.values.contains(key)) return@update s
        val slot = s.layout.firstEmpty(0 until homeCells) ?: run { toast("Home screen is full"); return@update s }
        s.copy(layout = s.layout + (slot to key))
    }

    fun update(block: (Settings) -> Settings) = prefs.update(block)
}
