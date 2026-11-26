package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.model.GameMapId
import com.example.model.GameSettings
import com.example.model.GraphicsQuality
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberStrike
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TacticalSurfaceElevated
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun MapSelectScreen(
    selectedMap: GameMapId,
    onSelectMap: (GameMapId) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalMidnight.copy(alpha = 0.86f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 700.dp)
                .align(Alignment.Center)
                .navigationBarsPadding()
        ) {
            SubScreenHeader(title = "SELECT ISLAND MAP", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(GameMapId.entries, key = { it.name }) { mapInfo ->
                    val isSelected = mapInfo == selectedMap
                    Card(
                        onClick = { onSelectMap(mapInfo) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) TacticalSurfaceElevated else TacticalSurface
                        ),
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) AmberStrike else TacticalBorder
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("map_card_${mapInfo.name.lowercase()}")
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                Image(
                                    painter = painterResource(id = mapInfo.previewRes),
                                    contentDescription = mapInfo.displayName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color.Transparent, TacticalSurface.copy(alpha = 0.85f))
                                            )
                                        )
                                )
                                if (isSelected) {
                                    Surface(
                                        color = EmeraldReady,
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Active Map",
                                                tint = TacticalMidnight,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "SELECTED",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TacticalMidnight
                                            )
                                        }
                                    }
                                }
                            }
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = mapInfo.displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = mapInfo.tagline,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CyberCyan
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = mapInfo.biomeDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    settings: GameSettings,
    onUpdateSettings: ((GameSettings) -> GameSettings) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TacticalMidnight.copy(alpha = 0.90f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 680.dp)
                .align(Alignment.Center)
                .navigationBarsPadding()
        ) {
            SubScreenHeader(title = "SETTINGS", onBack = onBack)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Graphics Quality
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.Tune,
                        title = "GRAPHICS QUALITY"
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GraphicsQuality.entries.forEach { quality ->
                                val selected = settings.graphicsQuality == quality
                                Surface(
                                    onClick = { onUpdateSettings { it.copy(graphicsQuality = quality) } },
                                    color = if (selected) AmberStrike.copy(alpha = 0.24f) else TacticalMidnight,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.5.dp, if (selected) AmberStrike else TacticalBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("graphics_${quality.name.lowercase()}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = quality.label,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (selected) AmberGlow else TextPrimary
                                        )
                                        Text(
                                            text = when (quality) {
                                                GraphicsQuality.LOW -> "Battery Saver"
                                                GraphicsQuality.MEDIUM -> "Balanced 3D"
                                                GraphicsQuality.HIGH -> "Full Grass & FX"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Audio
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.GraphicEq,
                        title = "AUDIO"
                    ) {
                        SettingsSliderRow(
                            label = "Music & Environment",
                            value = settings.musicVolume,
                            range = 0f..1f,
                            valueText = "${(settings.musicVolume * 100).toInt()}%",
                            onValueChange = { v -> onUpdateSettings { it.copy(musicVolume = v) } }
                        )
                        SettingsSliderRow(
                            label = "Sound Effects",
                            value = settings.sfxVolume,
                            range = 0f..1f,
                            valueText = "${(settings.sfxVolume * 100).toInt()}%",
                            onValueChange = { v -> onUpdateSettings { it.copy(sfxVolume = v) } }
                        )
                    }
                }

                // Touch Controls
                item {
                    SettingsSectionCard(
                        icon = Icons.Default.TouchApp,
                        title = "TOUCH CONTROLS"
                    ) {
                        SettingsSliderRow(
                            label = "Camera Sensitivity",
                            value = settings.cameraSensitivity,
                            range = 0.5f..2.0f,
                            valueText = String.format("%.1fx", settings.cameraSensitivity),
                            onValueChange = { v -> onUpdateSettings { it.copy(cameraSensitivity = v) } }
                        )
                        SettingsSliderRow(
                            label = "Joystick Size",
                            value = settings.joystickScale,
                            range = 0.75f..1.35f,
                            valueText = "${(settings.joystickScale * 100).toInt()}%",
                            onValueChange = { v -> onUpdateSettings { it.copy(joystickScale = v) } }
                        )
                        SettingsSliderRow(
                            label = "Control Opacity",
                            value = settings.buttonOpacity,
                            range = 0.45f..1.0f,
                            valueText = "${(settings.buttonOpacity * 100).toInt()}%",
                            onValueChange = { v -> onUpdateSettings { it.copy(buttonOpacity = v) } }
                        )
                        SettingsToggleRow(
                            label = "Auto-Sprint at Full Joystick Push",
                            checked = settings.autoSprint,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(autoSprint = c) } }
                        )
                        SettingsToggleRow(
                            label = "Invert Vertical Camera Look",
                            checked = settings.invertY,
                            onCheckedChange = { c -> onUpdateSettings { it.copy(invertY = c) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TacticalSurface),
        border = BorderStroke(1.dp, TacticalBorder),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = title, tint = CyberCyan, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = AmberGlow)
            }
            content()
        }
    }
}

@Composable
private fun SettingsSliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onValueChange: (Float) -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Text(text = valueText, style = MaterialTheme.typography.labelLarge, color = CyberCyan)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = AmberStrike,
                activeTrackColor = AmberStrike,
                inactiveTrackColor = TacticalBorder
            )
        )
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TacticalMidnight,
                checkedTrackColor = AmberStrike
            )
        )
    }
}
