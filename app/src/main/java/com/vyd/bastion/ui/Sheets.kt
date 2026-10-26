package com.vyd.bastion.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dock
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.vyd.bastion.AppEntry
import com.vyd.bastion.AppRepository
import com.vyd.bastion.AppsState
import com.vyd.bastion.LauncherViewModel
import com.vyd.bastion.ProfileKind
import com.vyd.bastion.Settings

private enum class PcPage { MAIN, HIDDEN, LOCKED, VAULT, SECRET, DIAG }

// ======================================================================
// PRIVACY CENTER — opened from the ⋮ button
// ======================================================================
@Composable
fun PrivacyCenterPanel(
    visible: Boolean,
    vm: LauncherViewModel, apps: AppsState, s: Settings, isDefault: Boolean,
    onDismiss: () -> Unit, onOpenVault: () -> Unit, onOpenPrivate: () -> Unit,
) {
    val act = LocalContext.current.findActivity()
    var page by remember { mutableStateOf(PcPage.MAIN) }
    androidx.compose.runtime.LaunchedEffect(visible) {
        if (visible) com.vyd.bastion.Diag.log("Privacy Center opened") else page = PcPage.MAIN
    }
    fun gated(p: PcPage, title: String) = vm.authenticate(act, title) { page = p }

    BackHandler(visible) { if (page != PcPage.MAIN) page = PcPage.MAIN else onDismiss() }
    Panel(visible, onDismiss, heightFraction = 1f) {
        AnimatedContent(
            page, label = "pc",
            transitionSpec = {
                val fwd = targetState.ordinal > initialState.ordinal
                (slideInHorizontally(tween(300)) { if (fwd) it else -it } + fadeIn()) togetherWith
                    (slideOutHorizontally(tween(300)) { if (fwd) -it / 3 else it / 3 } + fadeOut())
            },
            modifier = Modifier.fillMaxSize(),
        ) { p ->
            when (p) {
                PcPage.MAIN -> PcMain(vm, apps, s, isDefault, onOpenVault, onOpenPrivate, onNav = { target ->
                    when (target) {
                        PcPage.HIDDEN -> gated(target, "Manage hidden apps")
                        PcPage.LOCKED -> gated(target, "Manage locked apps")
                        PcPage.VAULT -> gated(target, "Manage vault")
                        PcPage.SECRET -> gated(target, "Change secret code")
                        PcPage.MAIN, PcPage.DIAG -> page = target
                    }
                })
                PcPage.HIDDEN -> PickerPage("Hidden apps", "Hidden apps disappear from the drawer and dock. They still run normally.",
                    apps.apps, s.hidden, vm.repo, vm::toggleHidden) { page = PcPage.MAIN }
                PcPage.LOCKED -> PickerPage("Locked apps", "Opening a locked app asks for your fingerprint or PIN first.",
                    apps.apps, s.locked, vm.repo, vm::toggleLocked) { page = PcPage.MAIN }
                PcPage.VAULT -> PickerPage("Vault apps", "Vault apps vanish from drawer, search and dock — only visible inside the Vault.",
                    apps.apps, s.vault, vm.repo, vm::toggleVault) { page = PcPage.MAIN }
                PcPage.SECRET -> SecretPage(vm, s) { page = PcPage.MAIN }
                PcPage.DIAG -> DiagPage { page = PcPage.MAIN }
            }
        }
    }
}

@Composable
private fun PcMain(
    vm: LauncherViewModel, apps: AppsState, s: Settings, isDefault: Boolean,
    onOpenVault: () -> Unit, onOpenPrivate: () -> Unit, onNav: (PcPage) -> Unit,
) {
    val act = LocalContext.current.findActivity()
    val ps = apps.privateSpace
    LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
        item {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ShieldPulse(54.dp, active = ps?.locked != false, icon = Icons.Rounded.Shield)
                Text("Privacy Center", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (ps?.locked == false) "Private space is open" else "You're locked down",
                    color = if (ps?.locked == false) Color(0xFFFFC857) else Mint, fontSize = 14.sp,
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Stat("Hidden", s.hidden.size, Modifier.weight(1f))
                    Stat("Locked", s.locked.size, Modifier.weight(1f))
                    if (!s.stealthVault) Stat("Vault", s.vault.size, Modifier.weight(1f))
                    if (ps != null) Stat("Private", ps.apps.size.takeIf { !ps.locked } ?: -1, Modifier.weight(1f))
                }
            }
        }
        if (!isDefault) item {
            Row(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp)).background(Color(0x33FFC857))
                    .clickable { vm.openHomeSettings(act) }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFC857))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Set Bastion as default home", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Required for Private Space, Home button & full protection.", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
            }
        }
        item {
            Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTile("Lock all", Icons.Rounded.Lock, listOf(Mint, Cyan), Modifier.weight(1f)) { vm.lockEverything() }
                if (!s.stealthVault) QuickTile("Vault", Icons.Rounded.Inventory2, listOf(Violet, Color(0xFFFF5FA2)), Modifier.weight(1f), onOpenVault)
                QuickTile("Private", Icons.Rounded.Security, listOf(Color(0xFF6C4BFF), Cyan), Modifier.weight(1f), onOpenPrivate)
            }
        }

        item { SectionLabel("Private space") }
        item { NavRow(Icons.Rounded.Security, "Open Private Space", "Hidden from your drawer — only reachable here") { onOpenPrivate() } }
        if (ps != null) {
            item {
                ToggleRow(Icons.Rounded.Security, "Private space locked",
                    if (ps.locked) "Apps are hidden & stopped" else "${ps.apps.size} private apps visible",
                    ps.locked) { vm.setPrivateLocked(it) }
            }
            item { NavRow(Icons.Rounded.ShoppingBag, "Install into Private Space", "Opens Play Store inside the space") { vm.openStore(act, true) } }
            item { NavRow(Icons.Rounded.Tune, "Private Space settings", "Lock type, auto-lock, hide on lock") { vm.privateSpaceSettings(act) } }
        } else {
            item {
                NavRow(Icons.Rounded.Security, "Set up Private Space",
                    if (!isDefault) "Make Bastion your default home, then it appears here"
                    else "Settings › Security & privacy › Private space") { vm.privateSpaceSettings(act) }
            }
        }

        item { SectionLabel("Hide & lock") }
        item { NavRow(Icons.Rounded.VisibilityOff, "Hidden apps", "${s.hidden.size} hidden") { onNav(PcPage.HIDDEN) } }
        item { NavRow(Icons.Rounded.Lock, "Locked apps", "${s.locked.size} need fingerprint/PIN") { onNav(PcPage.LOCKED) } }
        if (!s.stealthVault) item { NavRow(Icons.Rounded.Inventory2, "Vault apps", "${s.vault.size} in your vault") { onNav(PcPage.VAULT) } }

        item { SectionLabel("Stealth") }
        item {
            ToggleRow(Icons.Rounded.Visibility, "Stealth vault", "Hide every trace of the Vault. Open it by typing your secret code in search.",
                s.stealthVault) { on ->
                if (on) vm.update { it.copy(stealthVault = true) }
                else vm.authenticate(act, "Turn off stealth") { vm.update { it.copy(stealthVault = false) } }
            }
        }
        item { NavRow(Icons.Rounded.Key, "Secret code", "Currently set — tap to change") { onNav(PcPage.SECRET) } }

        item { SectionLabel("Protection") }
        item {
            ToggleRow(Icons.Rounded.Timer, "Auto-lock on screen off", "Locks Private Space & Vault the moment your screen turns off",
                s.autoLock) { on -> vm.update { it.copy(autoLock = on) } }
        }
        item {
            ToggleRow(Icons.Rounded.Screenshot, "Block screenshots & recents", "Launcher can't be screenshotted or previewed",
                s.secureScreen) { on -> vm.update { it.copy(secureScreen = on) } }
        }
        item {
            ToggleRow(Icons.Rounded.Search, "Hidden apps in search", "Let hidden (not vault) apps show up when you search",
                s.hiddenInSearch) { on -> vm.update { it.copy(hiddenInSearch = on) } }
        }
        item {
            ToggleRow(Icons.Rounded.Shield, "Lock this Privacy Center", "Ask for fingerprint before opening this menu",
                s.lockPrivacyCenter) { on ->
                if (on) vm.update { it.copy(lockPrivacyCenter = true) }
                else vm.authenticate(act, "Unlock Privacy Center") { vm.update { it.copy(lockPrivacyCenter = false) } }
            }
        }

        item { SectionLabel("Launcher") }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                RowIcon(Icons.Rounded.GridView)
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text("Grid columns", color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Apps per row in the drawer", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
                IconButton({ vm.update { it.copy(columns = (it.columns - 1).coerceAtLeast(3)) } }) { Icon(Icons.Rounded.Remove, "Fewer columns", tint = Color.White) }
                AnimatedContent(s.columns, label = "cols") { Text("$it", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                IconButton({ vm.update { it.copy(columns = (it.columns + 1).coerceAtMost(6)) } }) { Icon(Icons.Rounded.Add, "More columns", tint = Color.White) }
            }
        }
        item {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val up by com.vyd.bastion.Updater.state.collectAsState()
            var checking by remember { mutableStateOf(false) }
            val ver = act.packageManager.getPackageInfo(act.packageName, 0).versionName
            val (title, sub) = when (val u = up) {
                is com.vyd.bastion.Updater.State.Available -> "Install update ${u.version}" to "You have $ver — tap to download & install"
                is com.vyd.bastion.Updater.State.Downloading ->
                    "Updating to ${u.version}" to (if (u.progress >= 1f) "Confirm the install…" else "Downloading ${(u.progress * 100).toInt()}%")
                is com.vyd.bastion.Updater.State.Failed -> "Update failed" to "${u.msg} — tap to retry"
                else -> "Check for updates" to (if (checking) "Checking GitHub…" else "Version $ver")
            }
            NavRow(Icons.Rounded.AutoAwesome, title, sub) {
                when (val u = up) {
                    is com.vyd.bastion.Updater.State.Available ->
                        if (!com.vyd.bastion.Updater.canInstall(act)) {
                            vm.toast("Allow Bastion to install updates, then tap again")
                            com.vyd.bastion.Updater.openInstallPermission(act)
                        } else scope.launch { com.vyd.bastion.Updater.downloadAndInstall(act, u) }
                    is com.vyd.bastion.Updater.State.Downloading -> {}
                    else -> scope.launch { checking = true; com.vyd.bastion.Updater.check(act, force = true); checking = false }
                }
            }
        }
        item { NavRow(Icons.Rounded.Info, "Diagnostics", "Log of every button press — copy it to Claude if something fails") { onNav(PcPage.DIAG) } }
        item { NavRow(Icons.Rounded.Home, "Default home app", if (isDefault) "Bastion is your home ✓" else "Not set — tap to choose Bastion") { vm.openHomeSettings(act) } }
        item {
            Text("Gestures: swipe up = apps · swipe down = notifications · hold an app = menu · hold + drag = move it · double-tap home = lock everything",
                color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp))
        }
    }
}

@Composable
private fun Stat(label: String, value: Int, modifier: Modifier) {
    val v by animateIntAsState(value.coerceAtLeast(0), tween(700), label = "stat")
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(CardBg).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(if (value < 0) "🔒" else "$v", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}

@Composable
private fun QuickTile(text: String, icon: ImageVector, colors: List<Color>, modifier: Modifier, onClick0: () -> Unit) {
    val onClick = { com.vyd.bastion.Diag.log("tap tile: $text"); onClick0() }
    val src = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(if (pressed) 0.92f else 1f, spring(0.4f, 600f), label = "qt")
    Column(
        modifier.graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(colors))
            .clickable(interactionSource = src, indication = androidx.compose.material3.ripple(), onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(26.dp))
        Spacer(Modifier.height(6.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun RowIcon(icon: ImageVector) = Box(
    Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(CardBg), contentAlignment = Alignment.Center,
) { Icon(icon, null, tint = Violet, modifier = Modifier.size(22.dp)) }

@Composable
private fun NavRow(icon: ImageVector, title: String, sub: String, onClick0: () -> Unit) {
    val onClick = { com.vyd.bastion.Diag.log("tap row: $title"); onClick0() }
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha = 0.5f))
    }
}

@Composable
private fun ToggleRow(icon: ImageVector, title: String, sub: String, checked: Boolean, onChange0: (Boolean) -> Unit) {
    val onChange = { v: Boolean -> com.vyd.bastion.Diag.log("toggle: $title -> $v"); onChange0(v) }
    Row(
        Modifier.fillMaxWidth().clickable { onChange(!checked) }.padding(horizontal = 20.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(icon)
        Column(Modifier.weight(1f).padding(start = 14.dp, end = 10.dp)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(sub, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = Violet, checkedThumbColor = Color.White))
    }
}

// ----------------------------------------------------------------------
@Composable
private fun PickerPage(
    title: String, subtitle: String, apps: List<AppEntry>, selected: Set<String>,
    repo: AppRepository, onToggle: (String) -> Unit, onBack: () -> Unit,
) {
    var q by remember { mutableStateOf("") }
    val list = remember(apps, q) { apps.filter { q.isBlank() || it.label.contains(q.trim(), true) } }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 20.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
            Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            AnimatedContent(selected.size, label = "cnt") {
                Text("$it selected", color = Mint, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(subtitle, color = Color.White.copy(alpha = 0.65f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Row(
            Modifier.padding(20.dp).fillMaxWidth().height(48.dp).clip(RoundedCornerShape(24.dp)).background(CardBg)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.width(10.dp))
            Box(Modifier.weight(1f)) {
                if (q.isEmpty()) Text("Search", color = Color.White.copy(alpha = 0.45f))
                BasicTextField(q, { q = it }, singleLine = true, cursorBrush = SolidColor(Cyan),
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp), modifier = Modifier.fillMaxWidth())
            }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = 40.dp)) {
            items(list, key = { it.key }) { e -> PickRow(e, e.key in selected, repo) { onToggle(e.key) } }
        }
    }
}

@Composable
private fun PickRow(e: AppEntry, checked: Boolean, repo: AppRepository, onClick: () -> Unit) {
    val px = with(LocalDensity.current) { 42.dp.roundToPx() }
    val bmp by produceState(repo.cachedIcon(e.key), e.key) { value = repo.icon(e, px) }
    val bg by animateColorAsState(if (checked) Violet else Color.Transparent, label = "pk")
    val sc by animateFloatAsState(if (checked) 1f else 0f, spring(0.45f, 500f), label = "pks")
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bmp?.let { Image(it, null, Modifier.size(42.dp)) } ?: Spacer(Modifier.size(42.dp))
        Text(e.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 14.dp))
        Box(
            Modifier.size(28.dp).clip(CircleShape).background(bg)
                .border(2.dp, if (checked) Violet else Color.White.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Check, null, tint = Color.White,
                modifier = Modifier.size(18.dp).graphicsLayer { scaleX = sc; scaleY = sc })
        }
    }
}

@Composable
private fun SecretPage(vm: LauncherViewModel, s: Settings, onBack: () -> Unit) {
    var code by remember { mutableStateOf(s.secretCode) }
    var name by remember { mutableStateOf(s.name) }
    Column(Modifier.fillMaxWidth().padding(bottom = 30.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
            Text("Secret code", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Text("Type this word into app search and the Vault opens (after your fingerprint). Pick something nobody would search for.",
            color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 24.dp))
        Field(Icons.Rounded.Key, "Secret code", code) { code = it.replace(" ", "") }
        Field(Icons.Rounded.Person, "Your name (home greeting)", name) { name = it }
        Button(
            onClick = {
                vm.update { it.copy(secretCode = code.ifBlank { "vault" }, name = name.trim()) }
                vm.toast("Saved"); onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Violet),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp).fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(16.dp),
        ) { Text("Save", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun Field(icon: ImageVector, label: String, value: String, onChange: (String) -> Unit) {
    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(label, color = Violet, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
        Row(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(CardBg).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = Color.White.copy(alpha = 0.7f))
            Spacer(Modifier.width(10.dp))
            BasicTextField(value, onChange, singleLine = true, cursorBrush = SolidColor(Cyan),
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp), modifier = Modifier.fillMaxWidth())
        }
    }
}

// ======================================================================
// Standalone picker (used from the Vault "manage" button)
// ======================================================================
@Composable
fun AppPickerPanel(
    visible: Boolean, title: String, subtitle: String, apps: List<AppEntry>, selected: Set<String>, repo: AppRepository,
    onToggle: (String) -> Unit, onDismiss: () -> Unit,
) {
    BackHandler(visible) { onDismiss() }
    Panel(visible, onDismiss, heightFraction = 0.92f) {
        PickerPage(title, subtitle, apps, selected, repo, onToggle, onDismiss)
    }
}

// ======================================================================
// LONG-PRESS APP OPTIONS
// ======================================================================
@Composable
fun AppOptionsPanel(entry: AppEntry?, vm: LauncherViewModel, s: Settings, onDismiss: () -> Unit) {
    var last by remember { mutableStateOf(entry) }
    if (entry != null) last = entry
    BackHandler(entry != null) { onDismiss() }
    Panel(entry != null, onDismiss, heightFraction = null) {
        last?.let { AppOptionsContent(it, vm, s, onDismiss) }
    }
}

@Composable
private fun AppOptionsContent(e: AppEntry, vm: LauncherViewModel, s: Settings, onDismiss: () -> Unit) {
    val act = LocalContext.current.findActivity()
    val px = with(LocalDensity.current) { 64.dp.roundToPx() }
    val bmp by produceState(vm.repo.cachedIcon(e.key), e.key) { value = vm.repo.icon(e, px) }
    val main = e.profile != ProfileKind.PRIVATE
    val locked = e.key in s.locked
    run {
        Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            bmp?.let { Image(it, null, Modifier.size(64.dp)) }
            Text(e.label, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(
                if (e.profile == ProfileKind.PRIVATE) "In Private Space" else e.packageName,
                color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp,
            )
            Spacer(Modifier.height(18.dp))
            val opts = buildList {
                val onHome = s.layout.any { it.value == e.key && it.key < com.vyd.bastion.Settings.DOCK_START }
                if (main && e.key !in s.vault) add(Opt(if (onHome) "Off home" else "To home", Icons.Rounded.Home, Color.White) { vm.toggleHome(e.key) })
                if (main && e.key !in s.vault) add(Opt(if (e.key in s.dock) "Undock" else "Dock", Icons.Rounded.Dock, Cyan) { vm.toggleDock(e.key) })
                if (main && e.key !in s.vault) add(Opt(if (e.key in s.hidden) "Unhide" else "Hide",
                    if (e.key in s.hidden) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff, Color(0xFFFFC857)) {
                    if (e.key in s.hidden) vm.authenticate(act, "Unhide ${e.label}") { vm.toggleHidden(e.key) } else vm.toggleHidden(e.key)
                })
                add(Opt(if (locked) "Unlock" else "Lock", if (locked) Icons.Rounded.LockOpen else Icons.Rounded.Lock, Mint) {
                    if (locked) vm.authenticate(act, "Remove lock from ${e.label}") { vm.toggleLocked(e.key) } else vm.toggleLocked(e.key)
                })
                if (main && (e.key in s.vault || !s.stealthVault)) add(Opt(if (e.key in s.vault) "Unvault" else "Vault", Icons.Rounded.Inventory2, Color(0xFFFF5FA2)) {
                    if (e.key in s.vault) vm.toggleVault(e.key)
                    else { vm.toggleVault(e.key); vm.toast("${e.label} moved to Vault") }
                })
                add(Opt("Info", Icons.Rounded.Info, Violet) { vm.appInfo(e) })
                add(Opt("Uninstall", Icons.Rounded.Delete, Danger) { vm.uninstall(act, e) })
            }
            opts.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { o ->
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(CardBg)
                                .clickable { com.vyd.bastion.Diag.log("app menu: ${o.label} ${e.label}"); o.action(); onDismiss() }.padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(Modifier.size(42.dp).clip(CircleShape).background(o.tint.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                                Icon(o.icon, null, tint = o.tint)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(o.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

private class Opt(val label: String, val icon: ImageVector, val tint: Color, val action: () -> Unit)

@Composable
private fun DiagPage(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val ver = ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "?"
    val text = remember { com.vyd.bastion.Diag.text(ver) }
    Column(Modifier.fillMaxSize().padding(bottom = 16.dp)) {
        Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
            Text("Diagnostics", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    ctx.getSystemService(android.content.ClipboardManager::class.java)
                        .setPrimaryClip(android.content.ClipData.newPlainText("Bastion diagnostics", text))
                    android.widget.Toast.makeText(ctx, "Copied — paste it to Claude", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Violet),
            ) { Text("Copy") }
        }
        Text("Every tap and what Android answered. If a button seems dead, tap it, come back here, and copy this.",
            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp))
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(16.dp)).background(CardBg)
                .verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp)
        ) {
            Text(text, color = Color(0xFFE6E1F5), fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 11.sp)
        }
    }
}
