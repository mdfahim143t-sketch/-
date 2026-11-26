package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.GameMode
import com.example.model.HudSnapshot
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberStrike
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun PauseMenuOverlay(
    onResume: () -> Unit,
    onOpenSettings: () -> Unit,
    onRestart: () -> Unit,
    onReturnLobby: () -> Unit
) {
    BackHandler(onBack = onResume)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalMidnight.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = TacticalSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.5.dp, TacticalBorder),
            modifier = Modifier
                .padding(24.dp)
                .widthIn(max = 400.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.pause_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = AmberGlow
                )
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = AmberStrike, contentColor = TacticalMidnight),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("resume_button")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Resume")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.resume_match), style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = onOpenSettings,
                    border = BorderStroke(1.dp, TacticalBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("pause_settings_button")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = CyberCyan)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SETTINGS", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onRestart,
                    border = BorderStroke(1.dp, TacticalBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("restart_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Restart", tint = AmberGlow)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.restart_match), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
                OutlinedButton(
                    onClick = onReturnLobby,
                    border = BorderStroke(1.dp, CrimsonAlert.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("return_lobby_button")
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Lobby", tint = CrimsonAlert)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.return_lobby), style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                }
            }
        }
    }
}

@Composable
fun MatchResultOverlay(
    hud: HudSnapshot,
    gameMode: GameMode,
    newlyUnlocked: List<String>,
    onPlayAgain: () -> Unit,
    onReturnLobby: () -> Unit
) {
    BackHandler(onBack = onReturnLobby)

    Box(modifier = Modifier.fillMaxSize()) {
        // Bottom sheet style summary so the 3D Victory pose stays clearly visible above!
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TacticalMidnight.copy(alpha = 0.90f), TacticalMidnight.copy(alpha = 0.98f))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = 540.dp)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val titleText = when {
                    gameMode == GameMode.PRACTICE -> stringResource(R.string.practice_complete_title)
                    hud.isVictory -> stringResource(R.string.victory_title)
                    else -> stringResource(R.string.defeat_title)
                }
                val accent = when {
                    hud.isVictory -> AmberStrike
                    else -> CrimsonAlert
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = titleText,
                        tint = accent,
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.displayLarge,
                            color = accent
                        )
                        Text(
                            text = if (gameMode == GameMode.PRACTICE) {
                                "Training Session Completed"
                            } else {
                                "Rank #${hud.placementRank} of ${hud.totalSurvivors} • ${gameMode.title}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            color = TextSecondary
                        )
                    }
                }

                // Match Stats Grid
                Surface(
                    color = TacticalSurface.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, TacticalBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val min = hud.matchSeconds / 60
                        val sec = hud.matchSeconds % 60
                        ResultStatItem("ELIMINATIONS", "${hud.eliminations}", AmberGlow)
                        ResultStatItem("DAMAGE", "${hud.damageDealt}", CyberCyan)
                        ResultStatItem("SURVIVED", String.format("%02d:%02d", min, sec), EmeraldReady)
                    }
                }

                if (newlyUnlocked.isNotEmpty()) {
                    Surface(
                        color = EmeraldReady.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, EmeraldReady),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Unlocked", tint = EmeraldReady)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Unlocked: ${newlyUnlocked.joinToString(", ")}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldReady
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onReturnLobby,
                        border = BorderStroke(1.5.dp, TacticalBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("result_lobby_button")
                    ) {
                        Text("LOBBY", style = MaterialTheme.typography.labelLarge, color = TextPrimary)
                    }
                    Button(
                        onClick = onPlayAgain,
                        colors = ButtonDefaults.buttonColors(containerColor = AmberStrike, contentColor = TacticalMidnight),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("play_again_button")
                    ) {
                        Text("PLAY AGAIN", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.headlineMedium, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun ExitConfirmationDialog(
    onConfirmExit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TacticalSurface,
        title = {
            Text(stringResource(R.string.exit_dialog_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        },
        text = {
            Text(stringResource(R.string.exit_dialog_message), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        },
        confirmButton = {
            TextButton(onClick = onConfirmExit) {
                Text(stringResource(R.string.confirm_exit), color = CrimsonAlert)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = CyberCyan)
            }
        }
    )
}
