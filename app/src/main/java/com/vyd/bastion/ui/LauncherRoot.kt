package com.vyd.bastion.ui

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.runtime.produceState
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vyd.bastion.AppEntry
import com.vyd.bastion.AppsState
import com.vyd.bastion.Category
import com.vyd.bastion.LauncherViewModel
import com.vyd.bastion.PrivateSpace
import com.vyd.bastion.Settings
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar

private sealed interface Page {
    data class Cat(val c: Category) : Page
    data object Private : Page
}

private fun Page.title() = when (this) { is Page.Cat -> c.title; Page.Private -> "Private space" }
private fun Page.icon(): ImageVector = when (this) {
    Page.Private -> Icons.Rounded.Shield
    is Page.Cat -> when (c) {
        Category.ALL -> Icons.Rounded.Apps
        Category.CHAT -> Icons.Rounded.ChatBubbleOutline
        Category.SOCIAL -> Icons.Rounded.Groups
        Category.GAMES -> Icons.Rounded.SportsEsports
        Category.MEDIA -> Icons.Rounded.PhotoLibrary
        Category.WORK -> Icons.Rounded.AutoAwesome
        Category.TOOLS -> Icons.Rounded.Build
    }
}

@Composable
fun LauncherRoot(vm: LauncherViewModel) {
    val act = LocalContext.current.findActivity()
    val apps by vm.repo.state.collectAsStateWithLifecycle()
    val s by vm.prefs.settings.collectAsStateWithLifecycle()
    val vaultOpen by vm.vaultOpen.collectAsStateWithLifecycle()
    val privateOpen by vm.privateOpen.collectAsStateWithLifecycle()
    val isDefault by vm.isDefaultHome.collectAsStateWithLifecycle()
    val homeSig by vm.homeSignal.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var progress by remember { mutableFloatStateOf(0f) }
    var heightPx by remember { mutableFloatStateOf(2000f) }
    var searchMode by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var optionsFor by remember { mutableStateOf<AppEntry?>(null) }
    var vaultManage by remember { mutableStateOf(false) }
    var settleJob by remember { mutableStateOf<Job?>(null) }
    val drawerShown by remember { androidx.compose.runtime.derivedStateOf { progress > 0.002f } }
    val drag = remember { DragState() }
    val kb = LocalSoftwareKeyboardController.current

    fun settle(target: Float, velocity: Float = 0f) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(progress, target, velocity / heightPx, spring(0.82f, 320f)) { v, _ -> progress = v }
            if (target == 0f) searchMode = false
        }
    }
    fun openDrawer(search: Boolean = false) { searchMode = search; settle(1f) }
    fun closeDrawer() = settle(0f)
    fun openPrivacy() {
        if (s.lockPrivacyCenter) vm.authenticate(act, "Open Privacy Center") { showPrivacy = true }
        else showPrivacy = true
    }
    fun openVault() = vm.authenticate(act, "Open Vault") { vm.vaultOpen.value = true }

    drag.onStart = { _, from -> if (from == null) { kb?.hide(); closeDrawer() } }
    drag.onDrop = { e, from, at ->
        val target = drag.slotAt(at)
        when {
            from != null && drag.removeAt(at) -> vm.removeSlot(from)
            target != null -> vm.drop(e.key, from, target)
            from == null -> vm.dropAnywhere(e.key)
        }
    }

    LaunchedEffect(homeSig) {
        if (homeSig > 0) {
            showPrivacy = false; optionsFor = null; vaultManage = false
            vm.vaultOpen.value = false; vm.privateOpen.value = false; closeDrawer()
        }
    }
    BackHandler {
        when {
            vaultOpen -> vm.vaultOpen.value = false
            privateOpen -> vm.privateOpen.value = false
            drawerShown -> closeDrawer()
        }
    }

    val visible = remember(apps, s.hidden, s.vault) { apps.apps.filter { it.key !in s.hidden && it.key !in s.vault } }
    val byKey = remember(apps) { apps.apps.associateBy { it.key } }

    // Drawer pulls closed when the grid is already at the top and you keep dragging down.
    val drawerConn = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (progress in 0.0001f..0.9999f && available.y < 0) {
                    settleJob?.cancel()
                    val old = progress
                    progress = (progress - available.y / heightPx).coerceIn(0f, 1f)
                    return Offset(0f, -(progress - old) * heightPx)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && source == NestedScrollSource.UserInput) {
                    settleJob?.cancel()
                    progress = (progress - available.y / heightPx).coerceIn(0f, 1f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (progress < 1f) {
                    settle(if (available.y > 1200f || progress < 0.75f) 0f else 1f, -available.y)
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalDrag provides drag) {
    Box(Modifier.fillMaxSize().onSizeChanged { heightPx = it.height.toFloat().coerceAtLeast(1f) }) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.12f + 0.58f * progress)))
        AuroraBackground(Modifier.fillMaxSize().graphicsLayer { alpha = 0.25f + 0.75f * progress })

        HomeScreen(
            vm = vm, s = s, byKey = byKey,
            onSearch = { openDrawer(true) },
            onOptions = { optionsFor = it },
            modifier = Modifier
                .graphicsLayer {
                    alpha = (1f - progress * 1.6f).coerceIn(0f, 1f)
                    translationY = -progress * heightPx * 0.12f
                    val sc = 1f - 0.06f * progress; scaleX = sc; scaleY = sc
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { d ->
                        settleJob?.cancel()
                        progress = (progress - d / heightPx * 1.2f).coerceIn(0f, 1f)
                    },
                    onDragStopped = { v ->
                        if (progress == 0f && v > 600f) vm.expandNotifications()
                        else settle(if (v < -700f || (progress > 0.22f && v < 700f)) 1f else 0f, -v)
                    },
                ),
        )

        if (drawerShown || drag.active) {
            Drawer(
                vm = vm, apps = apps, s = s, visible = visible, isDefault = isDefault,
                searchMode = searchMode, onSearchMode = { searchMode = it },
                onOptions = { optionsFor = it }, onPrivacy = ::openPrivacy, onOpenVault = ::openVault,
                conn = drawerConn,
                modifier = Modifier.graphicsLayer {
                    alpha = (progress * 1.4f - 0.15f).coerceIn(0f, 1f)
                    translationY = (1f - progress) * heightPx * 0.3f
                },
            )
        }

        AnimatedVisibility(
            vaultOpen,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.9f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 1.05f),
        ) {
            VaultOverlay(vm, apps, s, onOptions = { optionsFor = it }, onManage = { vaultManage = true })
        }

        AnimatedVisibility(
            privateOpen,
            enter = fadeIn(tween(250)) + scaleIn(initialScale = 0.92f),
            exit = fadeOut(tween(200)) + scaleOut(targetScale = 1.05f),
        ) {
            PrivateSpaceOverlay(vm, apps.privateSpace, s, isDefault, onOptions = { optionsFor = it }, onPrivacy = ::openPrivacy)
        }

        DragLayer(drag, vm)
    }
    }

    if (showPrivacy) {
        PrivacyCenterSheet(
            vm = vm, apps = apps, s = s, isDefault = isDefault,
            onDismiss = { showPrivacy = false },
            onOpenVault = { showPrivacy = false; openVault() },
            onOpenPrivate = { showPrivacy = false; vm.privateOpen.value = true },
        )
    }
    if (vaultManage) {
        AppPickerSheet(
            title = "Vault apps", subtitle = "Vault apps vanish from your drawer, search and dock.",
            apps = apps.apps, selected = s.vault, repo = vm.repo,
            onToggle = vm::toggleVault, onDismiss = { vaultManage = false },
        )
    }
    optionsFor?.let { e -> AppOptionsSheet(e, vm, s, onDismiss = { optionsFor = null }) }
}

// ======================================================================
// HOME
// ======================================================================
@Composable
private fun HomeScreen(
    vm: LauncherViewModel, s: Settings, byKey: Map<String, AppEntry>,
    onSearch: () -> Unit, onOptions: (AppEntry) -> Unit, modifier: Modifier,
) {
    Column(
        modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { vm.lockEverything(silent = true) }) }
        ) {
            Spacer(Modifier.height(32.dp))
            BigClock()
            Greeting(s.name)
            Spacer(Modifier.height(12.dp))
            UpdateBanner()
        }
        HomeGrid(vm, s, byKey, onOptions, Modifier.weight(1f).fillMaxWidth())

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(54.dp).glass(RoundedCornerShape(27.dp))
                .clickable(onClick = onSearch).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, null, tint = Color.White.copy(alpha = 0.85f))
            Spacer(Modifier.width(12.dp))
            Text("Search apps", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Dock(vm, s, byKey, onOptions)
        SwipeHint()
    }
}

/** Free-placement home grid. Cells register as drop targets; icons jiggle while something is being dragged. */
@Composable
private fun HomeGrid(
    vm: LauncherViewModel, s: Settings, byKey: Map<String, AppEntry>,
    onOptions: (AppEntry) -> Unit, modifier: Modifier,
) {
    val act = LocalContext.current.findActivity()
    val drag = LocalDrag.current
    val wobble by loop(-2.2f, 2.2f, infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "wob")
    androidx.compose.foundation.layout.BoxWithConstraints(modifier) {
        val cols = 4
        val rows = (maxHeight / 104.dp).toInt().coerceIn(1, 6)
        val cells = cols * rows
        LaunchedEffect(cells) { vm.homeCells = cells; drag.targets.keys.removeAll { it in cells until 100 } }
        val cw = maxWidth / cols
        val ch = maxHeight / rows
        val hover = if (drag.active) drag.hoverSlot() else null

        for (i in 0 until cells) {
            val r = i / cols; val c = i % cols
            val e = s.layout[i]?.let(byKey::get)?.takeIf { it.key !in s.hidden && it.key !in s.vault }
            val isHover = hover == i
            val ring by animateFloatAsState(if (isHover) 1f else 0f, spring(0.5f, 500f), label = "ring")
            Box(
                Modifier.offset(cw * c, ch * r).size(cw, ch)
                    .onGloballyPositioned { drag.targets[i] = it },
                contentAlignment = Alignment.Center,
            ) {
                if (e == null) Box(Modifier.fillMaxSize().pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { vm.lockEverything(silent = true) })
                })
                if (drag.active) {
                    Box(
                        Modifier.size(66.dp).graphicsLayer { val k = 0.85f + 0.15f * ring; scaleX = k; scaleY = k }
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.06f + 0.18f * ring))
                    )
                }
                if (e != null) {
                    val lifted = drag.active && drag.fromSlot == i
                    AppIcon(
                        e, vm.repo,
                        Modifier.fillMaxWidth().graphicsLayer {
                            alpha = if (lifted) 0.25f else 1f
                            rotationZ = if (drag.active && !lifted) wobble * (if (i % 2 == 0) 1f else -1f) else 0f
                        },
                        size = 56.dp, showLabel = ch >= 92.dp, locked = e.key in s.locked,
                        canDrag = true, dragSlot = i,
                        onClick = { b -> vm.launch(act, e, b) }, onLongClick = { onOptions(e) },
                    )
                }
            }
        }
    }
}

@Composable
private fun Dock(vm: LauncherViewModel, s: Settings, byKey: Map<String, AppEntry>, onOptions: (AppEntry) -> Unit) {
    val act = LocalContext.current.findActivity()
    val drag = LocalDrag.current
    val hover = if (drag.active) drag.hoverSlot() else null
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 6.dp).height(80.dp).glass(RoundedCornerShape(30.dp)).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        for (j in 0 until Settings.DOCK_SIZE) {
            val slot = Settings.DOCK_START + j
            val e = s.layout[slot]?.let(byKey::get)?.takeIf { it.key !in s.hidden && it.key !in s.vault }
            val glow by animateFloatAsState(if (hover == slot) 1f else 0f, spring(0.5f, 500f), label = "dg")
            Box(
                Modifier.weight(1f).fillMaxHeight()
                    .onGloballyPositioned { drag.targets[slot] = it },
                contentAlignment = Alignment.Center,
            ) {
                if (drag.active) Box(
                    Modifier.size(58.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.05f + 0.2f * glow))
                )
                if (e != null) AppIcon(
                    e, vm.repo, Modifier.graphicsLayer { alpha = if (drag.active && drag.fromSlot == slot) 0.25f else 1f },
                    size = 52.dp, showLabel = false, locked = e.key in s.locked,
                    canDrag = true, dragSlot = slot,
                    onClick = { b -> vm.launch(act, e, b) }, onLongClick = { onOptions(e) },
                )
            }
        }
    }
}

/** The icon under your finger + the "Remove" target at the top. */
@Composable
private fun DragLayer(drag: DragState, vm: LauncherViewModel) {
    val e = drag.entry
    val density = androidx.compose.ui.platform.LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            drag.active && drag.fromSlot != null,
            enter = slideInVertically { -it } + fadeIn(), exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            val over = drag.overRemove()
            val bg by animateColorAsState(if (over) Danger else Color(0xCC1F1B2C), label = "rm")
            val sc by animateFloatAsState(if (over) 1.12f else 1f, spring(0.4f, 600f), label = "rms")
            Row(
                Modifier.statusBarsPadding().padding(top = 14.dp)
                    .graphicsLayer { scaleX = sc; scaleY = sc }
                    .onGloballyPositioned { drag.removeZone = it }
                    .clip(RoundedCornerShape(24.dp)).background(bg).padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Close, null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Remove", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
        if (e != null) {
            val px = with(density) { 64.dp.roundToPx() }
            val bmp by produceState(vm.repo.cachedIcon(e.key), e.key) { value = vm.repo.icon(e, px) }
            val pop = remember { androidx.compose.animation.core.Animatable(0.8f) }
            LaunchedEffect(e.key) { pop.animateTo(1.18f, spring(0.45f, 500f)) }
            Box(
                Modifier
                    .offset { IntOffset((drag.pos.x - px / 2f).roundToInt(), (drag.pos.y - px / 2f).roundToInt()) }
                    .size(64.dp)
                    .graphicsLayer { scaleX = pop.value; scaleY = pop.value; shadowElevation = 24f; shape = RoundedCornerShape(20.dp); clip = false }
            ) { IconImage(e, bmp, 64.dp, false) }
        }
    }
}

@Composable
private fun BigClock() {
    val ctx = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (Motion.enabled) { now = System.currentTimeMillis(); delay(1000L - now % 1000L) } }
    val fmt = if (DateFormat.is24HourFormat(ctx)) "HH:mm" else "h:mm"
    val time = DateFormat.format(fmt, now).toString()
    val style = TextStyle(
        fontSize = 88.sp, fontWeight = FontWeight.ExtraLight, letterSpacing = (-2).sp, shadow = TextShadow,
        brush = Brush.linearGradient(listOf(Color.White, Color(0xFFD9CCFF), Color(0xFFBDEBFF))),
    )
    Row {
        time.forEachIndexed { i, ch ->
            AnimatedContent(
                targetState = ch, label = "digit$i",
                transitionSpec = {
                    (slideInVertically(spring(0.7f, 300f)) { it } + fadeIn()) togetherWith
                        (slideOutVertically(spring(0.7f, 300f)) { -it } + fadeOut())
                },
            ) { c -> Text(c.toString(), style = style) }
        }
    }
    Text(
        DateFormat.format("EEEE, MMMM d", now).toString(),
        style = TextStyle(color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium, shadow = TextShadow),
    )
}

@Composable
private fun Greeting(name: String) {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val g = when (h) { in 4..11 -> "Good morning"; in 12..16 -> "Good afternoon"; in 17..21 -> "Good evening"; else -> "Late night" }
    Text(
        if (name.isBlank()) g else "$g, $name",
        style = TextStyle(color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp, shadow = TextShadow),
        modifier = Modifier.padding(top = 2.dp),
    )
}

@Composable
private fun PrivacyChip(ps: PrivateSpace?, s: Settings, onClick: () -> Unit) {
    val glow by loop(0.4f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "glow")
    val secured = ps == null || ps.locked
    val text = buildString {
        append(if (ps == null) "Shield on" else if (ps.locked) "Private space locked" else "Private space open")
        if (s.locked.isNotEmpty()) append(" · ${s.locked.size} locked")
        if (s.hidden.isNotEmpty()) append(" · ${s.hidden.size} hidden")
    }
    Row(
        Modifier.glass(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(9.dp).graphicsLayer { alpha = glow }.clip(CircleShape).background(if (secured) Mint else Color(0xFFFFC857)))
        Spacer(Modifier.width(9.dp))
        Icon(if (secured) Icons.Rounded.Lock else Icons.Rounded.LockOpen, null, Modifier.size(16.dp), tint = Color.White)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun SwipeHint() {
    val dy by loop(0f, -8f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "dy")
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.KeyboardArrowUp, null, tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.graphicsLayer { translationY = dy.dp.toPx() })
    }
}

// ======================================================================
// DRAWER
// ======================================================================
@Composable
private fun Drawer(
    vm: LauncherViewModel, apps: AppsState, s: Settings, visible: List<AppEntry>, isDefault: Boolean,
    searchMode: Boolean, onSearchMode: (Boolean) -> Unit,
    onOptions: (AppEntry) -> Unit, onPrivacy: () -> Unit, onOpenVault: () -> Unit,
    conn: NestedScrollConnection, modifier: Modifier,
) {
    val act = LocalContext.current.findActivity()
    val scope = rememberCoroutineScope()
    val pages = remember(visible) {
        Category.entries.filter { c -> c == Category.ALL || visible.any { it.category == c } }.map<Category, Page> { Page.Cat(it) }
    }
    val pager = rememberPagerState { pages.size }
    val current = pages.getOrNull(pager.currentPage) ?: pages.first()
    var query by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize().statusBarsPadding()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 24.dp, end = 10.dp, top = 22.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1f)) {
                AnimatedContent(searchMode, label = "hdr") { sm ->
                    if (sm) SearchField(query, onChange = { q ->
                        if (s.secretCode.isNotBlank() && q.trim().equals(s.secretCode, true)) {
                            query = ""; onSearchMode(false); onOpenVault()
                        } else query = q
                    }, onBack = { query = ""; onSearchMode(false) }, onGo = {
                        searchResults(query, visible, apps, s).firstOrNull()?.let { vm.launch(act, it) }
                    })
                    else AnimatedContent(
                        current.title(), label = "title",
                        transitionSpec = {
                            (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut())
                        },
                    ) { t ->
                        Text(t, color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                            style = TextStyle(shadow = TextShadow))
                    }
                }
            }
            if (!searchMode) IconButton({ onSearchMode(true) }) { Icon(Icons.Rounded.Search, "Search", tint = Color.White) }
            IconButton({ vm.openStore(act, false) }) { Icon(Icons.Rounded.ShoppingBag, "Store", tint = Color.White) }
            IconButton(onPrivacy) { Icon(Icons.Rounded.MoreVert, "Privacy", tint = Color.White) }
        }

        Box(Modifier.weight(1f).imePadding()) {
            if (searchMode) {
                val results = remember(query, visible, apps, s) { searchResults(query, visible, apps, s) }
                AppGrid(results, s, vm, onOptions, conn, animateKey = query)
            } else {
                HorizontalPager(pager, beyondViewportPageCount = 1) { i ->
                    when (val p = pages.getOrNull(i)) {
                        null -> {}
                        is Page.Cat -> AppGrid(
                            if (p.c == Category.ALL) visible else visible.filter { it.category == p.c },
                            s, vm, onOptions, conn, animateKey = p,
                        )
                        Page.Private -> {}
                    }
                }
            }
        }

        if (!searchMode) {
            CategoryBar(pages, pager.currentPage) { scope.launch { pager.animateScrollToPage(it) } }
        }
    }
}

private fun searchResults(q: String, visible: List<AppEntry>, apps: AppsState, s: Settings): List<AppEntry> {
    val pool = ((if (s.hiddenInSearch) apps.apps.filter { it.key !in s.vault } else visible) +
        emptyList<AppEntry>()).distinctBy { it.key }
    val t = q.trim()
    if (t.isEmpty()) return pool
    return pool.filter { it.label.contains(t, true) }
        .sortedBy { if (it.label.startsWith(t, true)) 0 else 1 }
}

@Composable
private fun SearchField(q: String, onChange: (String) -> Unit, onBack: () -> Unit, onGo: () -> Unit) {
    val fr = remember { FocusRequester() }
    val kb = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(120); fr.requestFocus(); kb?.show() }
    Row(
        Modifier.fillMaxWidth().height(52.dp).glass(RoundedCornerShape(26.dp)).padding(start = 4.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onBack) { Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White) }
        Box(Modifier.weight(1f)) {
            if (q.isEmpty()) Text("Search apps…", color = Color.White.copy(alpha = 0.55f), fontSize = 17.sp)
            BasicTextField(
                q, onChange, singleLine = true, cursorBrush = SolidColor(Cyan),
                textStyle = TextStyle(color = Color.White, fontSize = 17.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { onGo() }),
                modifier = Modifier.fillMaxWidth().focusRequester(fr),
            )
        }
        if (q.isNotEmpty()) Icon(Icons.Rounded.Close, "Clear", tint = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.clickable { onChange("") })
    }
}

@Composable
private fun AppGrid(
    list: List<AppEntry>, s: Settings, vm: LauncherViewModel, onOptions: (AppEntry) -> Unit,
    conn: NestedScrollConnection, animateKey: Any?,
) {
    val act = LocalContext.current.findActivity()
    if (list.isEmpty()) {
        Box(Modifier.fillMaxSize().nestedScroll(conn), contentAlignment = Alignment.Center) {
            Text("Nothing here", color = Color.White.copy(alpha = 0.6f))
        }
        return
    }
    androidx.compose.runtime.key(animateKey) {
        LazyVerticalGrid(
            GridCells.Fixed(s.columns),
            Modifier.fillMaxSize().nestedScroll(conn),
            contentPadding = PaddingValues(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(list, key = { _, e -> e.key }) { i, e ->
                AppIcon(
                    e, vm.repo, size = if (s.columns >= 5) 50.dp else 58.dp,
                    locked = e.key in s.locked, enterIndex = if (i < 28) i else -1,
                    canDrag = e.profile != com.vyd.bastion.ProfileKind.PRIVATE,
                    onClick = { b -> vm.launch(act, e, b) }, onLongClick = { onOptions(e) },
                )
            }
        }
    }
}

@Composable
private fun CategoryBar(pages: List<Page>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 12.dp).fillMaxWidth()
            .glass(RoundedCornerShape(30.dp)).padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        pages.forEachIndexed { i, p ->
            val sel = i == selected
            val bg by animateColorAsState(if (sel) Color.White.copy(alpha = 0.22f) else Color.Transparent, label = "bg")
            val sc by animateFloatAsState(if (sel) 1.15f else 1f, spring(0.4f, 500f), label = "sc")
            val tint = if (p == Page.Private) Mint else Color.White
            Column(
                Modifier.clip(RoundedCornerShape(20.dp)).background(bg)
                    .clickable { onSelect(i) }.padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(p.icon(), p.title(), tint = tint, modifier = Modifier.size(24.dp).graphicsLayer { scaleX = sc; scaleY = sc })
                Spacer(Modifier.height(4.dp))
                Box(Modifier.size(width = if (sel) 18.dp else 0.dp, height = 3.dp).clip(CircleShape).background(tint))
            }
        }
    }
}

// ======================================================================
// PRIVATE SPACE PAGE (Android 15+ system Private Space)
// ======================================================================
@Composable
private fun PrivateSpacePage(
    ps: PrivateSpace, vm: LauncherViewModel, s: Settings, isDefault: Boolean,
    onOptions: (AppEntry) -> Unit, conn: NestedScrollConnection,
) {
    val act = LocalContext.current.findActivity()
    AnimatedContent(
        ps.locked, label = "ps",
        transitionSpec = { (fadeIn(tween(350)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(200)) },
    ) { locked ->
        if (locked) {
            Column(
                Modifier.fillMaxSize().nestedScroll(conn).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
            ) {
                ShieldPulse(64.dp, active = false, icon = Icons.Rounded.Lock)
                Spacer(Modifier.height(18.dp))
                Text("A space for your private apps", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Locked and invisible. Unlock with your fingerprint or PIN — it can take a few seconds to load.",
                    color = Color.White.copy(alpha = 0.75f), textAlign = TextAlign.Center, fontSize = 14.sp,
                )
                Spacer(Modifier.height(22.dp))
                Button(
                    onClick = { vm.setPrivateLocked(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Color.Black),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Icon(Icons.Rounded.LockOpen, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text("Unlock", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                if (!isDefault) {
                    Spacer(Modifier.height(14.dp))
                    Text("Bastion must be your default home app to unlock here.",
                        color = Color(0xFFFFC857), fontSize = 12.sp, textAlign = TextAlign.Center)
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) { AppGrid(ps.apps, s, vm, onOptions, conn, animateKey = "ps-open") }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                ) {
                    PsPill("Lock", Icons.Rounded.Lock, Mint) { vm.setPrivateLocked(true) }
                    PsPill("Install", Icons.Rounded.ShoppingBag, Color.White) { vm.openStore(act, true) }
                    PsPill("Settings", Icons.Rounded.Tune, Color.White) { vm.privateSpaceSettings(act) }
                }
            }
        }
    }
}

@Composable
private fun PsPill(text: String, icon: ImageVector, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.glass(RoundedCornerShape(20.dp)).clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = tint)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

// ======================================================================
// VAULT (Bastion's own encrypted-by-your-lock hidden folder)
// ======================================================================
@Composable
private fun VaultOverlay(
    vm: LauncherViewModel, apps: AppsState, s: Settings,
    onOptions: (AppEntry) -> Unit, onManage: () -> Unit,
) {
    val act = LocalContext.current.findActivity()
    val list = remember(apps, s.vault) { apps.apps.filter { it.key in s.vault } }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF180B3A), Color(0xFF060410))))
            .clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        AuroraBackground(Modifier.fillMaxSize().graphicsLayer { alpha = 0.6f })
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Vault", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold)
                    Text("Only you can open this.", color = Mint, fontSize = 14.sp)
                }
                IconButton(onManage) { Icon(Icons.Rounded.Settings, "Manage", tint = Color.White) }
                IconButton({ vm.vaultOpen.value = false }) { Icon(Icons.Rounded.Lock, "Lock vault", tint = Mint) }
            }
            if (list.isEmpty()) {
                Column(
                    Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    ShieldPulse(56.dp, active = true, icon = Icons.Rounded.VerifiedUser)
                    Spacer(Modifier.height(16.dp))
                    Text("Your vault is empty", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("Apps you move here disappear everywhere else.", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp)
                    Spacer(Modifier.height(18.dp))
                    Button(onManage, colors = ButtonDefaults.buttonColors(containerColor = Violet)) { Text("Add apps") }
                }
            } else {
                LazyVerticalGrid(
                    GridCells.Fixed(s.columns), Modifier.weight(1f),
                    contentPadding = PaddingValues(12.dp),
                ) {
                    itemsIndexed(list, key = { _, e -> e.key }) { i, e ->
                        AppIcon(e, vm.repo, enterIndex = i, locked = e.key in s.locked,
                            onClick = { b -> vm.launch(act, e, b) }, onLongClick = { onOptions(e) })
                    }
                }
            }
            Text(
                if (s.stealthVault) "Stealth on — type “${s.secretCode}” in search to come back."
                else "Tip: turn on Stealth in Privacy Center to hide the Vault entirely.",
                color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}

// ======================================================================
// PRIVATE SPACE OVERLAY — only reachable from ⋮ › Privacy Center
// ======================================================================
@Composable
private fun PrivateSpaceOverlay(
    vm: LauncherViewModel, ps: PrivateSpace?, s: Settings, isDefault: Boolean,
    onOptions: (AppEntry) -> Unit, onPrivacy: () -> Unit,
) {
    val act = LocalContext.current.findActivity()
    val noScroll = remember { object : NestedScrollConnection {} }
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0B1A24), Color(0xFF050A0E))))
            .clickable(remember { MutableInteractionSource() }, null) {},
    ) {
        AuroraBackground(Modifier.fillMaxSize().graphicsLayer { alpha = 0.45f })
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp, top = 22.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Text("Private space", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f), style = TextStyle(shadow = TextShadow))
                IconButton({ vm.openStore(act, true) }) { Icon(Icons.Rounded.ShoppingBag, "Install", tint = Color.White) }
                IconButton(onPrivacy) { Icon(Icons.Rounded.MoreVert, "Privacy", tint = Color.White) }
                IconButton({ vm.privateOpen.value = false }) { Icon(Icons.Rounded.Close, "Close", tint = Color.White) }
            }
            Box(Modifier.weight(1f)) {
                if (ps != null) PrivateSpacePage(ps, vm, s, isDefault, onOptions, noScroll)
                else Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                ) {
                    ShieldPulse(56.dp, active = false, icon = Icons.Rounded.Shield)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (!isDefault) "Set Bastion as your default home app to see your Private Space."
                        else "No Private Space yet. Create one in Settings › Security & privacy › Private space.",
                        color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        { if (!isDefault) vm.openHomeSettings(act) else vm.privateSpaceSettings(act) },
                        colors = ButtonDefaults.buttonColors(containerColor = Cream, contentColor = Color.Black),
                    ) { Text(if (!isDefault) "Set default home" else "Set up", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

// ======================================================================
// UPDATE BANNER
// ======================================================================
@Composable
private fun UpdateBanner() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by com.vyd.bastion.Updater.state.collectAsStateWithLifecycle()
    val shimmer by loop(-1f, 2f, infiniteRepeatable(tween(2200)), label = "sh")
    AnimatedVisibility(
        st !is com.vyd.bastion.Updater.State.Idle,
        enter = slideInVertically(spring(0.6f, 300f)) { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val (title, sub, prog) = when (val u = st) {
            is com.vyd.bastion.Updater.State.Available -> Triple("Update ${u.version} ready", "Tap to install", null)
            is com.vyd.bastion.Updater.State.Downloading ->
                Triple("Updating to ${u.version}", if (u.progress >= 1f) "Confirm the install…" else "${(u.progress * 100).toInt()}%", u.progress)
            is com.vyd.bastion.Updater.State.Failed -> Triple("Update failed", u.msg + " · tap to retry", null)
            else -> Triple("", "", null)
        }
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(Violet, Cyan), start = Offset(shimmer * 600f, 0f), end = Offset(shimmer * 600f + 900f, 300f)))
                .clickable {
                    when (val u = st) {
                        is com.vyd.bastion.Updater.State.Available ->
                            if (!com.vyd.bastion.Updater.canInstall(ctx)) {
                                android.widget.Toast.makeText(ctx, "Allow Bastion to install updates, then tap again", android.widget.Toast.LENGTH_LONG).show()
                                com.vyd.bastion.Updater.openInstallPermission(ctx)
                            } else scope.launch { com.vyd.bastion.Updater.downloadAndInstall(ctx, u) }
                        is com.vyd.bastion.Updater.State.Failed ->
                            scope.launch { com.vyd.bastion.Updater.check(ctx, force = true) }
                        else -> {}
                    }
                }
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(sub, color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                }
            }
            if (prog != null) {
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f))) {
                    Box(Modifier.fillMaxWidth(prog).height(5.dp).clip(CircleShape).background(Color.White))
                }
            }
        }
    }
}
