package com.vyd.bastion

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.vyd.bastion.ui.BastionTheme
import com.vyd.bastion.ui.CrashScreen
import com.vyd.bastion.ui.LauncherRoot
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {
    private val vm: LauncherViewModel by viewModels()

    private val systemReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            when (i?.action) {
                Intent.ACTION_SCREEN_OFF -> vm.onScreenOff()
                else -> vm.repo.refresh()
            }
        }
    }

    private var showingCrash = false

    override fun onCreate(savedInstanceState: Bundle?) {
        CrashReporter.install(applicationContext)
        super.onCreate(savedInstanceState)
        CrashReporter.pending(this)?.let { trace ->
            showingCrash = true
            enableEdgeToEdge()
            setContent { BastionTheme { CrashScreen(trace) { CrashReporter.clear(this); recreate() } } }
            return
        }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_MANAGED_PROFILE_AVAILABLE)
            addAction(Intent.ACTION_MANAGED_PROFILE_UNAVAILABLE)
            addAction(Intent.ACTION_PROFILE_ACCESSIBLE)
            addAction(Intent.ACTION_PROFILE_INACCESSIBLE)
            addAction("android.intent.action.PROFILE_AVAILABLE")
            addAction("android.intent.action.PROFILE_UNAVAILABLE")
            addAction(Intent.ACTION_PROFILE_ADDED)
            addAction(Intent.ACTION_PROFILE_REMOVED)
        }
        ContextCompat.registerReceiver(this, systemReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)

        lifecycleScope.launch {
            vm.prefs.settings.collect { s ->
                if (s.secureScreen) window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                else window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }

        // Keep checking for updates every 10 minutes while the launcher is on screen.
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
                while (com.vyd.bastion.ui.Motion.enabled) {
                    Updater.check(applicationContext)
                    kotlinx.coroutines.delay(10 * 60 * 1000L)
                }
            }
        }

        setContent { BastionTheme { LauncherRoot(vm) } }
    }

    override fun onResume() {
        super.onResume()
        if (!showingCrash) {
            vm.onResume()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (showingCrash) return
        if (intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)) vm.goHome()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(systemReceiver) }
        super.onDestroy()
    }
}
