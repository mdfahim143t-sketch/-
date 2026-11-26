package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.ArenaGLSurfaceView
import com.example.model.GameScreen
import com.example.ui.screens.CharacterSelectScreen
import com.example.ui.screens.ExitConfirmationDialog
import com.example.ui.screens.GameplayHud
import com.example.ui.screens.MainMenuOverlay
import com.example.ui.screens.MapSelectScreen
import com.example.ui.screens.MatchResultOverlay
import com.example.ui.screens.OutfitCustomizeScreen
import com.example.ui.screens.PauseMenuOverlay
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LastSurvivorTheme
import com.example.viewmodel.GameViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GameViewModel by viewModels()
    private var glSurfaceView: ArenaGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LastSurvivorTheme {
                LastSurvivorApp(
                    viewModel = viewModel,
                    onSurfaceCreated = { view -> glSurfaceView = view },
                    onExitApp = { finishAffinity() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView?.onResume()
        viewModel.onAppResume()
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView?.onPause()
        viewModel.onAppPause()
    }
}

@Composable
fun LastSurvivorApp(
    viewModel: GameViewModel,
    onSurfaceCreated: (ArenaGLSurfaceView) -> Unit,
    onExitApp: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var returnFromSettings by remember { mutableStateOf(GameScreen.LOBBY) }
    var glViewRef by remember { mutableStateOf<ArenaGLSurfaceView?>(null) }

    LaunchedEffect(uiState.profile.settings.graphicsQuality, glViewRef) {
        glViewRef?.applyQuality(uiState.profile.settings.graphicsQuality)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Persistent Live 3D OpenGL ES Viewport (renders Lobby Vista or Active Open-World Island)
        AndroidView(
            factory = { ctx ->
                ArenaGLSurfaceView(ctx, viewModel.renderer).also { view ->
                    view.applyQuality(uiState.profile.settings.graphicsQuality)
                    glViewRef = view
                    onSurfaceCreated(view)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // Responsive Compose UI Layer
        when (uiState.screen) {
            GameScreen.LOBBY -> {
                MainMenuOverlay(
                    profile = uiState.profile,
                    onRotateCharacter = { dx -> viewModel.simulation.rotateLobby(dx) },
                    onSelectMode = { mode -> viewModel.selectGameMode(mode) },
                    onDeploy = { viewModel.startMatch() },
                    onNavigate = { target ->
                        if (target == GameScreen.SETTINGS) {
                            returnFromSettings = GameScreen.LOBBY
                        }
                        viewModel.navigateTo(target)
                    },
                    onRequestExit = { viewModel.setShowExitDialog(true) }
                )
            }

            GameScreen.CHARACTER_SELECT -> {
                CharacterSelectScreen(
                    profile = uiState.profile,
                    onRotateCharacter = { dx -> viewModel.simulation.rotateLobby(dx) },
                    onSelectCharacter = { id -> viewModel.selectCharacter(id) },
                    onBack = { viewModel.navigateTo(GameScreen.LOBBY) }
                )
            }

            GameScreen.OUTFIT_CUSTOMIZE -> {
                OutfitCustomizeScreen(
                    outfit = uiState.profile.outfit,
                    onRotateCharacter = { dx -> viewModel.simulation.rotateLobby(dx) },
                    onUpdateOutfit = { fn -> viewModel.updateOutfit(fn) },
                    onBack = { viewModel.navigateTo(GameScreen.LOBBY) }
                )
            }

            GameScreen.MAP_SELECT -> {
                MapSelectScreen(
                    selectedMap = uiState.profile.selectedMapId,
                    onSelectMap = { mapId -> viewModel.selectMap(mapId) },
                    onBack = { viewModel.navigateTo(GameScreen.LOBBY) }
                )
            }

            GameScreen.SETTINGS -> {
                SettingsScreen(
                    settings = uiState.profile.settings,
                    onUpdateSettings = { fn -> viewModel.updateSettings(fn) },
                    onBack = {
                        if (returnFromSettings == GameScreen.PAUSED) {
                            viewModel.pauseMatch()
                        } else {
                            viewModel.navigateTo(GameScreen.LOBBY)
                        }
                    }
                )
            }

            GameScreen.IN_GAME -> {
                GameplayHud(
                    hud = uiState.hud,
                    gameMode = viewModel.simulation.gameMode,
                    settings = uiState.profile.settings,
                    landmarks = viewModel.simulation.worldMap?.minimapLandmarks.orEmpty(),
                    simulation = viewModel.simulation,
                    onPause = { viewModel.pauseMatch() },
                    onFinishPractice = { viewModel.endPracticeSession() }
                )
            }

            GameScreen.PAUSED -> {
                PauseMenuOverlay(
                    onResume = { viewModel.resumeMatch() },
                    onOpenSettings = {
                        returnFromSettings = GameScreen.PAUSED
                        viewModel.navigateTo(GameScreen.SETTINGS)
                    },
                    onRestart = { viewModel.startMatch() },
                    onReturnLobby = { viewModel.returnToLobby() }
                )
            }

            GameScreen.MATCH_RESULT -> {
                MatchResultOverlay(
                    hud = uiState.hud,
                    gameMode = viewModel.simulation.gameMode,
                    newlyUnlocked = uiState.newlyUnlocked,
                    onPlayAgain = { viewModel.startMatch() },
                    onReturnLobby = { viewModel.returnToLobby() }
                )
            }
        }

        if (uiState.showExitDialog) {
            ExitConfirmationDialog(
                onConfirmExit = {
                    viewModel.setShowExitDialog(false)
                    onExitApp()
                },
                onDismiss = { viewModel.setShowExitDialog(false) }
            )
        }
    }
}
