package com.vyd.bastion

import android.content.ComponentName
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.os.Build
import android.os.Process
import android.os.UserHandle
import android.os.UserManager
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator

enum class ProfileKind { MAIN, WORK, PRIVATE }

enum class Category(val title: String) {
    ALL("All apps"), CHAT("Chat"), SOCIAL("Social"), GAMES("Games"),
    MEDIA("Media"), WORK("Work & AI"), TOOLS("Tools")
}

data class AppEntry(
    val key: String,
    val label: String,
    val component: ComponentName,
    val user: UserHandle,
    val profile: ProfileKind,
    val category: Category,
    val info: LauncherActivityInfo,
) {
    val packageName: String get() = component.packageName
}

data class PrivateSpace(
    val user: UserHandle,
    val locked: Boolean,
    val apps: List<AppEntry>,
)

data class AppsState(
    val loaded: Boolean = false,
    val apps: List<AppEntry> = emptyList(),      // main + work profile
    val privateSpace: PrivateSpace? = null,      // null = not present / not visible to us
)

class AppRepository(private val ctx: Context, private val scope: CoroutineScope) {
    private val la = ctx.getSystemService(LauncherApps::class.java)
    private val um = ctx.getSystemService(UserManager::class.java)

    private val _state = MutableStateFlow(AppsState())
    val state: StateFlow<AppsState> = _state

    private val callback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = refresh()
        override fun onPackagesAvailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = refresh()
        override fun onPackagesUnavailable(p: Array<out String>?, u: UserHandle?, r: Boolean) = refresh()
    }

    init {
        la.registerCallback(callback)
    }

    fun close() = la.unregisterCallback(callback)

    fun refresh() {
        scope.launch { _state.value = withContext(Dispatchers.Default) { load() } }
    }

    fun userSerial(user: UserHandle): Long = um.getSerialNumberForUser(user)

    private fun kindOf(user: UserHandle): ProfileKind {
        if (user == Process.myUserHandle()) return ProfileKind.MAIN
        if (Build.VERSION.SDK_INT >= 35) {
            runCatching {
                if (la.getLauncherUserInfo(user)?.userType == UserManager.USER_TYPE_PROFILE_PRIVATE)
                    return ProfileKind.PRIVATE
            }
        }
        return ProfileKind.WORK
    }

    private fun load(): AppsState {
        val collator = Collator.getInstance()
        val main = mutableListOf<AppEntry>()
        var ps: PrivateSpace? = null
        for (user in runCatching { la.profiles }.getOrDefault(listOf(Process.myUserHandle()))) {
            val kind = kindOf(user)
            val quiet = if (kind == ProfileKind.MAIN) false else runCatching { um.isQuietModeEnabled(user) }.getOrDefault(false)
            val acts = if (kind == ProfileKind.PRIVATE && quiet) emptyList()
            else runCatching { la.getActivityList(null, user) }.getOrDefault(emptyList())
            val serial = userSerial(user)
            val entries = acts.filter { it.componentName.packageName != ctx.packageName }.map { a ->
                AppEntry(
                    key = "${a.componentName.flattenToShortString()}#$serial",
                    label = a.label?.toString()?.trim().orEmpty().ifEmpty { a.componentName.packageName },
                    component = a.componentName,
                    user = user,
                    profile = kind,
                    category = categorize(a),
                    info = a,
                )
            }.sortedWith { x, y -> collator.compare(x.label, y.label) }
            if (kind == ProfileKind.PRIVATE) ps = PrivateSpace(user, quiet, entries)
            else main += entries
        }
        main.sortWith { x, y -> collator.compare(x.label, y.label) }
        return AppsState(loaded = true, apps = main.distinctBy { it.key }, privateSpace = ps?.copy(apps = ps!!.apps.distinctBy { it.key }))
    }

    // ---------- Icons ----------
    private val icons = LruCache<String, ImageBitmap>(400)
    fun cachedIcon(key: String): ImageBitmap? = icons.get(key)
    suspend fun icon(entry: AppEntry, sizePx: Int): ImageBitmap = icons.get(entry.key) ?: withContext(Dispatchers.Default) {
        val d = runCatching { entry.info.getBadgedIcon(0) }.getOrNull()
            ?: ctx.packageManager.defaultActivityIcon
        val bmp = d.toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).asImageBitmap()
        icons.put(entry.key, bmp); bmp
    }

    // ---------- Smart categories ----------
    private fun categorize(a: LauncherActivityInfo): Category {
        val p = a.componentName.packageName.lowercase()
        val l = (a.label?.toString() ?: "").lowercase()
        fun any(vararg k: String) = k.any { p.contains(it) || l.contains(it) }
        if (any("messag", "sms", "mms", "whatsapp", "telegram", "thoughtcrime", "signal", "discord", "dialer",
                "contacts", "gmail", "android.gm", "email", "outlook", "slack", "teams", "groupme", "jp.naver.line", "viber", "wechat"))
            return Category.CHAT
        if (any("instagram", "facebook", "twitter", "reddit", "tiktok", "musically", "snapchat", "threads",
                "linkedin", "pinterest", "bluesky", "tumblr", "mastodon"))
            return Category.SOCIAL
        if (any("claude", "anthropic", "openai", "chatgpt", "gemini", "bard", "copilot", "perplexity", "grok", "venice",
                "docs", "sheets", "slides", "drive", "office", "word", "excel", "notion", "keep", "calendar",
                "github", "code", "termux", "javascript", "acode", "trello", "todoist", "zoom", "meet"))
            return Category.WORK
        if (any("youtube", "spotify", "netflix", "music", "photos", "camera", "gallery", "video", "crunchyroll", "hulu",
                "disney", "primevideo", "plex", "podcast", "twitch", "pandora", "audible", "kindle", "tubi", "wbd.stream"))
            return Category.MEDIA
        if (any("game", "roblox", "minecraft", "mojang", "steam", "xbox", "playstation", "supercell", "epicgames", "chess"))
            return Category.GAMES
        return when (a.applicationInfo.category) {
            ApplicationInfo.CATEGORY_GAME -> Category.GAMES
            ApplicationInfo.CATEGORY_SOCIAL -> Category.SOCIAL
            ApplicationInfo.CATEGORY_AUDIO, ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_IMAGE, ApplicationInfo.CATEGORY_NEWS -> Category.MEDIA
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> Category.WORK
            else -> Category.TOOLS
        }
    }
}
