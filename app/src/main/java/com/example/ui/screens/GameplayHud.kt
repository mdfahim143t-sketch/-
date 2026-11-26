package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.GameSimulation
import com.example.engine.MinimapLandmark
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.HudSnapshot
import com.example.ui.theme.AmberGlow
import com.example.ui.theme.AmberStrike
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.EmeraldReady
import com.example.ui.theme.ShieldBlue
import com.example.ui.theme.TacticalBorder
import com.example.ui.theme.TacticalMidnight
import com.example.ui.theme.TacticalSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun GameplayHud(
    hud: HudSnapshot,
    gameMode: GameMode,
    settings: GameSettings,
    landmarks: List<MinimapLandmark>,
    simulation: GameSimulation,
    onPause: () -> Unit,
    onFinishPractice: () -> Unit
) {
    BackHandler(onBack = onPause)

    val ctrlAlpha = settings.buttonOpacity
    var joyKnobX by remember { mutableStateOf(0f) }
    var joyKnobY by remember { mutableStateOf(0f) }
    var joyActive by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 540.dp

        // 1. Damage Vignette Overlay
        if (hud.damageFlashAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, CrimsonAlert.copy(alpha = hud.damageFlashAlpha * 0.55f))
                        )
                    )
            )
        }

        // 2. Dual-Zone Touch Surface: Left Joystick Area + Right Camera Look Area
        Row(modifier = Modifier.fillMaxSize()) {
            // Left Touch Zone (Joystick)
            val joyRadiusPx = with(LocalDensity.current) { (54.dp * settings.joystickScale).toPx() }
            Box(
                modifier = Modifier
                    .weight(0.45f)
                    .fillMaxHeight()
                    .pointerInput(joyRadiusPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val origin = down.position
                            joyActive = true
                            var curId = down.id
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == curId } ?: break
                                if (!change.pressed) {
                                    break
                                }
                                val dx = change.position.x - origin.x
                                val dy = change.position.y - origin.y
                                val dist = sqrt(dx * dx + dy * dy)
                                val clamped = dist.coerceAtMost(joyRadiusPx)
                                val nx = if (dist > 1f) (dx / dist) * (clamped / joyRadiusPx) else 0f
                                val ny = if (dist > 1f) (dy / dist) * (clamped / joyRadiusPx) else 0f
                                joyKnobX = nx * joyRadiusPx
                                joyKnobY = ny * joyRadiusPx
                                simulation.joyX = nx
                                simulation.joyY = ny
                                change.consume()
                            }
                            joyActive = false
                            joyKnobX = 0f
                            joyKnobY = 0f
                            simulation.joyX = 0f
                            simulation.joyY = 0f
                        }
                    }
            )

            // Right Touch Zone (Smooth Camera Look)
            Box(
                modifier = Modifier
                    .weight(0.55f)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            simulation.rotateCamera(dragAmount.x, dragAmount.y)
                        }
                    }
            )
        }

        // 3. Center Tactical Crosshair & Hit Marker
        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .size(44.dp)
        ) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val gap = 6.dp.toPx()
            val len = 6.dp.toPx()
            val crossCol = Color.White.copy(alpha = 0.85f)
            drawLine(crossCol, Offset(c.x - gap - len, c.y), Offset(c.x - gap, c.y), 2.dp.toPx())
            drawLine(crossCol, Offset(c.x + gap, c.y), Offset(c.x + gap + len, c.y), 2.dp.toPx())
            drawLine(crossCol, Offset(c.x, c.y - gap - len), Offset(c.x, c.y - gap), 2.dp.toPx())
            drawLine(crossCol, Offset(c.x, c.y + gap), Offset(c.x, c.y + gap + len), 2.dp.toPx())
            drawCircle(AmberGlow, radius = 2.dp.toPx(), center = c)

            if (hud.hitMarkerAlpha > 0.02f) {
                val hmCol = AmberStrike.copy(alpha = hud.hitMarkerAlpha)
                val d1 = 5.dp.toPx()
                val d2 = 14.dp.toPx()
                drawLine(hmCol, Offset(c.x - d2, c.y - d2), Offset(c.x - d1, c.y - d1), 2.5.dp.toPx())
                drawLine(hmCol, Offset(c.x + d1, c.y - d1), Offset(c.x + d2, c.y - d2), 2.5.dp.toPx())
                drawLine(hmCol, Offset(c.x - d2, c.y + d2), Offset(c.x - d1, c.y + d1), 2.5.dp.toPx())
                drawLine(hmCol, Offset(c.x + d1, c.y + d1), Offset(c.x + d2, c.y + d2), 2.5.dp.toPx())
            }
        }

        // 4. Top HUD Strip (Minimap, Alive/Kills/Timer, Pause)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Top-Left: Minimap Radar + Safe Zone / Timer
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MinimapRadar(
                    hud = hud,
                    landmarks = landmarks,
                    showZone = gameMode != GameMode.PRACTICE,
                    sizeDp = if (isCompactWidth) 92 else 112
                )
                Surface(
                    color = TacticalMidnight.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (hud.isInSafeZone) TacticalBorder else CrimsonAlert)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!hud.isInSafeZone && gameMode != GameMode.PRACTICE) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Outside Zone",
                                tint = CrimsonAlert,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        val min = hud.matchSeconds / 60
                        val sec = hud.matchSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", min, sec),
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }
            }

            // Top-Center: Alive & Kills Pill + Status Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = TacticalMidnight.copy(alpha = 0.80f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (gameMode != GameMode.PRACTICE) {
                            StatPill("ALIVE", "${hud.aliveCount}", CyberCyan)
                            StatPill(
                                if (gameMode == GameMode.OFFLINE_ARENA) "ELIMS" else "KILLS",
                                if (gameMode == GameMode.OFFLINE_ARENA) "${hud.eliminations}/10" else "${hud.eliminations}",
                                AmberStrike
                            )
                        } else {
                            StatPill("MODE", "PRACTICE", EmeraldReady)
                            StatPill("DMG", "${hud.damageDealt}", AmberStrike)
                        }
                    }
                }

                hud.statusBanner?.let { msg ->
                    Surface(
                        color = AmberStrike.copy(alpha = 0.90f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelLarge,
                            color = TacticalMidnight,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            // Top-Right: Practice Complete + Pause
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (gameMode == GameMode.PRACTICE) {
                    Surface(
                        onClick = onFinishPractice,
                        color = EmeraldReady.copy(alpha = 0.9f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("finish_practice_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Check, contentDescription = "Finish", tint = TacticalMidnight, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("DONE", style = MaterialTheme.typography.labelLarge, color = TacticalMidnight)
                        }
                    }
                }

                IconButton(
                    onClick = onPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(TacticalMidnight.copy(alpha = 0.80f), CircleShape)
                        .border(1.dp, TacticalBorder, CircleShape)
                        .testTag("pause_button")
                ) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause Match", tint = TextPrimary)
                }
            }
        }

        // 5. Visual Virtual Joystick Indicator (Bottom-Left)
        val joySize = 114.dp * settings.joystickScale
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp, bottom = 28.dp)
                .size(joySize)
                .alpha(if (joyActive) minOf(1f, ctrlAlpha + 0.15f) else ctrlAlpha * 0.75f)
                .background(TacticalMidnight.copy(alpha = 0.45f), CircleShape)
                .border(2.dp, if (hud.isSprinting) AmberStrike else CyberCyan.copy(alpha = 0.65f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(joyKnobX.roundToInt(), joyKnobY.roundToInt()) }
                    .size(44.dp * settings.joystickScale)
                    .background(
                        if (hud.isSprinting) AmberStrike.copy(alpha = 0.9f) else CyberCyan.copy(alpha = 0.85f),
                        CircleShape
                    )
                    .border(2.dp, Color.White.copy(alpha = 0.7f), CircleShape)
            )
        }

        // 6. Contextual Interact / Loot Prompt
        hud.nearbyLootPrompt?.let { label ->
            Surface(
                onClick = { simulation.wantsInteract = true },
                color = AmberStrike.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 74.dp)
                    .minimumInteractiveComponentSize()
                    .testTag("interact_button")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Inventory2, contentDescription = "Pick up", tint = TacticalMidnight, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PICK UP: $label", style = MaterialTheme.typography.labelLarge, color = TacticalMidnight)
                }
            }
        }

        // 7. Bottom-Center Status Bars + Weapon & Supply Strip
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
                .width(if (isCompactWidth) 190.dp else 280.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Supply Quick-Use Pills (Med-Cell & Shield Core)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = { simulation.useMedCell() },
                    color = TacticalSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.65f)),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalHospital, contentDescription = "Med-Cell", tint = EmeraldReady, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("MED ${hud.medCells}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
                Surface(
                    onClick = { simulation.useShieldCore() },
                    color = TacticalSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ShieldBlue.copy(alpha = 0.65f)),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Shield Core", tint = ShieldBlue, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("AP ${hud.shieldCores}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                }
            }

            // Weapon Slots Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val active0 = hud.activeSlot == 0
                WeaponSlotChip(
                    name = hud.primaryWeapon.shortName,
                    ammo = hud.primaryAmmo,
                    reserve = hud.primaryReserve,
                    selected = active0,
                    reloading = active0 && hud.isReloading,
                    modifier = Modifier.weight(1f),
                    onClick = { simulation.switchWeaponSlot(0) }
                )
                hud.secondaryWeapon?.let { sec ->
                    val active1 = hud.activeSlot == 1
                    WeaponSlotChip(
                        name = sec.shortName,
                        ammo = hud.secondaryAmmo,
                        reserve = hud.secondaryReserve,
                        selected = active1,
                        reloading = active1 && hud.isReloading,
                        modifier = Modifier.weight(1f),
                        onClick = { simulation.switchWeaponSlot(1) }
                    )
                }
            }

            // Shield + Health + Stamina Bars
            Surface(
                color = TacticalMidnight.copy(alpha = 0.82f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, TacticalBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Shield Bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = "Shield", tint = ShieldBlue, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        LinearProgressIndicator(
                            progress = { hud.shield / hud.maxShield.toFloat() },
                            color = ShieldBlue,
                            trackColor = TacticalBorder,
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                    // Health Bar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Favorite, contentDescription = "Health", tint = EmeraldReady, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        LinearProgressIndicator(
                            progress = { hud.health / hud.maxHealth.toFloat() },
                            color = if (hud.health > 30) EmeraldReady else CrimsonAlert,
                            trackColor = TacticalBorder,
                            modifier = Modifier
                                .weight(1f)
                                .height(7.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("${hud.health}", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                    }
                    // Stamina Bar
                    LinearProgressIndicator(
                        progress = { hud.stamina },
                        color = AmberGlow,
                        trackColor = TacticalBorder.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }

        // 8. Right-Side Action Buttons (Fire, Jump, Crouch, Sprint, Reload)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = 24.dp)
                .alpha(ctrlAlpha),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionHudButton(
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    label = "SPRINT",
                    active = simulation.wantsSprint,
                    accent = AmberStrike,
                    onClick = { simulation.wantsSprint = !simulation.wantsSprint },
                    modifier = Modifier.testTag("sprint_button")
                )
                ActionHudButton(
                    icon = Icons.Default.Refresh,
                    label = "RELOAD",
                    active = hud.isReloading,
                    accent = CyberCyan,
                    onClick = { simulation.wantsReload = true },
                    modifier = Modifier.testTag("reload_button")
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ActionHudButton(
                        icon = Icons.Default.ArrowUpward,
                        label = "JUMP",
                        active = hud.isAirborne,
                        accent = CyberCyan,
                        onClick = { simulation.wantsJump = true },
                        modifier = Modifier.testTag("jump_button")
                    )
                    ActionHudButton(
                        icon = Icons.Default.ExpandMore,
                        label = "CROUCH",
                        active = hud.isCrouching,
                        accent = EmeraldReady,
                        onClick = { simulation.isCrouching = !simulation.isCrouching },
                        modifier = Modifier.testTag("crouch_button")
                    )
                }

                // Primary FIRE Hold-to-Shoot Button
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(AmberStrike.copy(alpha = 0.88f))
                        .border(2.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                simulation.isFiringInput = true
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.none { it.pressed }) break
                                }
                                simulation.isFiringInput = false
                            }
                        }
                        .testTag("fire_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Fire Weapon",
                            tint = TacticalMidnight,
                            modifier = Modifier.size(30.dp)
                        )
                        Text("FIRE", style = MaterialTheme.typography.labelSmall, color = TacticalMidnight)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "$label ", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelLarge, color = color)
    }
}

@Composable
private fun WeaponSlotChip(
    name: String,
    ammo: Int,
    reserve: Int,
    selected: Boolean,
    reloading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (selected) TacticalSurface.copy(alpha = 0.92f) else TacticalMidnight.copy(alpha = 0.65f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.5.dp, if (selected) AmberStrike else TacticalBorder),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) AmberGlow else TextSecondary,
                maxLines = 1
            )
            Text(
                text = if (reloading) "RELOADING..." else "$ammo / $reserve",
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun ActionHudButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = if (active) accent.copy(alpha = 0.90f) else TacticalMidnight.copy(alpha = 0.78f),
        shape = CircleShape,
        border = BorderStroke(1.5.dp, if (active) Color.White else accent.copy(alpha = 0.75f)),
        modifier = modifier.size(54.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) TacticalMidnight else TextPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = if (active) TacticalMidnight else TextSecondary
            )
        }
    }
}

@Composable
private fun MinimapRadar(
    hud: HudSnapshot,
    landmarks: List<MinimapLandmark>,
    showZone: Boolean,
    sizeDp: Int
) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(TacticalMidnight.copy(alpha = 0.80f))
            .border(1.5.dp, TacticalBorder, RoundedCornerShape(14.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Grid lines
            val gridCol = TacticalBorder.copy(alpha = 0.35f)
            drawLine(gridCol, Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), 1f)
            drawLine(gridCol, Offset(0f, h * 0.5f), Offset(w, h * 0.5f), 1f)

            // Landmarks (buildings & bridges)
            for (lm in landmarks) {
                val lx = (lm.normX - lm.normW * 0.5f) * w
                val lz = (lm.normZ - lm.normH * 0.5f) * h
                drawRect(
                    color = if (lm.isBridge) AmberGlow.copy(alpha = 0.6f) else Color(0xFF64748B).copy(alpha = 0.65f),
                    topLeft = Offset(lx, lz),
                    size = Size(maxOf(3f, lm.normW * w), maxOf(3f, lm.normH * h))
                )
            }

            // Safe Zone Circle
            if (showZone) {
                val zoneNormR = (hud.safeZoneRadius / 172f) * w
                val zcx = ((hud.safeZoneCenterX + 86f) / 172f) * w
                val zcz = ((hud.safeZoneCenterZ + 86f) / 172f) * h
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.75f),
                    radius = zoneNormR,
                    center = Offset(zcx, zcz),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Blips (Loot & Nearby Enemies)
            for (b in hud.mapBlips) {
                drawCircle(
                    color = if (b.isEnemy) CrimsonAlert else AmberStrike,
                    radius = if (b.isEnemy) 3.5.dp.toPx() else 2.2.dp.toPx(),
                    center = Offset(b.normX * w, b.normZ * h)
                )
            }

            // Player Dot & Heading Line
            val px = hud.playerNormX * w
            val pz = hud.playerNormZ * h
            val lookX = px + sin(hud.playerYawRad) * 10.dp.toPx()
            val lookZ = pz + cos(hud.playerYawRad) * 10.dp.toPx()
            drawLine(EmeraldReady, Offset(px, pz), Offset(lookX, lookZ), 2.dp.toPx())
            drawCircle(EmeraldReady, radius = 4.dp.toPx(), center = Offset(px, pz))
        }
    }
}
