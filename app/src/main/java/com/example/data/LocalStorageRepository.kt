package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.CharacterCatalog
import com.example.model.GameMapId
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.model.OutfitConfig
import com.example.model.PlayerStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "last_survivor_offline_prefs")

data class PersistedProfile(
    val selectedCharacterId: String = "kai",
    val selectedMapId: GameMapId = GameMapId.EMERALD_ISLE,
    val selectedGameMode: GameMode = GameMode.SOLO_SURVIVAL,
    val outfit: OutfitConfig = OutfitConfig(),
    val settings: GameSettings = GameSettings(),
    val stats: PlayerStats = PlayerStats()
)

class LocalStorageRepository(private val context: Context) {

    private object Keys {
        val CHARACTER_ID = stringPreferencesKey("selected_character_id")
        val MAP_ID = stringPreferencesKey("selected_map_id")
        val GAME_MODE = stringPreferencesKey("selected_game_mode")

        val HAIR_STYLE = intPreferencesKey("outfit_hair")
        val MASK_STYLE = intPreferencesKey("outfit_mask")
        val JACKET_STYLE = intPreferencesKey("outfit_jacket")
        val BOOTS_STYLE = intPreferencesKey("outfit_boots")
        val GLOVES_STYLE = intPreferencesKey("outfit_gloves")
        val BACKPACK_STYLE = intPreferencesKey("outfit_backpack")

        val GRAPHICS_QUALITY = stringPreferencesKey("settings_graphics")
        val MUSIC_VOLUME = floatPreferencesKey("settings_music_vol")
        val SFX_VOLUME = floatPreferencesKey("settings_sfx_vol")
        val JOYSTICK_SCALE = floatPreferencesKey("settings_joystick_scale")
        val CAMERA_SENS = floatPreferencesKey("settings_camera_sens")
        val BUTTON_OPACITY = floatPreferencesKey("settings_button_opacity")
        val AUTO_SPRINT = booleanPreferencesKey("settings_auto_sprint")
        val INVERT_Y = booleanPreferencesKey("settings_invert_y")

        val MATCHES_PLAYED = intPreferencesKey("stats_matches")
        val VICTORIES = intPreferencesKey("stats_wins")
        val TOTAL_ELIMS = intPreferencesKey("stats_elims")
        val BEST_SURVIVAL = intPreferencesKey("stats_best_survival")
        val UNLOCKED_IDS = stringSetPreferencesKey("stats_unlocked_ids")
    }

    val profileFlow: Flow<PersistedProfile> = context.dataStore.data.map { prefs ->
        val mapId = prefs[Keys.MAP_ID]?.let { name ->
            GameMapId.entries.firstOrNull { it.name == name }
        } ?: GameMapId.EMERALD_ISLE

        val mode = prefs[Keys.GAME_MODE]?.let { name ->
            GameMode.entries.firstOrNull { it.name == name }
        } ?: GameMode.SOLO_SURVIVAL

        val quality = prefs[Keys.GRAPHICS_QUALITY]?.let { name ->
            GraphicsQuality.entries.firstOrNull { it.name == name }
        } ?: GraphicsQuality.MEDIUM

        val wins = prefs[Keys.VICTORIES] ?: 0
        val defaultUnlocked = buildSet {
            add("kai")
            add("nova")
            if (wins >= 1) add("rex")
            if (wins >= 2) add("mira")
        }
        val unlocked = (prefs[Keys.UNLOCKED_IDS] ?: defaultUnlocked) + defaultUnlocked

        PersistedProfile(
            selectedCharacterId = prefs[Keys.CHARACTER_ID] ?: "kai",
            selectedMapId = mapId,
            selectedGameMode = mode,
            outfit = OutfitConfig(
                hairStyle = prefs[Keys.HAIR_STYLE] ?: 0,
                maskStyle = prefs[Keys.MASK_STYLE] ?: 1,
                jacketStyle = prefs[Keys.JACKET_STYLE] ?: 0,
                bootsStyle = prefs[Keys.BOOTS_STYLE] ?: 0,
                glovesStyle = prefs[Keys.GLOVES_STYLE] ?: 0,
                backpackStyle = prefs[Keys.BACKPACK_STYLE] ?: 1
            ),
            settings = GameSettings(
                graphicsQuality = quality,
                musicVolume = prefs[Keys.MUSIC_VOLUME] ?: 0.65f,
                sfxVolume = prefs[Keys.SFX_VOLUME] ?: 0.85f,
                joystickScale = prefs[Keys.JOYSTICK_SCALE] ?: 1.0f,
                cameraSensitivity = prefs[Keys.CAMERA_SENS] ?: 1.0f,
                buttonOpacity = prefs[Keys.BUTTON_OPACITY] ?: 0.85f,
                autoSprint = prefs[Keys.AUTO_SPRINT] ?: false,
                invertY = prefs[Keys.INVERT_Y] ?: false
            ),
            stats = PlayerStats(
                matchesPlayed = prefs[Keys.MATCHES_PLAYED] ?: 0,
                victories = wins,
                totalEliminations = prefs[Keys.TOTAL_ELIMS] ?: 0,
                bestSurvivalSeconds = prefs[Keys.BEST_SURVIVAL] ?: 0,
                unlockedIds = unlocked
            )
        )
    }

    suspend fun saveCharacter(characterId: String) {
        context.dataStore.edit { it[Keys.CHARACTER_ID] = characterId }
    }

    suspend fun saveMap(mapId: GameMapId) {
        context.dataStore.edit { it[Keys.MAP_ID] = mapId.name }
    }

    suspend fun saveGameMode(mode: GameMode) {
        context.dataStore.edit { it[Keys.GAME_MODE] = mode.name }
    }

    suspend fun saveOutfit(outfit: OutfitConfig) {
        context.dataStore.edit {
            it[Keys.HAIR_STYLE] = outfit.hairStyle
            it[Keys.MASK_STYLE] = outfit.maskStyle
            it[Keys.JACKET_STYLE] = outfit.jacketStyle
            it[Keys.BOOTS_STYLE] = outfit.bootsStyle
            it[Keys.GLOVES_STYLE] = outfit.glovesStyle
            it[Keys.BACKPACK_STYLE] = outfit.backpackStyle
        }
    }

    suspend fun saveSettings(settings: GameSettings) {
        context.dataStore.edit {
            it[Keys.GRAPHICS_QUALITY] = settings.graphicsQuality.name
            it[Keys.MUSIC_VOLUME] = settings.musicVolume
            it[Keys.SFX_VOLUME] = settings.sfxVolume
            it[Keys.JOYSTICK_SCALE] = settings.joystickScale
            it[Keys.CAMERA_SENS] = settings.cameraSensitivity
            it[Keys.BUTTON_OPACITY] = settings.buttonOpacity
            it[Keys.AUTO_SPRINT] = settings.autoSprint
            it[Keys.INVERT_Y] = settings.invertY
        }
    }

    suspend fun recordMatchResult(
        isVictory: Boolean,
        eliminations: Int,
        survivalSeconds: Int,
        isPractice: Boolean
    ): List<String> {
        val newlyUnlocked = mutableListOf<String>()
        context.dataStore.edit { prefs ->
            val prevMatches = prefs[Keys.MATCHES_PLAYED] ?: 0
            val prevWins = prefs[Keys.VICTORIES] ?: 0
            val prevElims = prefs[Keys.TOTAL_ELIMS] ?: 0
            val prevBest = prefs[Keys.BEST_SURVIVAL] ?: 0
            val prevUnlocked = prefs[Keys.UNLOCKED_IDS] ?: setOf("kai", "nova")

            val newWins = if (!isPractice && isVictory) prevWins + 1 else prevWins
            val newElims = prevElims + eliminations
            val updatedUnlocked = prevUnlocked.toMutableSet()

            for (char in CharacterCatalog.all) {
                if (char.id !in updatedUnlocked &&
                    (newWins >= char.unlockWinsRequired || newElims >= char.unlockWinsRequired * 4)
                ) {
                    updatedUnlocked.add(char.id)
                    newlyUnlocked.add("${char.name} (${char.codename})")
                }
            }

            prefs[Keys.MATCHES_PLAYED] = prevMatches + 1
            prefs[Keys.VICTORIES] = newWins
            prefs[Keys.TOTAL_ELIMS] = newElims
            prefs[Keys.BEST_SURVIVAL] = maxOf(prevBest, survivalSeconds)
            prefs[Keys.UNLOCKED_IDS] = updatedUnlocked
        }
        return newlyUnlocked
    }
}
