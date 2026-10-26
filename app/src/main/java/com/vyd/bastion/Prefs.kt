package com.vyd.bastion

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val hidden: Set<String> = emptySet(),
    val locked: Set<String> = emptySet(),
    val vault: Set<String> = emptySet(),
    /** Slot -> app key. 0..29 = home grid cells, 100..104 = dock. */
    val layout: Map<Int, String> = emptyMap(),
    val stealthVault: Boolean = false,
    val secretCode: String = "vault",
    val autoLock: Boolean = true,
    val secureScreen: Boolean = false,
    val hiddenInSearch: Boolean = false,
    val columns: Int = 4,
    val name: String = "David",
    val lockPrivacyCenter: Boolean = false,
) {
    val dock: List<String> get() = (DOCK_START until DOCK_START + DOCK_SIZE).mapNotNull { layout[it] }
    companion object { const val DOCK_START = 100; const val DOCK_SIZE = 5 }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("bastion", Context.MODE_PRIVATE)
    private val _s = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _s

    private fun read() = Settings(
        hidden = sp.getStringSet("hidden", emptySet())!!.toSet(),
        locked = sp.getStringSet("locked", emptySet())!!.toSet(),
        vault = sp.getStringSet("vault", emptySet())!!.toSet(),
        layout = readLayout(),
        stealthVault = sp.getBoolean("stealth", false),
        secretCode = sp.getString("secret", "vault")!!,
        autoLock = sp.getBoolean("autoLock", true),
        secureScreen = sp.getBoolean("secure", false),
        hiddenInSearch = sp.getBoolean("hiddenSearch", false),
        columns = sp.getInt("columns", 4),
        name = sp.getString("name", "David")!!,
        lockPrivacyCenter = sp.getBoolean("lockPC", false),
    )

    private fun readLayout(): Map<Int, String> {
        val raw = sp.getString("layout", null)
        if (raw == null) { // migrate the old dock list
            return sp.getString("dock", "")!!.split('|').filter { it.isNotBlank() }.take(Settings.DOCK_SIZE)
                .mapIndexed { i, k -> Settings.DOCK_START + i to k }.toMap()
        }
        return raw.split(';').mapNotNull { e ->
            val i = e.indexOf('='); if (i <= 0) null
            else e.substring(0, i).toIntOrNull()?.let { it to e.substring(i + 1) }
        }.toMap()
    }

    fun update(block: (Settings) -> Settings) {
        val n = block(_s.value)
        _s.value = n
        sp.edit()
            .putStringSet("hidden", n.hidden)
            .putStringSet("locked", n.locked)
            .putStringSet("vault", n.vault)
            .putString("layout", n.layout.entries.joinToString(";") { "${it.key}=${it.value}" })
            .putBoolean("stealth", n.stealthVault)
            .putString("secret", n.secretCode)
            .putBoolean("autoLock", n.autoLock)
            .putBoolean("secure", n.secureScreen)
            .putBoolean("hiddenSearch", n.hiddenInSearch)
            .putInt("columns", n.columns)
            .putString("name", n.name)
            .putBoolean("lockPC", n.lockPrivacyCenter)
            .apply()
    }
}
