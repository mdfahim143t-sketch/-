package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

enum class GameScreen {
    LOBBY,
    CHARACTER_SELECT,
    OUTFIT_CUSTOMIZE,
    MAP_SELECT,
    SETTINGS,
    IN_GAME,
    PAUSED,
    MATCH_RESULT
}

enum class GameMode(val title: String, val subtitle: String, val enemyCount: Int) {
    SOLO_SURVIVAL(
        title = "Solo Survival",
        subtitle = "10 Survivors • Shrinking Zone • Loot & Outlast",
        enemyCount = 9
    ),
    PRACTICE(
        title = "Practice Mode",
        subtitle = "No Enemies • Free Exploration & Movement Drills",
        enemyCount = 0
    ),
    OFFLINE_ARENA(
        title = "Offline Arena",
        subtitle = "Fast Combat • 7 Tactical Rivals • Supply Drops",
        enemyCount = 7
    )
}

enum class GraphicsQuality(val label: String, val resolutionScale: Float, val grassDensity: Int, val shadowsEnabled: Boolean, val drawDistance: Float) {
    LOW("Low", 0.65f, 0, false, 88f),
    MEDIUM("Medium", 0.85f, 180, true, 120f),
    HIGH("High", 1.0f, 420, true, 160f)
}

enum class GameMapId(
    val displayName: String,
    val tagline: String,
    val biomeDescription: String,
    @DrawableRes val previewRes: Int
) {
    EMERALD_ISLE(
        displayName = "Emerald Archipelago",
        tagline = "Coastal Villages • Winding River • Radar Hill",
        biomeDescription = "Lush open fields, two riverfront villages linked by wooden bridges, abandoned concrete depot, and northern radar station.",
        previewRes = R.drawable.img_map_emerald_isle
    ),
    CANYON_OUTPOST(
        displayName = "Canyon Outpost",
        tagline = "Red Sandstone • Steel Gorge Bridge • Watchtowers",
        biomeDescription = "Sun-baked mesas overlooking a deep river channel, fortified watchtower compound, and abandoned freight sheds.",
        previewRes = R.drawable.img_map_canyon_outpost
    ),
    TWILIGHT_PINES(
        displayName = "Twilight Pines",
        tagline = "Northern Forest • Stone Crossing • Beacon Ridge",
        biomeDescription = "Cool dusk pine woods, misty river valley with stone arch bridge, timber cabins, and illuminated hilltop beacon.",
        previewRes = R.drawable.img_map_twilight_pines
    )
}

data class SurvivorCharacter(
    val id: String,
    val name: String,
    val codename: String,
    val role: String,
    val bio: String,
    val skinToneHex: Long,
    val accentHex: Long,
    val speedBonus: Float,
    val armorBonus: Int,
    val unlockWinsRequired: Int
)

object CharacterCatalog {
    val all = listOf(
        SurvivorCharacter(
            id = "kai",
            name = "Kai Mercer",
            codename = "VANGUARD",
            role = "All-Terrain Tactician",
            bio = "Former island pathfinder trained for rapid traversal across rivers, bridges, and urban ruins.",
            skinToneHex = 0xFFE0AC84,
            accentHex = 0xFFFF9F1C,
            speedBonus = 1.0f,
            armorBonus = 0,
            unlockWinsRequired = 0
        ),
        SurvivorCharacter(
            id = "nova",
            name = "Nova Lin",
            codename = "RANGER",
            role = "Recon Scout",
            bio = "Agile perimeter runner specializing in high-ground positioning and quick sprint recovery.",
            skinToneHex = 0xFFF1C6A8,
            accentHex = 0xFF22D3EE,
            speedBonus = 1.06f,
            armorBonus = 0,
            unlockWinsRequired = 0
        ),
        SurvivorCharacter(
            id = "rex",
            name = "Rex Vance",
            codename = "SENTINEL",
            role = "Heavy Defender",
            bio = "Armored outpost guardian carrying reinforced plating for sustained close-quarters firefights.",
            skinToneHex = 0xFF9C6B4B,
            accentHex = 0xFF10B981,
            speedBonus = 0.96f,
            armorBonus = 20,
            unlockWinsRequired = 1
        ),
        SurvivorCharacter(
            id = "mira",
            name = "Mira Solis",
            codename = "GHOST",
            role = "Night Operative",
            bio = "Stealth specialist engineered for quiet flank routes through pine forests and abandoned depots.",
            skinToneHex = 0xFFD29972,
            accentHex = 0xFFA855F7,
            speedBonus = 1.04f,
            armorBonus = 10,
            unlockWinsRequired = 2
        )
    )

    fun byId(id: String): SurvivorCharacter = all.firstOrNull { it.id == id } ?: all.first()
}

data class OutfitConfig(
    val hairStyle: Int = 0,
    val maskStyle: Int = 1,
    val jacketStyle: Int = 0,
    val bootsStyle: Int = 0,
    val glovesStyle: Int = 0,
    val backpackStyle: Int = 1
)

object OutfitCatalog {
    val hairStyles = listOf("Tactical Crop", "Swept Crest", "Mohawk Ridge", "Twin Tail")
    val maskStyles = listOf("Unmasked", "Amber Visor", "Half Respirator", "Recon Helmet")
    val jacketStyles = listOf("Vanguard Slate", "Nomad Sand", "Cobalt Striker", "Crimson Apex")
    val bootsStyles = listOf("Field Boots", "Exo Greaves", "Stealth Treads")
    val glovesStyles = listOf("Recon Wraps", "Armored Gauntlets", "Carbon Grip")
    val backpackStyles = listOf("Compact Pack", "Field Rucksack", "Tech Thruster")
}

enum class ArcadeWeapon(
    val displayName: String,
    val shortName: String,
    val damage: Int,
    val fireIntervalSec: Float,
    val magSize: Int,
    val reloadSec: Float,
    val range: Float,
    val pellets: Int,
    val spreadRad: Float,
    val colorHex: Long
) {
    PULSE_RIFLE("VX-9 Pulse Rifle", "VX-9 PULSE", 18, 0.16f, 28, 1.5f, 65f, 1, 0.025f, 0xFF38BDF8),
    ARC_SCATTER("SG-4 Arc Scatter", "SG-4 ARC", 11, 0.62f, 8, 1.8f, 26f, 5, 0.11f, 0xFFFF9F1C),
    VOLT_CARBINE("K-12 Volt Carbine", "K-12 VOLT", 13, 0.11f, 36, 1.4f, 48f, 1, 0.045f, 0xFF10B981),
    PLASMA_LANCE("M-77 Plasma Lance", "M-77 LANCE", 44, 0.78f, 10, 2.0f, 95f, 1, 0.008f, 0xFFE879F9)
}

data class GameSettings(
    val graphicsQuality: GraphicsQuality = GraphicsQuality.MEDIUM,
    val musicVolume: Float = 0.65f,
    val sfxVolume: Float = 0.85f,
    val joystickScale: Float = 1.0f,
    val cameraSensitivity: Float = 1.0f,
    val buttonOpacity: Float = 0.85f,
    val autoSprint: Boolean = false,
    val invertY: Boolean = false
)

data class PlayerStats(
    val matchesPlayed: Int = 0,
    val victories: Int = 0,
    val totalEliminations: Int = 0,
    val bestSurvivalSeconds: Int = 0,
    val unlockedIds: Set<String> = setOf("kai", "nova")
)

data class MinimapBlip(
    val normX: Float,
    val normZ: Float,
    val isEnemy: Boolean = false,
    val isLoot: Boolean = false
)

data class HudSnapshot(
    val health: Int = 100,
    val maxHealth: Int = 100,
    val shield: Int = 50,
    val maxShield: Int = 100,
    val stamina: Float = 1f,
    val aliveCount: Int = 10,
    val totalSurvivors: Int = 10,
    val eliminations: Int = 0,
    val damageDealt: Int = 0,
    val matchSeconds: Int = 0,
    val safeZoneRadius: Float = 85f,
    val safeZoneCenterX: Float = 0f,
    val safeZoneCenterZ: Float = 0f,
    val isInSafeZone: Boolean = true,
    val isCrouching: Boolean = false,
    val isSprinting: Boolean = false,
    val isAirborne: Boolean = false,
    val activeSlot: Int = 0,
    val primaryWeapon: ArcadeWeapon = ArcadeWeapon.PULSE_RIFLE,
    val secondaryWeapon: ArcadeWeapon? = null,
    val primaryAmmo: Int = 28,
    val primaryReserve: Int = 84,
    val secondaryAmmo: Int = 0,
    val secondaryReserve: Int = 0,
    val isReloading: Boolean = false,
    val medCells: Int = 2,
    val shieldCores: Int = 1,
    val nearbyLootPrompt: String? = null,
    val hitMarkerAlpha: Float = 0f,
    val damageFlashAlpha: Float = 0f,
    val statusBanner: String? = null,
    val playerNormX: Float = 0.5f,
    val playerNormZ: Float = 0.5f,
    val playerYawRad: Float = 0f,
    val mapBlips: List<MinimapBlip> = emptyList(),
    val matchFinished: Boolean = false,
    val isVictory: Boolean = false,
    val placementRank: Int = 1
)
