package com.vyd.bastion

import android.content.ComponentName
import android.content.Intent
import android.os.Process
import android.os.UserHandle
import android.view.WindowManager
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class LauncherUiTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private lateinit var vm: LauncherViewModel
    private val me: UserHandle = Process.myUserHandle()
    private val privUser: UserHandle = UserHandle.getUserHandleForUid(10 * 100000)

    private fun app(label: String, pkg: String, cat: Category, user: UserHandle = me, kind: ProfileKind = ProfileKind.MAIN) =
        AppEntry("$pkg/.Main#${if (kind == ProfileKind.PRIVATE) 10 else 0}", label, ComponentName(pkg, "$pkg.Main"), user, kind, cat, null)

    private val chrome = app("Chrome", "com.android.chrome", Category.TOOLS)
    private val insta = app("Instagram", "com.instagram.android", Category.SOCIAL)
    private val msgs = app("Messages", "com.google.android.apps.messaging", Category.CHAT)
    private val yt = app("YouTube", "com.google.android.youtube", Category.MEDIA)
    private val privX = app("X", "com.twitter.android", Category.SOCIAL, privUser, ProfileKind.PRIVATE)

    private fun tick(ms: Long = 1200) = rule.mainClock.advanceTimeBy(ms)
    private val s get() = vm.prefs.settings.value

    private fun inject(privateLocked: Boolean = true) {
        rule.runOnUiThread {
            vm.repo.injectForTest(AppsState(true, listOf(chrome, insta, msgs, yt), PrivateSpace(privUser, privateLocked, if (privateLocked) emptyList() else listOf(privX))))
        }
        tick()
    }

    companion object {
        init { com.vyd.bastion.ui.Motion.enabled = false }
    }

    @Before fun setUp() {
        // Simulator: behave like a phone with no screen lock, so privacy checks pass straight through.
        org.robolectric.shadow.api.Shadow.extract<org.robolectric.shadows.ShadowBiometricManager>(
            androidx.test.core.app.ApplicationProvider.getApplicationContext<android.app.Application>()
                .getSystemService(android.hardware.biometrics.BiometricManager::class.java)
        ).setCanAuthenticate(false)
        vm = ViewModelProvider(rule.activity)[LauncherViewModel::class.java]
        inject()
    }

    private fun openDrawer() {
        rule.onNodeWithText("Search apps").performTouchInput {
            swipeUp(startY = centerY, endY = centerY - 1200f, durationMillis = 200)
        }
        tick(1500)
    }

    private fun openPrivacyCenter() {
        openDrawer()
        rule.onNodeWithContentDescription("Privacy").performClick()
        tick(1500)
        rule.onNodeWithText("Privacy Center").assertExists()
    }

    /** Finds [text], scrolling any lazy list until it's composed, then taps it. */
    private fun tapRow(text: String) {
        if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            val lists = rule.onAllNodes(hasScrollToNodeAction()).fetchSemanticsNodes().size
            for (i in 0 until lists) {
                runCatching { rule.onAllNodes(hasScrollToNodeAction())[i].performScrollToNode(hasText(text)) }
                tick(300)
                if (rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()) break
            }
        }
        val node = rule.onAllNodes(hasText(text) and hasClickAction()).let { all ->
            if (all.fetchSemanticsNodes().isNotEmpty()) all[0] else rule.onAllNodesWithText(text)[0]
        }
        runCatching { node.performScrollTo() }
        node.performClick(); tick()
    }
    private fun tapTile(text: String) { rule.onNode(hasText(text) and hasClickAction()).performClick(); tick(1500) }

    // ---------------- Home ----------------
    @Test fun home_showsClockSearchAndDock() {
        rule.onNodeWithText("Search apps").assertExists()
        rule.onNodeWithText("Good", substring = true).assertExists()
    }

    @Test fun home_longPressDoesNotOpenPrivacy() {
        rule.onNodeWithText("Good", substring = true).performTouchInput { longClick() }
        tick()
        rule.onNodeWithText("Privacy Center").assertDoesNotExist()
    }

    // ---------------- Drawer ----------------
    @Test fun drawer_opensAndShowsApps_noPrivateTab() {
        openDrawer()
        rule.onNodeWithText("All apps").assertExists()
        rule.onNodeWithText("Chrome").assertExists()
        rule.onNodeWithContentDescription("Private space").assertDoesNotExist()
    }

    @Test fun drawer_categoryBarSwitchesPage() {
        openDrawer()
        rule.onNodeWithContentDescription("Social").performClick(); tick(1500)
        rule.onNodeWithText("Social").assertExists()
        rule.onNodeWithText("Instagram").assertExists()
    }

    @Test fun drawer_searchFiltersAndSecretCodeOpensVault() {
        openDrawer()
        rule.onNodeWithContentDescription("Search").performClick(); tick()
        rule.onNode(hasSetTextAction()).performTextInput("chr"); tick()
        rule.onNodeWithText("Chrome").assertExists()
        rule.onNodeWithText("Instagram").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextReplacement("vault"); tick(1500)
        rule.onNodeWithText("Only you can open this.").assertExists()
    }

    @Test fun drawer_storeButtonStartsSomething() {
        openDrawer()
        rule.onNodeWithContentDescription("Store").performClick(); tick()
    }

    @Test fun drawer_tapAppLaunchesWithoutCrash() {
        openDrawer()
        rule.onNodeWithText("Chrome").performClick(); tick()
    }

    // ---------------- App options (long-press) ----------------
    private fun openOptions(label: String) {
        openDrawer()
        rule.onNodeWithText(label).performTouchInput { longClick() }
        tick(1500)
        rule.onNodeWithText("Uninstall").assertExists()
    }

    @Test fun options_dock() { openOptions("Chrome"); tapRow("Dock"); assertTrue(chrome.key in s.dock) }
    @Test fun options_toHome() { openOptions("Chrome"); tapRow("To home"); assertTrue(s.layout.any { it.value == chrome.key && it.key < 100 }) }
    @Test fun options_hide() { openOptions("Chrome"); tapRow("Hide"); assertTrue(chrome.key in s.hidden) }
    @Test fun options_lock() { openOptions("Chrome"); tapRow("Lock"); assertTrue(chrome.key in s.locked) }
    @Test fun options_vault() { openOptions("Chrome"); tapRow("Vault"); assertTrue(chrome.key in s.vault) }
    @Test fun options_info() { openOptions("Chrome"); tapRow("Info") }
    @Test fun options_uninstall() {
        openOptions("Chrome"); tapRow("Uninstall")
        assertEquals(Intent.ACTION_DELETE, shadowOf(rule.activity).nextStartedActivity?.action)
    }

    // ---------------- Drag & drop ----------------
    @Test fun drag_fromDrawerToHome() {
        openDrawer()
        rule.onNodeWithText("Chrome").performTouchInput {
            down(center); advanceEventTime(800); moveBy(androidx.compose.ui.geometry.Offset(0f, 40f))
        }
        tick(600)
        rule.onNodeWithText("Chrome").performTouchInput { moveBy(androidx.compose.ui.geometry.Offset(0f, 200f)); up() }
        tick(1500)
        assertTrue("layout=${s.layout}", s.layout.values.contains(chrome.key))
    }

    // ---------------- Privacy Center ----------------
    @Test fun pc_opensFromDots() = openPrivacyCenter()

    @Test fun pc_toggles() {
        openPrivacyCenter()
        val a0 = s.autoLock; tapRow("Auto-lock on screen off"); assertNotEquals(a0, s.autoLock)
        tapRow("Block screenshots & recents"); assertTrue(s.secureScreen)
        tick()
        assertTrue((rule.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0)
        tapRow("Hidden apps in search"); assertTrue(s.hiddenInSearch)
        tapRow("Lock this Privacy Center"); assertTrue(s.lockPrivacyCenter)
        tapRow("Lock this Privacy Center"); assertFalse(s.lockPrivacyCenter)
        tapRow("Stealth vault"); assertTrue(s.stealthVault)
        tapRow("Stealth vault"); assertFalse(s.stealthVault)
    }

    @Test fun pc_switchVisualStateFollows() {
        openPrivacyCenter()
        tapRow("Hidden apps in search")
        rule.onNode(hasText("Hidden apps in search")).assertExists()
        assertTrue(s.hiddenInSearch)
        rule.onAllNodes(isToggleable() and isOn()).fetchSemanticsNodes().let { assertTrue(it.size >= 2) } // auto-lock + this
    }

    @Test fun pc_gridColumns() {
        openPrivacyCenter()
        val c0 = s.columns
        tapRow("Grid columns")
        rule.onNodeWithContentDescription("More columns").performClick(); tick()
        assertEquals(c0 + 1, s.columns)
        rule.onNodeWithContentDescription("Fewer columns").performClick(); tick()
        assertEquals(c0, s.columns)
    }

    @Test fun pc_hiddenPicker() {
        openPrivacyCenter()
        tapRow("Hidden apps"); tick(800)
        rule.onNodeWithText("Hidden apps disappear", substring = true).assertExists()
        tapRow("Instagram")
        assertTrue(insta.key in s.hidden)
        rule.onNodeWithContentDescription("Back").performClick(); tick(800)
        rule.onNodeWithText("Privacy Center").assertExists()
    }

    @Test fun pc_lockedPicker() {
        openPrivacyCenter()
        tapRow("Locked apps"); tick(800)
        tapRow("YouTube")
        assertTrue(yt.key in s.locked)
    }

    @Test fun pc_vaultPicker() {
        openPrivacyCenter()
        tapRow("Vault apps"); tick(800)
        tapRow("Messages")
        assertTrue(msgs.key in s.vault)
    }

    @Test fun pc_secretCode() {
        openPrivacyCenter()
        tapRow("Secret code"); tick(800)
        rule.onAllNodes(hasSetTextAction())[0].performTextReplacement("bananas"); tick()
        rule.onNodeWithText("Save").performClick(); tick()
        assertEquals("bananas", s.secretCode)
    }

    @Test fun pc_lockAllTile() { openPrivacyCenter(); rule.onNodeWithText("Lock all").performClick(); tick() }

    @Test fun pc_vaultTileOpensVault() {
        openPrivacyCenter()
        tapTile("Vault")
        rule.onNodeWithText("Only you can open this.").assertExists()
        rule.onNodeWithText("Add apps").performClick(); tick(1500)
        rule.onNodeWithText("Vault apps vanish", substring = true).assertExists()
    }

    @Test fun pc_privateTileOpensPrivateSpace_locked() {
        openPrivacyCenter()
        tapTile("Private")
        rule.onNodeWithText("A space for your private apps").assertExists()
        rule.onNodeWithText("Unlock").performClick(); tick()
        rule.onNodeWithContentDescription("Close").performClick(); tick(1000)
        rule.onNodeWithText("A space for your private apps").assertDoesNotExist()
    }

    @Test fun privateSpace_unlockedShowsAppsAndTheyLaunch() {
        inject(privateLocked = false)
        openPrivacyCenter()
        tapTile("Private")
        rule.onNodeWithText("X").assertExists().performClick(); tick()
        rule.onNodeWithText("Lock").assertExists()
    }

    @Test fun pc_privateRows() {
        openPrivacyCenter()
        tapRow("Open Private Space"); tick(1200)
        rule.onNodeWithText("A space for your private apps").assertExists()
    }

    @Test fun pc_privateSettingsAndInstallRows() {
        openPrivacyCenter()
        tapRow("Install into Private Space")
        openPrivacyCenterAgain()
        tapRow("Private Space settings")
    }

    private fun openPrivacyCenterAgain() {
        if (rule.onAllNodesWithText("Privacy Center").fetchSemanticsNodes().isEmpty()) {
            rule.onNodeWithContentDescription("Privacy").performClick(); tick(1500)
        }
    }

    @Test fun pc_defaultHomeRow() {
        openPrivacyCenter()
        tapRow("Default home app")
        assertEquals(android.provider.Settings.ACTION_HOME_SETTINGS, shadowOf(rule.activity).nextStartedActivity?.action)
    }

    @Test fun pc_checkForUpdatesRow() {
        openPrivacyCenter()
        tapRow("Check for updates"); tick(3000)
    }

    @Test fun pc_toggleChangesVisibleOnHome_hiddenAppLeavesDrawer() {
        openPrivacyCenter()
        tapRow("Hidden apps"); tick(800)
        tapRow("Chrome")
        rule.activity.onBackPressedDispatcher.let { rule.runOnUiThread { it.onBackPressed() } }; tick(800)
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }; tick(1500)
        // sheet may still be open; dismiss via home signal
        rule.runOnUiThread { vm.goHome() }; tick(1500)
        openDrawer()
        rule.onNodeWithText("Chrome").assertDoesNotExist()
    }
}
