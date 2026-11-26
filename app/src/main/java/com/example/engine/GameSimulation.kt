package com.example.engine

import com.example.audio.SoundEngine
import com.example.model.ArcadeWeapon
import com.example.model.CharacterCatalog
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.HudSnapshot
import com.example.model.MinimapBlip
import com.example.model.OutfitConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

enum class AiBehaviorState {
    PATROL,
    ENGAGE,
    SEEK_COVER,
    EVADE_STORM
}

data class EnemyActor(
    val id: Int,
    var x: Float,
    var y: Float,
    var z: Float,
    var yawRad: Float,
    var health: Int,
    var maxHealth: Int,
    var characterId: String,
    var outfit: OutfitConfig,
    var weapon: ArcadeWeapon,
    var state: AiBehaviorState = AiBehaviorState.PATROL,
    var waypointIndex: Int = 0,
    var fireCooldown: Float = 0.8f,
    var stateTimer: Float = 0f,
    var animTime: Float = 0f,
    var recoil: Float = 0f,
    var coverTarget: Vec3? = null,
    var alive: Boolean = true,
    var animState: CharacterAnimState = CharacterAnimState.IDLE
)

data class EnergyBolt(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    var fromPlayer: Boolean,
    var color: FloatArray
)

data class ParticleFx(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    var life: Float,
    var maxLife: Float,
    var size: Float,
    var color: FloatArray
)

class GameSimulation(
    private val soundEngine: SoundEngine?
) {
    // Player input state (written from UI thread)
    @Volatile var joyX: Float = 0f
    @Volatile var joyY: Float = 0f
    @Volatile var isFiringInput: Boolean = false
    @Volatile var wantsJump: Boolean = false
    @Volatile var wantsSprint: Boolean = false
    @Volatile var isCrouching: Boolean = false
    @Volatile var wantsInteract: Boolean = false
    @Volatile var wantsReload: Boolean = false

    // Camera angles (radians)
    @Volatile var camYawRad: Float = 0f
    @Volatile var camPitchRad: Float = 0.30f
    @Volatile var lobbyYawRad: Float = 0.35f

    // Player state
    var playerX = -26f
    var playerY = 2f
    var playerZ = -18f
    var playerVelY = 0f
    var playerYawRad = 0f
    var playerAnimState = CharacterAnimState.IDLE
    var playerAnimTime = 0f
    var playerRecoil = 0f
    var isAirborne = false
    var health = 100
    var maxHealth = 100
    var shield = 50
    var maxShield = 100
    var stamina = 1f

    var activeSlot = 0
    var primaryWeapon = ArcadeWeapon.PULSE_RIFLE
    var secondaryWeapon: ArcadeWeapon? = ArcadeWeapon.ARC_SCATTER
    var primaryAmmo = ArcadeWeapon.PULSE_RIFLE.magSize
    var primaryReserve = 84
    var secondaryAmmo = ArcadeWeapon.ARC_SCATTER.magSize
    var secondaryReserve = 24
    var reloadTimer = 0f
    var fireCooldown = 0f
    var medCells = 2
    var shieldCores = 1

    var eliminations = 0
    var damageDealt = 0
    var matchElapsed = 0f
    var matchFinished = false
    var isVictory = false
    var placementRank = 1
    var hitMarkerAlpha = 0f
    var damageFlashAlpha = 0f
    var statusBanner: String? = null
    private var statusBannerTimer = 0f
    private var footstepTimer = 0f
    private var stormDamageTimer = 0f

    // Safe Zone
    var safeZoneRadius = 86f
    var safeZoneCenterX = 0f
    var safeZoneCenterZ = 0f
    private var safeZoneMinRadius = 16f

    // Entities
    val enemies = ArrayList<EnemyActor>()
    val bolts = ArrayList<EnergyBolt>()
    val particles = ArrayList<ParticleFx>()
    val practiceTargetAlive = BooleanArray(5) { true }
    private val practiceTargetRespawn = FloatArray(5) { 0f }

    var gameMode = GameMode.SOLO_SURVIVAL
    var playerCharacterId = "kai"
    var playerOutfit = OutfitConfig()
    var settings = GameSettings()
    var worldMap: WorldMap? = null

    private var arenaWaveTimer = 0f
    private var nextEnemyId = 100

    fun startNewMatch(
        map: WorldMap,
        mode: GameMode,
        characterId: String,
        outfit: OutfitConfig,
        gameSettings: GameSettings
    ) {
        worldMap = map
        gameMode = mode
        playerCharacterId = characterId
        playerOutfit = outfit
        settings = gameSettings

        val charInfo = CharacterCatalog.byId(characterId)
        maxHealth = 100
        health = 100
        maxShield = 100
        shield = 40 + charInfo.armorBonus
        stamina = 1f

        // Spawn player near West Village road
        playerX = -26f
        playerZ = -20f
        playerY = map.getWalkableHeight(playerX, playerZ)
        playerVelY = 0f
        playerYawRad = 0f
        camYawRad = 0f
        camPitchRad = 0.28f
        isAirborne = false
        isCrouching = false
        wantsSprint = false

        activeSlot = 0
        primaryWeapon = ArcadeWeapon.PULSE_RIFLE
        secondaryWeapon = ArcadeWeapon.ARC_SCATTER
        primaryAmmo = primaryWeapon.magSize
        primaryReserve = primaryWeapon.magSize * 3
        secondaryAmmo = ArcadeWeapon.ARC_SCATTER.magSize
        secondaryReserve = ArcadeWeapon.ARC_SCATTER.magSize * 3
        reloadTimer = 0f
        fireCooldown = 0f
        medCells = 2
        shieldCores = 1

        eliminations = 0
        damageDealt = 0
        matchElapsed = 0f
        matchFinished = false
        isVictory = false
        placementRank = 1
        hitMarkerAlpha = 0f
        damageFlashAlpha = 0f
        statusBanner = when (mode) {
            GameMode.SOLO_SURVIVAL -> "SOLO SURVIVAL: Outlast all 9 rivals!"
            GameMode.PRACTICE -> "PRACTICE MODE: Explore & test target drones"
            GameMode.OFFLINE_ARENA -> "OFFLINE ARENA: Eliminate 10 rivals to win!"
        }
        statusBannerTimer = 3.5f

        safeZoneCenterX = 0f
        safeZoneCenterZ = 0f
        safeZoneRadius = if (mode == GameMode.OFFLINE_ARENA) 62f else 86f
        safeZoneMinRadius = if (mode == GameMode.OFFLINE_ARENA) 22f else 15f

        bolts.clear()
        particles.clear()
        enemies.clear()
        for (i in practiceTargetAlive.indices) {
            practiceTargetAlive[i] = true
            practiceTargetRespawn[i] = 0f
        }

        // Spawn AI enemies
        val spawnPoints = listOf(
            Vec3(32f, 0f, 22f),
            Vec3(34f, 0f, -34f),
            Vec3(-34f, 0f, 32f),
            Vec3(0f, 0f, 26f),
            Vec3(12f, 0f, -14f),
            Vec3(-8f, 0f, 38f),
            Vec3(42f, 0f, -10f),
            Vec3(-44f, 0f, -4f),
            Vec3(18f, 0f, 44f)
        )
        val count = mode.enemyCount.coerceAtMost(spawnPoints.size)
        val rng = Random(System.currentTimeMillis())
        for (i in 0 until count) {
            val sp = spawnPoints[i]
            val sx = sp.x + (rng.nextFloat() - 0.5f) * 6f
            val sz = sp.z + (rng.nextFloat() - 0.5f) * 6f
            enemies.add(createEnemy(i + 1, sx, sz, map, rng, i))
        }
        nextEnemyId = 100
        arenaWaveTimer = 0f
    }

    private fun createEnemy(id: Int, x: Float, z: Float, map: WorldMap, rng: Random, wpSeed: Int): EnemyActor {
        val chars = CharacterCatalog.all
        val c = chars[id % chars.size]
        val w = ArcadeWeapon.entries[id % ArcadeWeapon.entries.size]
        return EnemyActor(
            id = id,
            x = x,
            y = map.getWalkableHeight(x, z),
            z = z,
            yawRad = rng.nextFloat() * 2f * PI.toFloat(),
            health = if (gameMode == GameMode.OFFLINE_ARENA) 75 else 85,
            maxHealth = 85,
            characterId = c.id,
            outfit = OutfitConfig(
                hairStyle = id % 4,
                maskStyle = (id % 3) + 1,
                jacketStyle = (id + 1) % 4,
                bootsStyle = id % 3,
                glovesStyle = id % 3,
                backpackStyle = id % 3
            ),
            weapon = w,
            waypointIndex = wpSeed % map.patrolWaypoints.size
        )
    }

    fun rotateCamera(dx: Float, dy: Float) {
        val sens = 0.0055f * settings.cameraSensitivity
        camYawRad -= dx * sens
        val ySign = if (settings.invertY) -1f else 1f
        camPitchRad = Math3D.clamp(camPitchRad + dy * sens * ySign, -0.25f, 1.05f)
    }

    fun rotateLobby(dx: Float) {
        lobbyYawRad += dx * 0.008f
    }

    fun switchWeaponSlot(slot: Int) {
        if (slot == 1 && secondaryWeapon == null) return
        if (activeSlot != slot) {
            activeSlot = slot
            reloadTimer = 0f
            soundEngine?.playUiClick()
        }
    }

    fun useMedCell() {
        if (medCells <= 0 || health >= maxHealth || matchFinished) return
        medCells--
        health = minOf(maxHealth, health + 45)
        soundEngine?.playPickup()
        showBanner("Used Med-Cell (+45 Health)")
    }

    fun useShieldCore() {
        if (shieldCores <= 0 || shield >= maxShield || matchFinished) return
        shieldCores--
        shield = minOf(maxShield, shield + 45)
        soundEngine?.playPickup()
        showBanner("Used Shield Core (+45 Armor)")
    }

    private fun showBanner(text: String) {
        statusBanner = text
        statusBannerTimer = 2.6f
    }

    fun stepLobby(dt: Float) {
        playerAnimTime += dt
        lobbyYawRad += dt * 0.22f
    }

    fun stepMatch(dtRaw: Float) {
        val dt = dtRaw.coerceIn(0.001f, 0.05f)
        val map = worldMap ?: return

        if (matchFinished) {
            playerAnimTime += dt
            updateParticles(dt)
            return
        }

        matchElapsed += dt
        if (hitMarkerAlpha > 0f) hitMarkerAlpha = maxOf(0f, hitMarkerAlpha - dt * 3.5f)
        if (damageFlashAlpha > 0f) damageFlashAlpha = maxOf(0f, damageFlashAlpha - dt * 2.2f)
        if (playerRecoil > 0f) playerRecoil = maxOf(0f, playerRecoil - dt * 6f)
        if (statusBannerTimer > 0f) {
            statusBannerTimer -= dt
            if (statusBannerTimer <= 0f) statusBanner = null
        }

        updatePlayerMovement(dt, map)
        updateWeaponsAndLoot(dt, map)
        updateSafeZone(dt)
        updateEnemies(dt, map)
        updateBolts(dt, map)
        updateParticles(dt)
        updatePracticeTargets(dt)
        checkMatchEndConditions()
    }

    private fun updatePlayerMovement(dt: Float, map: WorldMap) {
        val charInfo = CharacterCatalog.byId(playerCharacterId)
        val inputMag = sqrt(joyX * joyX + joyY * joyY).coerceAtMost(1f)
        val autoSprintActive = settings.autoSprint && inputMag > 0.78f
        val isSprinting = (wantsSprint || autoSprintActive) && !isCrouching && inputMag > 0.25f && stamina > 0.05f

        if (isSprinting) {
            stamina = maxOf(0f, stamina - dt * 0.22f)
        } else {
            stamina = minOf(1f, stamina + dt * 0.18f)
        }

        var moveSpeed = when {
            isCrouching -> 3.6f
            isSprinting -> 9.4f
            else -> 6.2f
        } * charInfo.speedBonus

        val groundH = map.getWalkableHeight(playerX, playerZ)
        val inShallowRiver = !map.isOnBridge(playerX, playerZ) && groundH < map.waterLevel + 0.12f
        if (inShallowRiver) moveSpeed *= 0.68f

        if (inputMag > 0.08f) {
            // Forward vector from camYawRad (camera looks toward -sin(yaw), -cos(yaw) or +sin/+cos)
            val fwdX = sin(camYawRad)
            val fwdZ = cos(camYawRad)
            val rightX = cos(camYawRad)
            val rightZ = -sin(camYawRad)

            // joyY is negative when pushed UP on screen
            val forwardAmount = -joyY
            val strafeAmount = joyX
            val dirX = fwdX * forwardAmount + rightX * strafeAmount
            val dirZ = fwdZ * forwardAmount + rightZ * strafeAmount
            val normLen = sqrt(dirX * dirX + dirZ * dirZ).coerceAtLeast(1e-4f)
            val vx = (dirX / normLen) * moveSpeed * inputMag
            val vz = (dirZ / normLen) * moveSpeed * inputMag

            val nextX = Math3D.clamp(playerX + vx * dt, -map.worldHalfSize + 3f, map.worldHalfSize - 3f)
            if (!collidesWithWorld(nextX, playerY, playerZ, 0.42f, map)) {
                playerX = nextX
            }
            val nextZ = Math3D.clamp(playerZ + vz * dt, -map.worldHalfSize + 3f, map.worldHalfSize - 3f)
            if (!collidesWithWorld(playerX, playerY, nextZ, 0.42f, map)) {
                playerZ = nextZ
            }

            val targetYaw = atan2(vx, vz)
            playerYawRad += Math3D.angleDiffRad(playerYawRad, targetYaw) * minOf(1f, dt * 14f)

            if (!isAirborne) {
                footstepTimer -= dt
                if (footstepTimer <= 0f) {
                    soundEngine?.playFootstep(isSprinting)
                    footstepTimer = if (isSprinting) 0.28f else 0.42f
                }
            }
        }

        // Jump & Gravity
        if (wantsJump) {
            wantsJump = false
            if (!isAirborne) {
                isCrouching = false
                playerVelY = 7.4f
                isAirborne = true
                soundEngine?.playJump()
            }
        }

        val targetGroundY = map.getWalkableHeight(playerX, playerZ)
        if (isAirborne || playerY > targetGroundY + 0.05f) {
            playerVelY -= 19.5f * dt
            playerY += playerVelY * dt
            if (playerY <= targetGroundY) {
                playerY = targetGroundY
                playerVelY = 0f
                isAirborne = false
            }
        } else {
            playerY = targetGroundY
        }

        playerAnimTime += dt
        playerAnimState = when {
            isAirborne -> CharacterAnimState.JUMP
            isCrouching -> CharacterAnimState.CROUCH
            inputMag > 0.1f && isSprinting -> CharacterAnimState.RUN
            inputMag > 0.1f -> CharacterAnimState.WALK
            else -> CharacterAnimState.IDLE
        }
    }

    private fun collidesWithWorld(x: Float, y: Float, z: Float, radius: Float, map: WorldMap): Boolean {
        for (c in map.colliders) {
            if (y > c.maxY) continue
            if (x + radius > c.minX && x - radius < c.maxX &&
                z + radius > c.minZ && z - radius < c.maxZ
            ) {
                return true
            }
        }
        return false
    }

    private fun currentWeapon(): ArcadeWeapon =
        if (activeSlot == 1 && secondaryWeapon != null) secondaryWeapon!! else primaryWeapon

    private fun currentAmmo(): Int = if (activeSlot == 0) primaryAmmo else secondaryAmmo
    private fun setCurrentAmmo(v: Int) {
        if (activeSlot == 0) primaryAmmo = v else secondaryAmmo = v
    }

    private fun currentReserve(): Int = if (activeSlot == 0) primaryReserve else secondaryReserve
    private fun setCurrentReserve(v: Int) {
        if (activeSlot == 0) primaryReserve = v else secondaryReserve = v
    }

    private fun updateWeaponsAndLoot(dt: Float, map: WorldMap) {
        val weapon = currentWeapon()
        if (fireCooldown > 0f) fireCooldown -= dt

        // Reloading
        if (wantsReload) {
            wantsReload = false
            if (reloadTimer <= 0f && currentAmmo() < weapon.magSize && currentReserve() > 0) {
                reloadTimer = weapon.reloadSec
                soundEngine?.playReload()
            }
        }
        if (reloadTimer > 0f) {
            reloadTimer -= dt
            if (reloadTimer <= 0f) {
                val needed = weapon.magSize - currentAmmo()
                val take = minOf(needed, currentReserve())
                setCurrentAmmo(currentAmmo() + take)
                setCurrentReserve(currentReserve() - take)
            }
        }

        // Firing
        if (isFiringInput && fireCooldown <= 0f && reloadTimer <= 0f) {
            if (currentAmmo() > 0) {
                firePlayerWeapon(weapon, map)
                setCurrentAmmo(currentAmmo() - 1)
                fireCooldown = weapon.fireIntervalSec
                if (currentAmmo() == 0 && currentReserve() > 0) {
                    reloadTimer = weapon.reloadSec
                    soundEngine?.playReload()
                }
            } else if (currentReserve() > 0) {
                reloadTimer = weapon.reloadSec
                soundEngine?.playReload()
            }
        }

        // Loot pickup & respawn
        var nearestLoot: WorldLoot? = null
        var nearestDist = 2.8f
        for (loot in map.lootSpawns) {
            if (loot.collected) {
                if (gameMode != GameMode.SOLO_SURVIVAL) {
                    loot.respawnTimer -= dt
                    if (loot.respawnTimer <= 0f) loot.collected = false
                }
                continue
            }
            val d = Math3D.dist2D(playerX, playerZ, loot.x, loot.z)
            if (d < nearestDist) {
                nearestDist = d
                nearestLoot = loot
            }
        }

        if (wantsInteract) {
            wantsInteract = false
            if (nearestLoot != null) {
                collectLoot(nearestLoot)
            }
        }
    }

    private fun collectLoot(loot: WorldLoot) {
        loot.collected = true
        loot.respawnTimer = 25f
        soundEngine?.playPickup()
        when (loot.type) {
            LootType.MED_CELL -> {
                medCells++
                showBanner("Picked up Med-Cell")
            }
            LootType.SHIELD_CORE -> {
                shieldCores++
                showBanner("Picked up Shield Core")
            }
            LootType.AMMO_PACK -> {
                primaryReserve += 40
                if (secondaryWeapon != null) secondaryReserve += 24
                showBanner("Picked up Energy Cells")
            }
            LootType.WEAPON_PULSE -> equipLootWeapon(ArcadeWeapon.PULSE_RIFLE)
            LootType.WEAPON_SCATTER -> equipLootWeapon(ArcadeWeapon.ARC_SCATTER)
            LootType.WEAPON_CARBINE -> equipLootWeapon(ArcadeWeapon.VOLT_CARBINE)
            LootType.WEAPON_LANCE -> equipLootWeapon(ArcadeWeapon.PLASMA_LANCE)
        }
    }

    private fun equipLootWeapon(w: ArcadeWeapon) {
        if (primaryWeapon == w) {
            primaryReserve += w.magSize * 2
            showBanner("${w.shortName} Ammo Refilled")
            return
        }
        if (secondaryWeapon == null || activeSlot == 1) {
            secondaryWeapon = w
            secondaryAmmo = w.magSize
            secondaryReserve = w.magSize * 3
            activeSlot = 1
        } else {
            primaryWeapon = w
            primaryAmmo = w.magSize
            primaryReserve = w.magSize * 3
            activeSlot = 0
        }
        reloadTimer = 0f
        showBanner("Equipped ${w.displayName}")
    }

    private fun firePlayerWeapon(weapon: ArcadeWeapon, map: WorldMap) {
        soundEngine?.playWeaponShot(weapon.ordinal)
        playerRecoil = 1.0f
        // Align player toward camera aim direction
        playerYawRad = camYawRad

        val aimDirX = sin(camYawRad) * cos(camPitchRad * 0.5f)
        val aimDirY = -sin(camPitchRad * 0.55f)
        val aimDirZ = cos(camYawRad) * cos(camPitchRad * 0.5f)
        val muzzleX = playerX + sin(playerYawRad) * 0.6f + cos(playerYawRad) * 0.25f
        val muzzleY = playerY + (if (isCrouching) 0.92f else 1.28f)
        val muzzleZ = playerZ + cos(playerYawRad) * 0.6f - sin(playerYawRad) * 0.25f

        val wColor = Math3D.colorToFloats(weapon.colorHex)
        spawnBurst(muzzleX, muzzleY, muzzleZ, wColor, 3, 2.5f)

        val rng = Random.Default
        for (p in 0 until weapon.pellets) {
            val sx = (rng.nextFloat() - 0.5f) * weapon.spreadRad
            val sy = (rng.nextFloat() - 0.5f) * weapon.spreadRad
            val sz = (rng.nextFloat() - 0.5f) * weapon.spreadRad
            val dx = aimDirX + sx
            val dy = aimDirY + sy
            val dz = aimDirZ + sz
            val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-4f)
            val ndx = dx / len; val ndy = dy / len; val ndz = dz / len

            bolts.add(
                EnergyBolt(
                    x = muzzleX, y = muzzleY, z = muzzleZ,
                    vx = ndx * 58f, vy = ndy * 58f, vz = ndz * 58f,
                    life = 0.65f,
                    fromPlayer = true,
                    color = wColor
                )
            )

            // Instant hitscan check against obstacles, enemies, and practice targets
            var maxHitDist = weapon.range
            for (c in map.colliders) {
                val t = Math3D.rayIntersectsAABB(
                    muzzleX, muzzleY, muzzleZ, ndx, ndy, ndz,
                    c.minX, -4f, c.minZ, c.maxX, c.maxY, c.maxZ, maxHitDist
                )
                if (t != null && t < maxHitDist) maxHitDist = t
            }

            var bestEnemy: EnemyActor? = null
            var bestT = maxHitDist
            for (e in enemies) {
                if (!e.alive) continue
                val t = Math3D.rayIntersectsSphere(
                    muzzleX, muzzleY, muzzleZ, ndx, ndy, ndz,
                    e.x, e.y + 1.05f, e.z, 0.78f, bestT
                )
                if (t != null && t < bestT) {
                    bestT = t
                    bestEnemy = e
                }
            }

            if (bestEnemy != null) {
                damageEnemy(bestEnemy, weapon.damage, true)
                val hx = muzzleX + ndx * bestT
                val hy = muzzleY + ndy * bestT
                val hz = muzzleZ + ndz * bestT
                spawnBurst(hx, hy, hz, floatArrayOf(1f, 0.7f, 0.2f, 1f), 5, 4f)
            } else if (gameMode == GameMode.PRACTICE) {
                for (ti in map.practiceTargets.indices) {
                    if (!practiceTargetAlive[ti]) continue
                    val pt = map.practiceTargets[ti]
                    val t = Math3D.rayIntersectsSphere(
                        muzzleX, muzzleY, muzzleZ, ndx, ndy, ndz,
                        pt.x, pt.y + 1.3f, pt.z, 0.85f, maxHitDist
                    )
                    if (t != null) {
                        practiceTargetAlive[ti] = false
                        practiceTargetRespawn[ti] = 4.0f
                        hitMarkerAlpha = 1f
                        damageDealt += weapon.damage
                        soundEngine?.playHitMarker()
                        spawnBurst(pt.x, pt.y + 1.3f, pt.z, floatArrayOf(0.2f, 0.95f, 1f, 1f), 10, 5f)
                        showBanner("Target Drone Neutralized!")
                        break
                    }
                }
            }
        }
    }

    private fun damageEnemy(enemy: EnemyActor, amount: Int, byPlayer: Boolean) {
        if (!enemy.alive) return
        enemy.health -= amount
        if (byPlayer) {
            damageDealt += amount
            hitMarkerAlpha = 1f
            soundEngine?.playHitMarker()
        }
        if (enemy.health <= 0) {
            enemy.alive = false
            spawnBurst(enemy.x, enemy.y + 1f, enemy.z, floatArrayOf(1f, 0.45f, 0.15f, 1f), 14, 5.5f)
            if (byPlayer) {
                eliminations++
                soundEngine?.playElimination()
                showBanner("Eliminated Rival #${enemy.id}!")
            }
            // Drop a Med-Cell or Shield Core at enemy location
            worldMap?.lootSpawns?.add(
                WorldLoot(
                    id = 5000 + enemy.id,
                    x = enemy.x,
                    y = enemy.y,
                    z = enemy.z,
                    type = if (enemy.id % 2 == 0) LootType.MED_CELL else LootType.SHIELD_CORE
                )
            )
        } else if (enemy.health < 40 && enemy.state != AiBehaviorState.SEEK_COVER) {
            enemy.state = AiBehaviorState.SEEK_COVER
            enemy.coverTarget = findNearestCover(enemy.x, enemy.z)
            enemy.stateTimer = 2.8f
        }
    }

    private fun findNearestCover(ex: Float, ez: Float): Vec3? {
        val map = worldMap ?: return null
        var best: Vec3? = null
        var bestDist = Float.MAX_VALUE
        for (cp in map.coverPoints) {
            val d = Math3D.dist2D(ex, ez, cp.x, cp.z)
            if (d < bestDist && d > 1.5f) {
                bestDist = d
                best = cp
            }
        }
        return best
    }

    private fun updateSafeZone(dt: Float) {
        if (gameMode == GameMode.PRACTICE) return

        val shrinkSpeed = if (gameMode == GameMode.SOLO_SURVIVAL) 0.58f else 0.42f
        if (safeZoneRadius > safeZoneMinRadius) {
            safeZoneRadius = maxOf(safeZoneMinRadius, safeZoneRadius - shrinkSpeed * dt)
        }

        val playerDist = Math3D.dist2D(playerX, playerZ, safeZoneCenterX, safeZoneCenterZ)
        if (playerDist > safeZoneRadius) {
            stormDamageTimer += dt
            if (stormDamageTimer >= 1.0f) {
                stormDamageTimer = 0f
                applyDamageToPlayer(6)
                showBanner("Outside Safe Zone! Move Inward!")
            }
        } else {
            stormDamageTimer = 0f
        }
    }

    private fun updateEnemies(dt: Float, map: WorldMap) {
        val rng = Random.Default

        // Respawn waves in Offline Arena up to 10 eliminations
        if (gameMode == GameMode.OFFLINE_ARENA) {
            val aliveCount = enemies.count { it.alive }
            if (aliveCount < 4 && eliminations + aliveCount < 10) {
                arenaWaveTimer += dt
                if (arenaWaveTimer >= 2.2f) {
                    arenaWaveTimer = 0f
                    val wp = map.patrolWaypoints[nextEnemyId % map.patrolWaypoints.size]
                    enemies.add(createEnemy(nextEnemyId++, wp.x, wp.z, map, rng, nextEnemyId))
                }
            }
        }

        for (e in enemies) {
            if (!e.alive) continue
            e.animTime += dt
            if (e.recoil > 0f) e.recoil = maxOf(0f, e.recoil - dt * 5f)
            if (e.fireCooldown > 0f) e.fireCooldown -= dt

            val distToStorm = Math3D.dist2D(e.x, e.z, safeZoneCenterX, safeZoneCenterZ)
            val distToPlayer = Math3D.dist2D(e.x, e.z, playerX, playerZ)

            // AI State Transitions
            if (gameMode != GameMode.PRACTICE && distToStorm > safeZoneRadius - 3f) {
                e.state = AiBehaviorState.EVADE_STORM
            } else if (e.state == AiBehaviorState.SEEK_COVER) {
                e.stateTimer -= dt
                if (e.stateTimer <= 0f) e.state = AiBehaviorState.ENGAGE
            } else if (distToPlayer < 28f && hasLineOfSight(e.x, e.y + 1.2f, e.z, playerX, playerY + 1.1f, playerZ, map)) {
                e.state = AiBehaviorState.ENGAGE
            } else if (e.state == AiBehaviorState.ENGAGE && distToPlayer > 36f) {
                e.state = AiBehaviorState.PATROL
            }

            var moveTargetX = e.x
            var moveTargetZ = e.z
            var moveSpeed = 0f

            when (e.state) {
                AiBehaviorState.PATROL -> {
                    val wp = map.patrolWaypoints[e.waypointIndex % map.patrolWaypoints.size]
                    moveTargetX = wp.x
                    moveTargetZ = wp.z
                    moveSpeed = 3.8f
                    if (Math3D.dist2D(e.x, e.z, wp.x, wp.z) < 3.0f) {
                        e.waypointIndex = (e.waypointIndex + 1 + rng.nextInt(2)) % map.patrolWaypoints.size
                    }
                }
                AiBehaviorState.EVADE_STORM -> {
                    moveTargetX = safeZoneCenterX
                    moveTargetZ = safeZoneCenterZ
                    moveSpeed = 6.2f
                }
                AiBehaviorState.SEEK_COVER -> {
                    val cp = e.coverTarget
                    if (cp != null) {
                        moveTargetX = cp.x
                        moveTargetZ = cp.z
                        moveSpeed = if (Math3D.dist2D(e.x, e.z, cp.x, cp.z) > 1.4f) 5.6f else 0f
                    }
                }
                AiBehaviorState.ENGAGE -> {
                    // Strafe or close in slightly
                    if (distToPlayer > 14f) {
                        moveTargetX = playerX
                        moveTargetZ = playerZ
                        moveSpeed = 4.2f
                    } else {
                        val strafeAngle = atan2(playerX - e.x, playerZ - e.z) + PI.toFloat() * 0.5f
                        moveTargetX = e.x + sin(strafeAngle) * 4f
                        moveTargetZ = e.z + cos(strafeAngle) * 4f
                        moveSpeed = 2.8f
                    }

                    // Face player & fire
                    val aimYaw = atan2(playerX - e.x, playerZ - e.z)
                    e.yawRad += Math3D.angleDiffRad(e.yawRad, aimYaw) * minOf(1f, dt * 10f)

                    if (e.fireCooldown <= 0f) {
                        e.fireCooldown = e.weapon.fireIntervalSec * 2.8f + rng.nextFloat() * 0.45f
                        e.recoil = 0.8f
                        fireEnemyBolt(e, rng)
                    }
                }
            }

            if (moveSpeed > 0.1f) {
                val dx = moveTargetX - e.x
                val dz = moveTargetZ - e.z
                val d = sqrt(dx * dx + dz * dz).coerceAtLeast(1e-4f)
                val nx = e.x + (dx / d) * moveSpeed * dt
                val nz = e.z + (dz / d) * moveSpeed * dt
                if (!collidesWithWorld(nx, e.y, e.z, 0.4f, map)) e.x = nx
                if (!collidesWithWorld(e.x, e.y, nz, 0.4f, map)) e.z = nz
                e.y = map.getWalkableHeight(e.x, e.z)

                if (e.state != AiBehaviorState.ENGAGE) {
                    val targetYaw = atan2(dx, dz)
                    e.yawRad += Math3D.angleDiffRad(e.yawRad, targetYaw) * minOf(1f, dt * 8f)
                }
                e.animState = if (moveSpeed > 5f) CharacterAnimState.RUN else CharacterAnimState.WALK
            } else {
                e.animState = if (e.state == AiBehaviorState.SEEK_COVER) CharacterAnimState.CROUCH else CharacterAnimState.IDLE
            }
        }
    }

    private fun fireEnemyBolt(e: EnemyActor, rng: Random) {
        val dx = playerX - e.x + (rng.nextFloat() - 0.5f) * 2.2f
        val dy = (playerY + 1.05f) - (e.y + 1.2f) + (rng.nextFloat() - 0.5f) * 0.4f
        val dz = playerZ - e.z + (rng.nextFloat() - 0.5f) * 2.2f
        val len = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-4f)
        val ndx = dx / len; val ndy = dy / len; val ndz = dz / len

        bolts.add(
            EnergyBolt(
                x = e.x + ndx * 0.7f,
                y = e.y + 1.25f,
                z = e.z + ndz * 0.7f,
                vx = ndx * 36f,
                vy = ndy * 36f,
                vz = ndz * 36f,
                life = 1.4f,
                fromPlayer = false,
                color = floatArrayOf(1f, 0.28f, 0.25f, 1f)
            )
        )
    }

    private fun hasLineOfSight(x0: Float, y0: Float, z0: Float, x1: Float, y1: Float, z1: Float, map: WorldMap): Boolean {
        val dx = x1 - x0; val dy = y1 - y0; val dz = z1 - z0
        val dist = sqrt(dx * dx + dy * dy + dz * dz).coerceAtLeast(1e-4f)
        val ndx = dx / dist; val ndy = dy / dist; val ndz = dz / dist
        for (c in map.colliders) {
            if (c.isTree) continue
            val hit = Math3D.rayIntersectsAABB(x0, y0, z0, ndx, ndy, ndz, c.minX, -4f, c.minZ, c.maxX, c.maxY, c.maxZ, dist)
            if (hit != null && hit < dist) return false
        }
        return true
    }

    private fun updateBolts(dt: Float, map: WorldMap) {
        val iter = bolts.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            b.z += b.vz * dt
            b.life -= dt
            if (b.life <= 0f || b.y < map.getRawTerrainHeight(b.x, b.z)) {
                iter.remove()
                continue
            }
            if (!b.fromPlayer) {
                val pCenterY = playerY + (if (isCrouching) 0.7f else 1.05f)
                val dx = b.x - playerX; val dy = b.y - pCenterY; val dz = b.z - playerZ
                if (dx * dx + dy * dy + dz * dz < 0.75f) {
                    applyDamageToPlayer(9)
                    spawnBurst(b.x, b.y, b.z, floatArrayOf(1f, 0.25f, 0.25f, 1f), 5, 3.5f)
                    iter.remove()
                    continue
                }
            }
        }
    }

    private fun applyDamageToPlayer(dmg: Int) {
        if (matchFinished) return
        damageFlashAlpha = 0.85f
        var rem = dmg
        if (shield > 0) {
            val absorbed = minOf(shield, rem)
            shield -= absorbed
            rem -= absorbed
        }
        if (rem > 0) {
            health = maxOf(0, health - rem)
        }
        if (health <= 0) {
            matchFinished = true
            isVictory = false
            placementRank = enemies.count { it.alive } + 1
            soundEngine?.playUiClick()
        }
    }

    private fun updateParticles(dt: Float) {
        val iter = particles.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            p.vy -= 8f * dt
            p.life -= dt
            if (p.life <= 0f) iter.remove()
        }
    }

    private fun updatePracticeTargets(dt: Float) {
        if (gameMode != GameMode.PRACTICE) return
        for (i in practiceTargetAlive.indices) {
            if (!practiceTargetAlive[i]) {
                practiceTargetRespawn[i] -= dt
                if (practiceTargetRespawn[i] <= 0f) practiceTargetAlive[i] = true
            }
        }
    }

    private fun spawnBurst(x: Float, y: Float, z: Float, color: FloatArray, count: Int, speed: Float) {
        if (settings.graphicsQuality == com.example.model.GraphicsQuality.LOW) return
        val rng = Random.Default
        for (i in 0 until count) {
            if (particles.size > 90) break
            particles.add(
                ParticleFx(
                    x = x, y = y, z = z,
                    vx = (rng.nextFloat() - 0.5f) * speed,
                    vy = rng.nextFloat() * speed * 0.7f,
                    vz = (rng.nextFloat() - 0.5f) * speed,
                    life = 0.35f + rng.nextFloat() * 0.25f,
                    maxLife = 0.6f,
                    size = 0.14f + rng.nextFloat() * 0.12f,
                    color = color
                )
            )
        }
    }

    private fun checkMatchEndConditions() {
        when (gameMode) {
            GameMode.SOLO_SURVIVAL -> {
                val aliveEnemies = enemies.count { it.alive }
                if (aliveEnemies == 0 && !matchFinished) {
                    matchFinished = true
                    isVictory = true
                    placementRank = 1
                    playerAnimState = CharacterAnimState.VICTORY
                }
            }
            GameMode.OFFLINE_ARENA -> {
                if (eliminations >= 10 && !matchFinished) {
                    matchFinished = true
                    isVictory = true
                    placementRank = 1
                    playerAnimState = CharacterAnimState.VICTORY
                }
            }
            GameMode.PRACTICE -> Unit
        }
    }

    fun finishPracticeMatch() {
        if (gameMode == GameMode.PRACTICE && !matchFinished) {
            matchFinished = true
            isVictory = true
            placementRank = 1
        }
    }

    fun buildHudSnapshot(): HudSnapshot {
        val map = worldMap
        val half = map?.worldHalfSize ?: 86f
        val normScale = half * 2f

        var nearbyPrompt: String? = null
        if (map != null) {
            var bestD = 2.8f
            for (l in map.lootSpawns) {
                if (l.collected) continue
                val d = Math3D.dist2D(playerX, playerZ, l.x, l.z)
                if (d < bestD) {
                    bestD = d
                    nearbyPrompt = l.label()
                }
            }
        }

        val blips = ArrayList<MinimapBlip>()
        for (e in enemies) {
            if (!e.alive) continue
            if (Math3D.dist2D(playerX, playerZ, e.x, e.z) < 55f) {
                blips.add(
                    MinimapBlip(
                        normX = (e.x + half) / normScale,
                        normZ = (e.z + half) / normScale,
                        isEnemy = true
                    )
                )
            }
        }
        if (map != null) {
            for (l in map.lootSpawns) {
                if (l.collected) continue
                if (Math3D.dist2D(playerX, playerZ, l.x, l.z) < 34f) {
                    blips.add(
                        MinimapBlip(
                            normX = (l.x + half) / normScale,
                            normZ = (l.z + half) / normScale,
                            isLoot = true
                        )
                    )
                }
            }
        }

        val aliveEnemies = enemies.count { it.alive }
        val totalSurvivors = if (gameMode == GameMode.PRACTICE) 1 else (gameMode.enemyCount + 1)
        val aliveTotal = if (gameMode == GameMode.PRACTICE) 1 else (aliveEnemies + if (health > 0) 1 else 0)

        return HudSnapshot(
            health = health,
            maxHealth = maxHealth,
            shield = shield,
            maxShield = maxShield,
            stamina = stamina,
            aliveCount = aliveTotal,
            totalSurvivors = totalSurvivors,
            eliminations = eliminations,
            damageDealt = damageDealt,
            matchSeconds = matchElapsed.toInt(),
            safeZoneRadius = safeZoneRadius,
            safeZoneCenterX = safeZoneCenterX,
            safeZoneCenterZ = safeZoneCenterZ,
            isInSafeZone = Math3D.dist2D(playerX, playerZ, safeZoneCenterX, safeZoneCenterZ) <= safeZoneRadius,
            isCrouching = isCrouching,
            isSprinting = playerAnimState == CharacterAnimState.RUN,
            isAirborne = isAirborne,
            activeSlot = activeSlot,
            primaryWeapon = primaryWeapon,
            secondaryWeapon = secondaryWeapon,
            primaryAmmo = primaryAmmo,
            primaryReserve = primaryReserve,
            secondaryAmmo = secondaryAmmo,
            secondaryReserve = secondaryReserve,
            isReloading = reloadTimer > 0f,
            medCells = medCells,
            shieldCores = shieldCores,
            nearbyLootPrompt = nearbyPrompt,
            hitMarkerAlpha = hitMarkerAlpha,
            damageFlashAlpha = damageFlashAlpha,
            statusBanner = statusBanner,
            playerNormX = (playerX + half) / normScale,
            playerNormZ = (playerZ + half) / normScale,
            playerYawRad = camYawRad,
            mapBlips = blips,
            matchFinished = matchFinished,
            isVictory = isVictory,
            placementRank = placementRank
        )
    }
}
