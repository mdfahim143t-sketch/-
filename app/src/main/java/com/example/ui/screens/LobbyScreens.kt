package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PersistedProfile
import com.example.model.CharacterCatalog
import com.example.model.GameMode
import com.example.model.GameScreen
import com.example.model.OutfitCatalog
import com.example.model.OutfitConfig
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
fun MainMenuOverlay(
    profile: PersistedProfile,
    onRotateCharacter: (Float) -> Unit,
    onSelectMode: (GameMode) -> Unit,
    onDeploy: () -> Unit,
    onNavigate: (GameScreen) -> Unit,
    onRequestExit: () -> Unit
) {
    val selectedChar = CharacterCatalog.byId(profile.selectedCharacterId)

    Box(modifier = Modifier.fillMaxSize()) {
        // Center drag-to-rotate zone over 3D character
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotateCharacter(dragAmount.x)
                    }
                }
        )

        // Subtle bottom gradient for HUD readability without hiding 3D sky/character
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(270.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, TacticalMidnight.copy(alpha = 0.88f), TacticalMidnight.copy(alpha = 0.96f))
                    )
                )
        )

        // Top Header with Logo, Offline Badge, Settings & Exit
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_emblem),
                    contentDescription = "Last Survivor Emblem",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.5.dp, AmberStrike, RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LAST SURVIVOR",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary,
                        letterSpacing = 1.6.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.game_subtitle),
                            style = MaterialTheme.typography.labelSmall,
                            color = AmberStrike
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = EmeraldReady.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, EmeraldReady.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Offline Mode",
                                    tint = EmeraldReady,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "OFFLINE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldReady
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TacticalSurface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = "Wins",
                            tint = AmberGlow,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${profile.stats.victories}W • ${profile.stats.totalEliminations}K",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = { onNavigate(GameScreen.SETTINGS) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(TacticalSurface.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, TacticalBorder, CircleShape)
                        .testTag("settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = TextPrimary
                    )
                }

                IconButton(
                    onClick = onRequestExit,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .background(TacticalSurface.copy(alpha = 0.85f), CircleShape)
                        .border(1.dp, TacticalBorder, CircleShape)
                        .testTag("exit_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Exit Game",
                        tint = TextSecondary
                    )
                }
            }
        }

        // Bottom Tactical Lobby Dock
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Character / Drag prompt pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = TacticalSurface.copy(alpha = 0.78f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, TacticalBorder)
                ) {
                    Text(
                        text = "${selectedChar.name.uppercase()} • ${selectedChar.codename}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CyberCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
                Text(
                    text = "Drag character to rotate",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }

            // Game Mode Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GameMode.entries.forEach { mode ->
                    val selected = profile.selectedGameMode == mode
                    Surface(
                        onClick = { onSelectMode(mode) },
                        color = if (selected) AmberStrike.copy(alpha = 0.22f) else TacticalSurface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, if (selected) AmberStrike else TacticalBorder),
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("mode_${mode.name.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) AmberGlow else TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (mode == GameMode.PRACTICE) "0 Enemies" else "${mode.enemyCount} AI Rivals",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Customization & Map Action Row + Deploy CTA
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LobbyNavChip(
                    icon = Icons.Default.Person,
                    label = "SURVIVORS",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("characters_button"),
                    onClick = { onNavigate(GameScreen.CHARACTER_SELECT) }
                )
                LobbyNavChip(
                    icon = Icons.Default.Checkroom,
                    label = "OUTFIT",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("outfit_button"),
                    onClick = { onNavigate(GameScreen.OUTFIT_CUSTOMIZE) }
                )
                LobbyNavChip(
                    icon = Icons.Default.Map,
                    label = "ISLAND",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("maps_button"),
                    onClick = { onNavigate(GameScreen.MAP_SELECT) }
                )
            }

            // Primary DEPLOY / PLAY Button
            Button(
                onClick = onDeploy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberStrike,
                    contentColor = TacticalMidnight
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("play_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Deploy",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PLAY • ${profile.selectedGameMode.title.uppercase()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun LobbyNavChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = TacticalSurfaceElevated.copy(alpha = 0.90f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, TacticalBorder),
        modifier = modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = CyberCyan,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun CharacterSelectScreen(
    profile: PersistedProfile,
    onRotateCharacter: (Float) -> Unit,
    onSelectCharacter: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        // Top half shows live 3D character preview (interactive rotate)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotateCharacter(dragAmount.x)
                    }
                }
        )

        SubScreenHeader(
            title = "SELECT SURVIVOR",
            onBack = onBack
        )

        // Bottom drawer with character cards
        LazyColumn(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.50f)
                .background(
                    Brush.verticalGradient(
                        listOf(TacticalMidnight.copy(alpha = 0.72f), TacticalMidnight.copy(alpha = 0.96f))
                    ),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(CharacterCatalog.all, key = { it.id }) { char ->
                val unlocked = char.id in profile.stats.unlockedIds
                val selected = char.id == profile.selectedCharacterId
                Card(
                    onClick = { if (unlocked) onSelectCharacter(char.id) },
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) TacticalSurfaceElevated else TacticalSurface
                    ),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) AmberStrike else TacticalBorder
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("character_card_${char.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(Color(char.accentHex).copy(alpha = 0.22f), CircleShape)
                                .border(2.dp, Color(char.accentHex), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (unlocked) Icons.Default.Person else Icons.Default.Lock,
                                contentDescription = char.name,
                                tint = Color(char.accentHex)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${char.name} (${char.codename})",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                if (selected) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = EmeraldReady,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = char.role,
                                style = MaterialTheme.typography.labelSmall,
                                color = CyberCyan
                            )
                            Text(
                                text = if (unlocked) char.bio else "Unlock: ${char.unlockWinsRequired} Win(s) or ${char.unlockWinsRequired * 4} Eliminations",
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

@Composable
fun OutfitCustomizeScreen(
    outfit: OutfitConfig,
    onRotateCharacter: (Float) -> Unit,
    onUpdateOutfit: ((OutfitConfig) -> OutfitConfig) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onRotateCharacter(dragAmount.x)
                    }
                }
        )

        SubScreenHeader(
            title = "ARMORY & OUTFIT",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .fillMaxHeight(0.48f)
                .background(
                    Brush.verticalGradient(
                        listOf(TacticalMidnight.copy(alpha = 0.72f), TacticalMidnight.copy(alpha = 0.96f))
                    ),
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutfitOptionRow(
                    title = "JACKET",
                    options = OutfitCatalog.jacketStyles,
                    selectedIndex = outfit.jacketStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(jacketStyle = idx) } }
                )
            }
            item {
                OutfitOptionRow(
                    title = "MASK & VISOR",
                    options = OutfitCatalog.maskStyles,
                    selectedIndex = outfit.maskStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(maskStyle = idx) } }
                )
            }
            item {
                OutfitOptionRow(
                    title = "HAIRSTYLE",
                    options = OutfitCatalog.hairStyles,
                    selectedIndex = outfit.hairStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(hairStyle = idx) } }
                )
            }
            item {
                OutfitOptionRow(
                    title = "BACKPACK",
                    options = OutfitCatalog.backpackStyles,
                    selectedIndex = outfit.backpackStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(backpackStyle = idx) } }
                )
            }
            item {
                OutfitOptionRow(
                    title = "GLOVES",
                    options = OutfitCatalog.glovesStyles,
                    selectedIndex = outfit.glovesStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(glovesStyle = idx) } }
                )
            }
            item {
                OutfitOptionRow(
                    title = "BOOTS",
                    options = OutfitCatalog.bootsStyles,
                    selectedIndex = outfit.bootsStyle,
                    onSelect = { idx -> onUpdateOutfit { it.copy(bootsStyle = idx) } }
                )
            }
        }
    }
}

@Composable
private fun OutfitOptionRow(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = AmberGlow
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(options.indices.toList()) { idx ->
                val selected = idx == selectedIndex
                Surface(
                    onClick = { onSelect(idx) },
                    color = if (selected) AmberStrike.copy(alpha = 0.22f) else TacticalSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.5.dp, if (selected) AmberStrike else TacticalBorder),
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(
                        text = options[idx],
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selected) TextPrimary else TextSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SubScreenHeader(
    title: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .background(TacticalSurface.copy(alpha = 0.88f), CircleShape)
                .border(1.dp, TacticalBorder, CircleShape)
                .testTag("back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Surface(
            color = TacticalSurface.copy(alpha = 0.88f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, TacticalBorder)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}
