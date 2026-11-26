package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.MusicTrackMode
import com.example.audio.SoundEngine
import com.example.data.LocalStorageRepository
import com.example.data.PersistedProfile
import com.example.engine.ArenaRenderer
import com.example.engine.GameSimulation
import com.example.engine.WorldMap
import com.example.model.GameMapId
import com.example.model.GameMode
import com.example.model.GameScreen
import com.example.model.GameSettings
import com.example.model.HudSnapshot
import com.example.model.OutfitConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameUiState(
    val screen: GameScreen = GameScreen.LOBBY,
    val profile: PersistedProfile = PersistedProfile(),
    val hud: HudSnapshot = HudSnapshot(),
    val newlyUnlocked: List<String> = emptyList(),
    val showExitDialog: Boolean = false
)

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LocalStorageRepository(application.applicationContext)
    val soundEngine = SoundEngine(application.applicationContext)
    val simulation = GameSimulation(soundEngine)

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    val renderer = ArenaRenderer(simulation) {
        onSimulationHudTick()
    }

    private var matchResultRecorded = false

    init {
        soundEngine.initialize()
        soundEngine.setTrackMode(MusicTrackMode.MENU)

        viewModelScope.launch {
            repository.profileFlow.collect { profile ->
                _uiState.update { it.copy(profile = profile) }
                soundEngine.musicVolume = profile.settings.musicVolume
                soundEngine.sfxVolume = profile.settings.sfxVolume
                renderer.lobbyCharacterId = profile.selectedCharacterId
                renderer.lobbyOutfit = profile.outfit
                renderer.requestMapAndQuality(profile.selectedMapId, profile.settings.graphicsQuality)
            }
        }
    }

    private fun onSimulationHudTick() {
        val snapshot = simulation.buildHudSnapshot()
        if (snapshot.matchFinished && !matchResultRecorded) {
            matchResultRecorded = true
            val isPractice = simulation.gameMode == GameMode.PRACTICE
            soundEngine.setTrackMode(if (snapshot.isVictory) MusicTrackMode.VICTORY else MusicTrackMode.MENU)
            if (snapshot.isVictory) {
                soundEngine.playElimination()
            }
            renderer.isLobbyScene = true
            renderer.isVictoryScene = snapshot.isVictory

            viewModelScope.launch {
                val unlocked = repository.recordMatchResult(
                    isVictory = snapshot.isVictory,
                    eliminations = snapshot.eliminations,
                    survivalSeconds = snapshot.matchSeconds,
                    isPractice = isPractice
                )
                _uiState.update {
                    it.copy(
                        screen = GameScreen.MATCH_RESULT,
                        hud = snapshot,
                        newlyUnlocked = unlocked
                    )
                }
            }
        } else {
            _uiState.update { it.copy(hud = snapshot) }
        }
    }

    fun navigateTo(screen: GameScreen) {
        soundEngine.playUiClick()
        renderer.isVictoryScene = false
        renderer.isLobbyScene = screen != GameScreen.IN_GAME && screen != GameScreen.PAUSED
        renderer.isPaused = screen == GameScreen.PAUSED
        if (screen == GameScreen.LOBBY) {
            soundEngine.setTrackMode(MusicTrackMode.MENU)
        }
        _uiState.update { it.copy(screen = screen) }
    }

    fun selectGameMode(mode: GameMode) {
        soundEngine.playUiClick()
        viewModelScope.launch { repository.saveGameMode(mode) }
    }

    fun selectCharacter(characterId: String) {
        val unlocked = _uiState.value.profile.stats.unlockedIds
        if (characterId !in unlocked) return
        soundEngine.playUiClick()
        renderer.lobbyCharacterId = characterId
        viewModelScope.launch { repository.saveCharacter(characterId) }
    }

    fun updateOutfit(transform: (OutfitConfig) -> OutfitConfig) {
        soundEngine.playUiClick()
        val updated = transform(_uiState.value.profile.outfit)
        renderer.lobbyOutfit = updated
        viewModelScope.launch { repository.saveOutfit(updated) }
    }

    fun selectMap(mapId: GameMapId) {
        soundEngine.playUiClick()
        renderer.requestMapAndQuality(mapId, _uiState.value.profile.settings.graphicsQuality)
        viewModelScope.launch { repository.saveMap(mapId) }
    }

    fun updateSettings(transform: (GameSettings) -> GameSettings) {
        val updated = transform(_uiState.value.profile.settings)
        soundEngine.musicVolume = updated.musicVolume
        soundEngine.sfxVolume = updated.sfxVolume
        renderer.requestMapAndQuality(_uiState.value.profile.selectedMapId, updated.graphicsQuality)
        viewModelScope.launch { repository.saveSettings(updated) }
    }

    fun startMatch() {
        soundEngine.playUiClick()
        val profile = _uiState.value.profile
        val map = WorldMap(profile.selectedMapId).also {
            it.generateLayoutOnly() // pure CPU colliders & loot metadata for simulation
        }
        matchResultRecorded = false
        simulation.startNewMatch(
            map = map,
            mode = profile.selectedGameMode,
            characterId = profile.selectedCharacterId,
            outfit = profile.outfit,
            gameSettings = profile.settings
        )
        renderer.isVictoryScene = false
        renderer.isPaused = false
        renderer.isLobbyScene = false
        renderer.requestMapAndQuality(profile.selectedMapId, profile.settings.graphicsQuality)
        soundEngine.setTrackMode(MusicTrackMode.ENVIRONMENT)
        _uiState.update {
            it.copy(
                screen = GameScreen.IN_GAME,
                hud = simulation.buildHudSnapshot(),
                newlyUnlocked = emptyList()
            )
        }
    }

    fun pauseMatch() {
        if (_uiState.value.screen != GameScreen.IN_GAME) return
        soundEngine.playUiClick()
        renderer.isPaused = true
        _uiState.update { it.copy(screen = GameScreen.PAUSED) }
    }

    fun resumeMatch() {
        soundEngine.playUiClick()
        renderer.isPaused = false
        _uiState.update { it.copy(screen = GameScreen.IN_GAME) }
    }

    fun endPracticeSession() {
        simulation.finishPracticeMatch()
        onSimulationHudTick()
    }

    fun returnToLobby() {
        soundEngine.playUiClick()
        renderer.isPaused = false
        renderer.isVictoryScene = false
        renderer.isLobbyScene = true
        soundEngine.setTrackMode(MusicTrackMode.MENU)
        _uiState.update { it.copy(screen = GameScreen.LOBBY) }
    }

    fun setShowExitDialog(show: Boolean) {
        soundEngine.playUiClick()
        _uiState.update { it.copy(showExitDialog = show) }
    }

    fun onAppPause() {
        soundEngine.onPause()
        if (_uiState.value.screen == GameScreen.IN_GAME) {
            renderer.isPaused = true
            _uiState.update { it.copy(screen = GameScreen.PAUSED) }
        }
    }

    fun onAppResume() {
        soundEngine.onResume()
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.release()
    }
}
