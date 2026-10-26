package com.vyd.bastion

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Settings(
    val hidden: Set<String> = emptySet(),
    val locked: Set<String> = emptySet(),
    val vault: Set<String> = emptySet(),
    val dock: List<String> = emptyList(),
    val stealthVault: Boolean = false,
    val secretCode: String = "vault",
    val autoLock: Boolean = true,
    val secureScreen: Boolean = false,
    val hiddenInSearch: Boolean = false,
    val columns: Int = 4,
    val name: String = "David",
    val lockPrivacyCenter: Boolean = false,
)

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("bastion", Context.MODE_PRIVATE)
    private val _s = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _s

    private fun read() = Settings(
        hidden = sp.getStringSet("hidden", emptySet())!!.toSet(),
        locked = sp.getStringSet("locked", emptySet())!!.toSet(),
        vault = sp.getStringSet("vault", emptySet())!!.toSet(),
        dock = sp.getString("dock", "")!!.split('|').filter { it.isNotBlank() },
        stealthVault = sp.getBoolean("stealth", false),
        secretCode = sp.getString("secret", "vault")!!,
        autoLock = sp.getBoolean("autoLock", true),
        secureScreen = sp.getBoolean("secure", false),
        hiddenInSearch = sp.getBoolean("hiddenSearch", false),
        columns = sp.getInt("columns", 4),
        name = sp.getString("name", "David")!!,
        lockPrivacyCenter = sp.getBoolean("lockPC", false),
    )

    fun update(block: (Settings) -> Settings) {
        val n = block(_s.value)
        _s.value = n
        sp.edit()
            .putStringSet("hidden", n.hidden)
            .putStringSet("locked", n.locked)
            .putStringSet("vault", n.vault)
            .putString("dock", n.dock.joinToString("|"))
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
