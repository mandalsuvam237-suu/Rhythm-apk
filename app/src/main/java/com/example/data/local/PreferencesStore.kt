package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.FontChoice
import com.example.data.model.ThemePalette
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.levelUpDataStore: DataStore<Preferences> by preferencesDataStore(name = "level_up_prefs")

data class VisualPreferences(
    val themePalette: ThemePalette = ThemePalette.LIGHT,
    val fontChoice: FontChoice = FontChoice.PLUS_JAKARTA,
    val compactCards: Boolean = false,
    val lastSyncEpochMs: Long = 0L,
    val seededOnce: Boolean = false
)

data class SecurityPreferences(
    val lockOnStartup: Boolean = false,
    val lockSensitiveCategories: Boolean = true,
    val sensitiveCategoryIds: Set<String> = DEFAULT_SENSITIVE_CATEGORY_IDS,
    val backupPasscodeHash: String = ""
) {
    val hasBackupPasscode: Boolean
        get() = backupPasscodeHash.isNotBlank()

    companion object {
        val DEFAULT_SENSITIVE_CATEGORY_IDS: Set<String> = setOf("cat_personal", "cat_family")
    }
}

class PreferencesStore(private val context: Context) {

    private object Keys {
        val THEME_PALETTE = stringPreferencesKey("theme_palette")
        val FONT_CHOICE = stringPreferencesKey("font_choice")
        val COMPACT_CARDS = booleanPreferencesKey("compact_cards")
        val LAST_SYNC_MS = longPreferencesKey("last_sync_ms")
        val SEEDED_ONCE = booleanPreferencesKey("seeded_once")
        val LOCK_ON_STARTUP = booleanPreferencesKey("lock_on_startup")
        val LOCK_SENSITIVE_CATEGORIES = booleanPreferencesKey("lock_sensitive_categories")
        val SENSITIVE_CATEGORY_IDS = stringSetPreferencesKey("sensitive_category_ids")
        val BACKUP_PASSCODE_HASH = stringPreferencesKey("backup_passcode_hash")
    }

    val visualPreferences: Flow<VisualPreferences> = context.levelUpDataStore.data.map { prefs ->
        val palette = prefs[Keys.THEME_PALETTE]?.let {
            runCatching { ThemePalette.valueOf(it) }.getOrNull()
        } ?: ThemePalette.LIGHT
        val font = prefs[Keys.FONT_CHOICE]?.let {
            runCatching { FontChoice.valueOf(it) }.getOrNull()
        } ?: FontChoice.PLUS_JAKARTA
        VisualPreferences(
            themePalette = palette,
            fontChoice = font,
            compactCards = prefs[Keys.COMPACT_CARDS] ?: false,
            lastSyncEpochMs = prefs[Keys.LAST_SYNC_MS] ?: 0L,
            seededOnce = prefs[Keys.SEEDED_ONCE] ?: false
        )
    }

    val securityPreferences: Flow<SecurityPreferences> = context.levelUpDataStore.data.map { prefs ->
        SecurityPreferences(
            lockOnStartup = prefs[Keys.LOCK_ON_STARTUP] ?: false,
            lockSensitiveCategories = prefs[Keys.LOCK_SENSITIVE_CATEGORIES] ?: true,
            sensitiveCategoryIds = prefs[Keys.SENSITIVE_CATEGORY_IDS]
                ?: SecurityPreferences.DEFAULT_SENSITIVE_CATEGORY_IDS,
            backupPasscodeHash = prefs[Keys.BACKUP_PASSCODE_HASH] ?: ""
        )
    }

    suspend fun setThemePalette(palette: ThemePalette) {
        context.levelUpDataStore.edit { it[Keys.THEME_PALETTE] = palette.name }
    }

    suspend fun setFontChoice(font: FontChoice) {
        context.levelUpDataStore.edit { it[Keys.FONT_CHOICE] = font.name }
    }

    suspend fun setCompactCards(compact: Boolean) {
        context.levelUpDataStore.edit { it[Keys.COMPACT_CARDS] = compact }
    }

    suspend fun setLastSyncEpochMs(epochMs: Long) {
        context.levelUpDataStore.edit { it[Keys.LAST_SYNC_MS] = epochMs }
    }

    suspend fun markSeeded() {
        context.levelUpDataStore.edit { it[Keys.SEEDED_ONCE] = true }
    }

    suspend fun setLockOnStartup(enabled: Boolean) {
        context.levelUpDataStore.edit { it[Keys.LOCK_ON_STARTUP] = enabled }
    }

    suspend fun setLockSensitiveCategories(enabled: Boolean) {
        context.levelUpDataStore.edit { it[Keys.LOCK_SENSITIVE_CATEGORIES] = enabled }
    }

    suspend fun setSensitiveCategoryIds(ids: Set<String>) {
        context.levelUpDataStore.edit { it[Keys.SENSITIVE_CATEGORY_IDS] = ids }
    }

    suspend fun toggleSensitiveCategory(categoryId: String) {
        context.levelUpDataStore.edit { prefs ->
            val current = prefs[Keys.SENSITIVE_CATEGORY_IDS]
                ?: SecurityPreferences.DEFAULT_SENSITIVE_CATEGORY_IDS
            prefs[Keys.SENSITIVE_CATEGORY_IDS] = if (categoryId in current) {
                current - categoryId
            } else {
                current + categoryId
            }
        }
    }

    suspend fun setBackupPasscodeHash(hash: String) {
        context.levelUpDataStore.edit { it[Keys.BACKUP_PASSCODE_HASH] = hash }
    }
}
