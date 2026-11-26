package com.example.engine

import com.example.model.ArcadeWeapon
import com.example.model.GameMapId
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class WorldCollider(
    val minX: Float,
    val maxX: Float,
    val minZ: Float,
    val maxZ: Float,
    val maxY: Float,
    val isTree: Boolean = false
)

enum class LootType {
    WEAPON_PULSE,
    WEAPON_SCATTER,
    WEAPON_CARBINE,
    WEAPON_LANCE,
    MED_CELL,
    SHIELD_CORE,
    AMMO_PACK
}

data class WorldLoot(
    val id: Int,
    val x: Float,
    var y: Float,
    val z: Float,
    val type: LootType,
    var collected: Boolean = false,
    var respawnTimer: Float = 0f
) {
    fun label(): String = when (type) {
        LootType.WEAPON_PULSE -> ArcadeWeapon.PULSE_RIFLE.displayName
        LootType.WEAPON_SCATTER -> ArcadeWeapon.ARC_SCATTER.displayName
        LootType.WEAPON_CARBINE -> ArcadeWeapon.VOLT_CARBINE.displayName
        LootType.WEAPON_LANCE -> ArcadeWeapon.PLASMA_LANCE.displayName
        LootType.MED_CELL -> "Med-Cell (+40 HP)"
        LootType.SHIELD_CORE -> "Shield Core (+40 AP)"
        LootType.AMMO_PACK -> "Energy Cells (+40)"
    }
}

data class BiomePalette(
    val zenithSky: FloatArray,
    val horizonSky: FloatArray,
    val sunDir: FloatArray,
    val sunColor: FloatArray,
    val ambientSky: FloatArray,
    val ambientGround: FloatArray,
    val fogColor: FloatArray,
    val grassColor: FloatArray,
    val hillColor: FloatArray,
    val sandColor: FloatArray,
    val roadColor: FloatArray,
    val waterColor: FloatArray,
    val treeFoliage1: FloatArray,
    val treeFoliage2: FloatArray,
    val wallColor: FloatArray,
    val roofColor: FloatArray
)

data class MinimapLandmark(
    val normX: Float,
    val normZ: Float,
    val normW: Float,
    val normH: Float,
    val isBridge: Boolean = false
)

class WorldMap(val mapId: GameMapId) {
    val worldHalfSize = 86f
    val waterLevel = -0.55f
    val colliders = ArrayList<WorldCollider>()
    val lootSpawns = ArrayList<WorldLoot>()
    val coverPoints = ArrayList<Vec3>()
    val patrolWaypoints = ArrayList<Vec3>()
    val minimapLandmarks = ArrayList<MinimapLandmark>()
    val practiceTargets = ArrayList<Vec3>()

    val palette: BiomePalette = when (mapId) {
        GameMapId.EMERALD_ISLE -> BiomePalette(
            zenithSky = floatArrayOf(0.14f, 0.42f, 0.78f),
            horizonSky = floatArrayOf(0.66f, 0.84f, 0.96f),
            sunDir = floatArrayOf(0.55f, 0.72f, 0.42f),
            sunColor = floatArrayOf(1.0f, 0.94f, 0.80f),
            ambientSky = floatArrayOf(0.58f, 0.76f, 0.95f),
            ambientGround = floatArrayOf(0.28f, 0.36f, 0.24f),
            fogColor = floatArrayOf(0.62f, 0.80f, 0.92f),
            grassColor = floatArrayOf(0.24f, 0.56f, 0.25f),
            hillColor = floatArrayOf(0.36f, 0.48f, 0.29f),
            sandColor = floatArrayOf(0.79f, 0.72f, 0.50f),
            roadColor = floatArrayOf(0.52f, 0.44f, 0.34f),
            waterColor = floatArrayOf(0.11f, 0.46f, 0.68f),
            treeFoliage1 = floatArrayOf(0.15f, 0.46f, 0.21f),
            treeFoliage2 = floatArrayOf(0.22f, 0.55f, 0.25f),
            wallColor = floatArrayOf(0.84f, 0.80f, 0.72f),
            roofColor = floatArrayOf(0.72f, 0.29f, 0.22f)
        )
        GameMapId.CANYON_OUTPOST -> BiomePalette(
            zenithSky = floatArrayOf(0.26f, 0.29f, 0.55f),
            horizonSky = floatArrayOf(0.95f, 0.63f, 0.39f),
            sunDir = floatArrayOf(-0.60f, 0.58f, 0.54f),
            sunColor = floatArrayOf(1.0f, 0.80f, 0.55f),
            ambientSky = floatArrayOf(0.84f, 0.62f, 0.48f),
            ambientGround = floatArrayOf(0.42f, 0.26f, 0.18f),
            fogColor = floatArrayOf(0.86f, 0.60f, 0.42f),
            grassColor = floatArrayOf(0.68f, 0.46f, 0.26f),
            hillColor = floatArrayOf(0.58f, 0.34f, 0.22f),
            sandColor = floatArrayOf(0.82f, 0.64f, 0.42f),
            roadColor = floatArrayOf(0.46f, 0.36f, 0.28f),
            waterColor = floatArrayOf(0.16f, 0.43f, 0.49f),
            treeFoliage1 = floatArrayOf(0.36f, 0.44f, 0.22f),
            treeFoliage2 = floatArrayOf(0.44f, 0.49f, 0.25f),
            wallColor = floatArrayOf(0.76f, 0.68f, 0.58f),
            roofColor = floatArrayOf(0.44f, 0.35f, 0.30f)
        )
        GameMapId.TWILIGHT_PINES -> BiomePalette(
            zenithSky = floatArrayOf(0.06f, 0.11f, 0.24f),
            horizonSky = floatArrayOf(0.35f, 0.44f, 0.62f),
            sunDir = floatArrayOf(0.38f, 0.48f, -0.78f),
            sunColor = floatArrayOf(0.84f, 0.86f, 1.0f),
            ambientSky = floatArrayOf(0.36f, 0.46f, 0.68f),
            ambientGround = floatArrayOf(0.16f, 0.22f, 0.24f),
            fogColor = floatArrayOf(0.28f, 0.36f, 0.50f),
            grassColor = floatArrayOf(0.16f, 0.34f, 0.24f),
            hillColor = floatArrayOf(0.25f, 0.32f, 0.34f),
            sandColor = floatArrayOf(0.46f, 0.47f, 0.44f),
            roadColor = floatArrayOf(0.34f, 0.35f, 0.38f),
            waterColor = floatArrayOf(0.12f, 0.30f, 0.46f),
            treeFoliage1 = floatArrayOf(0.09f, 0.27f, 0.20f),
            treeFoliage2 = floatArrayOf(0.13f, 0.34f, 0.25f),
            wallColor = floatArrayOf(0.58f, 0.53f, 0.46f),
            roofColor = floatArrayOf(0.24f, 0.29f, 0.36f)
        )
    }

    private val seed: Int = when (mapId) {
        GameMapId.EMERALD_ISLE -> 101
        GameMapId.CANYON_OUTPOST -> 207
        GameMapId.TWILIGHT_PINES -> 313
    }

    fun riverCenterX(z: Float): Float = sin(z * 0.045f) * 14f + cos(z * 0.02f) * 4f

    fun isOnRoad(x: Float, z: Float): Boolean {
        val bridge1Z = -26f
        val bridge2Z = 24f
        if (abs(z - bridge1Z) < 2.6f && abs(x) < 46f) return true
        if (abs(z - bridge2Z) < 2.6f && abs(x) < 46f) return true
        if (abs(x - (-30f)) < 2.4f && z in -40f..40f) return true
        if (abs(x - 32f) < 2.4f && z in -38f..38f) return true
        return false
    }

    fun isOnBridge(x: Float, z: Float): Boolean {
        val b1X = riverCenterX(-26f)
        val b2X = riverCenterX(24f)
        return (abs(z - (-26f)) < 2.6f && abs(x - b1X) < 7.5f) ||
            (abs(z - 24f) < 2.6f && abs(x - b2X) < 7.5f)
    }

    fun getRawTerrainHeight(x: Float, z: Float): Float {
        val n1 = Math3D.fbm2D(x * 0.022f + 10f, z * 0.022f + 10f, seed, 3)
        var h = (n1 - 0.44f) * 14f

        // Gentle island rim falloff near beaches
        val edgeDist = maxOf(abs(x), abs(z))
        if (edgeDist > 68f) {
            val fall = Math3D.smoothstep(68f, 88f, edgeDist)
            h -= fall * 6.5f
        }

        // Flatten village & military outpost plateaus
        val westVillageDist = Math3D.dist2D(x, z, -30f, -20f)
        if (westVillageDist < 18f) {
            val flat = 1f - Math3D.smoothstep(9f, 18f, westVillageDist)
            h = Math3D.lerp(h, 1.4f, flat)
        }
        val eastVillageDist = Math3D.dist2D(x, z, 32f, 20f)
        if (eastVillageDist < 18f) {
            val flat = 1f - Math3D.smoothstep(9f, 18f, eastVillageDist)
            h = Math3D.lerp(h, 1.6f, flat)
        }
        val milBaseDist = Math3D.dist2D(x, z, 34f, -34f)
        if (milBaseDist < 16f) {
            val flat = 1f - Math3D.smoothstep(8f, 16f, milBaseDist)
            h = Math3D.lerp(h, 2.8f, flat)
        }
        val ruinsDist = Math3D.dist2D(x, z, -34f, 32f)
        if (ruinsDist < 15f) {
            val flat = 1f - Math3D.smoothstep(8f, 15f, ruinsDist)
            h = Math3D.lerp(h, 1.8f, flat)
        }

        // Carve winding river bed
        val rx = riverCenterX(z)
        val distToRiver = abs(x - rx)
        if (distToRiver < 8.5f && abs(z) < 76f) {
            val carve = 1f - Math3D.smoothstep(3.5f, 8.5f, distToRiver)
            h = Math3D.lerp(h, -1.6f, carve)
        }

        return h
    }

    fun getWalkableHeight(x: Float, z: Float): Float {
        if (isOnBridge(x, z)) return 0.55f
        return getRawTerrainHeight(x, z)
    }

    fun generateLayoutOnly() {
        buildWorldInternal(grassCount = 0, buildMeshes = false)
    }

    fun buildStaticGeometry(grassCount: Int): Triple<Mesh, Mesh, Mesh> {
        return buildWorldInternal(grassCount = grassCount, buildMeshes = true)!!
    }

    private fun buildWorldInternal(grassCount: Int, buildMeshes: Boolean): Triple<Mesh, Mesh, Mesh>? {
        colliders.clear()
        lootSpawns.clear()
        coverPoints.clear()
        patrolWaypoints.clear()
        minimapLandmarks.clear()
        practiceTargets.clear()

        val worldBuilder = if (buildMeshes) MeshDataBuilder() else null
        val waterBuilder = if (buildMeshes) MeshDataBuilder() else null
        val grassBuilder = if (buildMeshes) MeshDataBuilder() else null

        if (worldBuilder != null && waterBuilder != null) {
            // 1. Terrain Grid (84x84 cells across 172x172m)
            val gridN = 84
            val step = (worldHalfSize * 2f) / gridN
            val start = -worldHalfSize

            val heights = Array(gridN + 1) { FloatArray(gridN + 1) }
            for (iz in 0..gridN) {
                val z = start + iz * step
                for (ix in 0..gridN) {
                    val x = start + ix * step
                    heights[iz][ix] = getRawTerrainHeight(x, z)
                }
            }

            fun vertexColor(x: Float, z: Float, h: Float, slope: Float): FloatArray {
                if (isOnRoad(x, z) && abs(x - riverCenterX(z)) > 5.5f && h > waterLevel + 0.1f) {
                    return palette.roadColor
                }
                if (h < waterLevel + 0.45f) return palette.sandColor
                if (slope > 0.34f || h > 5.5f) return palette.hillColor
                return palette.grassColor
            }

            for (iz in 0 until gridN) {
                val z0 = start + iz * step
                val z1 = z0 + step
                for (ix in 0 until gridN) {
                    val x0 = start + ix * step
                    val x1 = x0 + step

                    val h00 = heights[iz][ix]
                    val h10 = heights[iz][ix + 1]
                    val h01 = heights[iz + 1][ix]
                    val h11 = heights[iz + 1][ix + 1]

                    val nx = ((h00 - h10) + (h01 - h11)) * 0.5f / step
                    val nz = ((h00 - h01) + (h10 - h11)) * 0.5f / step
                    val nLen = kotlin.math.sqrt(nx * nx + 1f + nz * nz)
                    val nrmX = nx / nLen; val nrmY = 1f / nLen; val nrmZ = nz / nLen
                    val slope = 1f - nrmY

                    val c00 = vertexColor(x0, z0, h00, slope)
                    val c10 = vertexColor(x1, z0, h10, slope)
                    val c01 = vertexColor(x0, z1, h01, slope)
                    val c11 = vertexColor(x1, z1, h11, slope)

                    worldBuilder.addTri(
                        x0, h00, z0, nrmX, nrmY, nrmZ, c00[0], c00[1], c00[2], 1f,
                        x0, h01, z1, nrmX, nrmY, nrmZ, c01[0], c01[1], c01[2], 1f,
                        x1, h11, z1, nrmX, nrmY, nrmZ, c11[0], c11[1], c11[2], 1f
                    )
                    worldBuilder.addTri(
                        x0, h00, z0, nrmX, nrmY, nrmZ, c00[0], c00[1], c00[2], 1f,
                        x1, h11, z1, nrmX, nrmY, nrmZ, c11[0], c11[1], c11[2], 1f,
                        x1, h10, z0, nrmX, nrmY, nrmZ, c10[0], c10[1], c10[2], 1f
                    )
                }
            }

            // 2. Water Plane (Subdivided for gentle vertex waves)
            val wGrid = 36
            val wStep = (worldHalfSize * 2.2f) / wGrid
            val wStart = -worldHalfSize * 1.1f
            val wc = palette.waterColor
            for (iz in 0 until wGrid) {
                val z0 = wStart + iz * wStep
                val z1 = z0 + wStep
                for (ix in 0 until wGrid) {
                    val x0 = wStart + ix * wStep
                    val x1 = x0 + wStep
                    waterBuilder.addQuad(
                        x0, waterLevel, z0,
                        x0, waterLevel, z1,
                        x1, waterLevel, z1,
                        x1, waterLevel, z0,
                        0f, 1f, 0f,
                        wc[0], wc[1], wc[2], 0.84f
                    )
                }
            }
        }

        // 3. Bridges across the river
        addBridge(worldBuilder, riverCenterX(-26f), -26f)
        addBridge(worldBuilder, riverCenterX(24f), 24f)

        // 4. West Village (Houses)
        addHouse(worldBuilder, -36f, -18f, 6.2f, 3.6f, 5.4f, false)
        addHouse(worldBuilder, -24f, -15f, 5.4f, 3.4f, 4.8f, true)
        addHouse(worldBuilder, -34f, -31f, 6.8f, 3.8f, 5.2f, false)
        addHouse(worldBuilder, -22f, -32f, 5.0f, 3.2f, 5.0f, true)

        // 5. East Village (Houses)
        addHouse(worldBuilder, 26f, 15f, 6.0f, 3.6f, 5.2f, false)
        addHouse(worldBuilder, 39f, 16f, 5.6f, 3.4f, 5.0f, true)
        addHouse(worldBuilder, 27f, 30f, 6.5f, 3.8f, 5.4f, false)
        addHouse(worldBuilder, 40f, 31f, 5.2f, 3.3f, 4.8f, true)

        // 6. Military-Inspired Outpost (NE Quadrant)
        addMilitaryOutpost(worldBuilder, 34f, -34f)

        // 7. Abandoned Concrete Ruins (SW Quadrant)
        addAbandonedWarehouse(worldBuilder, -34f, 32f)
        addAbandonedWarehouse(worldBuilder, -44f, 22f)

        // 8. Trees & Rocks scattered naturally
        val rng = Random(seed + 77)
        for (i in 0 until 68) {
            val tx = rng.nextFloat() * 144f - 72f
            val tz = rng.nextFloat() * 144f - 72f
            val th = getRawTerrainHeight(tx, tz)
            if (th < waterLevel + 0.5f) continue
            if (isOnRoad(tx, tz)) continue
            if (Math3D.dist2D(tx, tz, -30f, -22f) < 16f) continue
            if (Math3D.dist2D(tx, tz, 32f, 22f) < 16f) continue
            if (Math3D.dist2D(tx, tz, 34f, -34f) < 14f) continue
            if (Math3D.dist2D(tx, tz, -36f, 28f) < 14f) continue
            if (abs(tx - riverCenterX(tz)) < 7.5f) continue

            if (i % 4 == 0) {
                addRock(worldBuilder, tx, th, tz, 1.0f + rng.nextFloat() * 1.1f)
            } else {
                addTree(worldBuilder, tx, th, tz, 0.85f + rng.nextFloat() * 0.65f, i % 2 == 0)
            }
        }

        // 9. Supply Crates & Loot Pickups across Villages, Outpost, Bridges, and Fields
        val lootLocations = listOf(
            Triple(-30f, -22f, LootType.WEAPON_PULSE),
            Triple(-36f, -12f, LootType.MED_CELL),
            Triple(-24f, -24f, LootType.SHIELD_CORE),
            Triple(32f, 22f, LootType.WEAPON_CARBINE),
            Triple(39f, 23f, LootType.MED_CELL),
            Triple(26f, 22f, LootType.AMMO_PACK),
            Triple(34f, -34f, LootType.WEAPON_LANCE),
            Triple(29f, -30f, LootType.SHIELD_CORE),
            Triple(39f, -38f, LootType.AMMO_PACK),
            Triple(-34f, 32f, LootType.WEAPON_SCATTER),
            Triple(-42f, 27f, LootType.MED_CELL),
            Triple(riverCenterX(-26f), -26f, LootType.AMMO_PACK),
            Triple(riverCenterX(24f), 24f, LootType.SHIELD_CORE),
            Triple(0f, 0f, LootType.WEAPON_PULSE),
            Triple(-12f, 8f, LootType.MED_CELL),
            Triple(14f, -10f, LootType.WEAPON_CARBINE),
            Triple(8f, 44f, LootType.WEAPON_SCATTER),
            Triple(-10f, -44f, LootType.WEAPON_LANCE)
        )
        lootLocations.forEachIndexed { idx, (lx, lz, type) ->
            val ly = getWalkableHeight(lx, lz)
            lootSpawns.add(WorldLoot(idx, lx, ly, lz, type))
        }

        // Practice targets for Practice Mode
        practiceTargets.add(Vec3(-12f, getWalkableHeight(-12f, -6f), -6f))
        practiceTargets.add(Vec3(12f, getWalkableHeight(12f, -8f), -8f))
        practiceTargets.add(Vec3(0f, getWalkableHeight(0f, 16f), 16f))
        practiceTargets.add(Vec3(-20f, getWalkableHeight(-20f, 12f), 12f))
        practiceTargets.add(Vec3(22f, getWalkableHeight(22f, -14f), -14f))

        // Patrol waypoints for AI
        patrolWaypoints.addAll(
            listOf(
                Vec3(-30f, 0f, -22f),
                Vec3(riverCenterX(-26f), 0f, -26f),
                Vec3(34f, 0f, -34f),
                Vec3(32f, 0f, 22f),
                Vec3(riverCenterX(24f), 0f, 24f),
                Vec3(-34f, 0f, 32f),
                Vec3(0f, 0f, 0f),
                Vec3(-16f, 0f, 4f),
                Vec3(16f, 0f, -4f)
            )
        )

        // 10. Animated Grass Tufts
        if (grassBuilder != null) {
            val gc = palette.grassColor
            for (i in 0 until grassCount) {
                val gx = rng.nextFloat() * 124f - 62f
                val gz = rng.nextFloat() * 124f - 62f
                val gh = getRawTerrainHeight(gx, gz)
                if (gh < waterLevel + 0.55f || isOnRoad(gx, gz)) continue
                if (abs(gx - riverCenterX(gz)) < 6.5f) continue
                val bladeH = 0.55f + rng.nextFloat() * 0.45f
                val w = 0.14f
                val r = minOf(1f, gc[0] * 1.25f)
                val g = minOf(1f, gc[1] * 1.28f)
                val b = minOf(1f, gc[2] * 1.15f)
                grassBuilder.addTri(
                    gx - w, gh, gz, 0f, 1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, 1f,
                    gx + w, gh, gz, 0f, 1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, 1f,
                    gx, gh + bladeH, gz + 0.05f, 0f, 1f, 0f, r, g, b, 1f
                )
                grassBuilder.addTri(
                    gx, gh, gz - w, 0f, 1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, 1f,
                    gx, gh, gz + w, 0f, 1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, 1f,
                    gx + 0.04f, gh + bladeH, gz, 0f, 1f, 0f, r, g, b, 1f
                )
            }
        }

        return if (worldBuilder != null && waterBuilder != null && grassBuilder != null) {
            Triple(worldBuilder.build(), waterBuilder.build(), grassBuilder.build())
        } else {
            null
        }
    }

    private fun addBridge(b: MeshDataBuilder?, cx: Float, cz: Float) {
        val len = 15.5f
        val wid = 4.6f
        val deckY = 0.42f
        if (b != null) {
            // Deck
            b.addBox(cx, deckY, cz, len, 0.35f, wid, 0.48f, 0.36f, 0.24f)
            // Side rails
            b.addBox(cx, deckY + 0.55f, cz - wid * 0.46f, len, 0.32f, 0.22f, 0.38f, 0.28f, 0.20f)
            b.addBox(cx, deckY + 0.55f, cz + wid * 0.46f, len, 0.32f, 0.22f, 0.38f, 0.28f, 0.20f)
            // Support pillars in water
            for (px in listOf(cx - 5f, cx, cx + 5f)) {
                b.addBox(px, -0.7f, cz - wid * 0.42f, 0.5f, 2.6f, 0.5f, 0.35f, 0.35f, 0.38f)
                b.addBox(px, -0.7f, cz + wid * 0.42f, 0.5f, 2.6f, 0.5f, 0.35f, 0.35f, 0.38f)
            }
        }
        minimapLandmarks.add(
            MinimapLandmark(
                normX = (cx + worldHalfSize) / (worldHalfSize * 2f),
                normZ = (cz + worldHalfSize) / (worldHalfSize * 2f),
                normW = len / (worldHalfSize * 2f),
                normH = wid / (worldHalfSize * 2f),
                isBridge = true
            )
        )
    }

    private fun addHouse(b: MeshDataBuilder?, cx: Float, cz: Float, w: Float, h: Float, d: Float, altRoof: Boolean) {
        val baseY = getRawTerrainHeight(cx, cz) - 0.35f
        if (b != null) {
            val wallC = palette.wallColor
            val roofC = if (altRoof) floatArrayOf(palette.roofColor[0] * 0.8f, palette.roofColor[1] * 1.15f, palette.roofColor[2] * 1.25f) else palette.roofColor

            // Main walls
            b.addBox(cx, baseY + h * 0.5f, cz, w, h, d, wallC[0], wallC[1], wallC[2])
            // Foundation trim
            b.addBox(cx, baseY + 0.25f, cz, w + 0.25f, 0.5f, d + 0.25f, 0.36f, 0.36f, 0.38f)
            // Door & window accents
            b.addBox(cx, baseY + 1.1f, cz + d * 0.5f + 0.04f, 1.1f, 2.0f, 0.1f, 0.20f, 0.16f, 0.14f)
            b.addBox(cx - w * 0.28f, baseY + 2.1f, cz + d * 0.5f + 0.04f, 0.9f, 0.8f, 0.1f, 0.26f, 0.42f, 0.54f)
            b.addBox(cx + w * 0.28f, baseY + 2.1f, cz + d * 0.5f + 0.04f, 0.9f, 0.8f, 0.1f, 0.26f, 0.42f, 0.54f)
            // Pitched stepped roof
            b.addBox(cx, baseY + h + 0.35f, cz, w + 0.7f, 0.7f, d + 0.7f, roofC[0], roofC[1], roofC[2])
            b.addBox(cx, baseY + h + 0.9f, cz, w * 0.72f, 0.6f, d * 0.72f, roofC[0] * 0.92f, roofC[1] * 0.92f, roofC[2] * 0.92f)
        }

        colliders.add(WorldCollider(cx - w * 0.5f, cx + w * 0.5f, cz - d * 0.5f, cz + d * 0.5f, baseY + h + 1.2f))
        coverPoints.add(Vec3(cx - w * 0.75f, baseY + 0.35f, cz))
        coverPoints.add(Vec3(cx + w * 0.75f, baseY + 0.35f, cz))
        minimapLandmarks.add(
            MinimapLandmark(
                normX = (cx + worldHalfSize) / (worldHalfSize * 2f),
                normZ = (cz + worldHalfSize) / (worldHalfSize * 2f),
                normW = w / (worldHalfSize * 2f),
                normH = d / (worldHalfSize * 2f)
            )
        )
    }

    private fun addMilitaryOutpost(b: MeshDataBuilder?, cx: Float, cz: Float) {
        val baseY = getRawTerrainHeight(cx, cz) - 0.3f
        val wx = cx + 6.5f; val wz = cz - 5.5f
        val bx = cx - 6.0f; val bz = cz + 4.5f
        val sx = cx - 5.5f; val sz = cz - 5.5f

        if (b != null) {
            // Concrete Helipad / Supply Pad
            b.addBox(cx, baseY + 0.18f, cz, 12f, 0.36f, 12f, 0.38f, 0.40f, 0.42f)
            b.addBox(cx, baseY + 0.38f, cz, 4.5f, 0.06f, 4.5f, 0.95f, 0.68f, 0.18f)

            // Watchtower
            b.addBox(wx, baseY + 3.2f, wz, 2.8f, 6.4f, 2.8f, 0.34f, 0.38f, 0.32f)
            b.addBox(wx, baseY + 6.6f, wz, 3.8f, 0.5f, 3.8f, 0.25f, 0.28f, 0.25f)
            b.addBox(wx, baseY + 8.4f, wz, 3.4f, 0.4f, 3.4f, 0.42f, 0.45f, 0.40f)
            // Radar mast
            b.addCylinder(wx, baseY + 8.6f, wz, 0.16f, 0.12f, 2.4f, 8, 0.75f, 0.78f, 0.82f)
            b.addBox(wx + 0.4f, baseY + 10.6f, wz, 1.4f, 0.8f, 0.3f, 0.90f, 0.92f, 0.95f)

            // Defensive Barricades
            b.addBox(bx, baseY + 0.85f, bz, 4.2f, 1.7f, 1.1f, 0.54f, 0.46f, 0.34f)

            // Supply Container
            b.addBox(sx, baseY + 1.4f, sz, 4.8f, 2.8f, 2.6f, 0.16f, 0.42f, 0.52f)
        }

        colliders.add(WorldCollider(wx - 1.5f, wx + 1.5f, wz - 1.5f, wz + 1.5f, baseY + 8.8f))
        colliders.add(WorldCollider(bx - 2.1f, bx + 2.1f, bz - 0.55f, bz + 0.55f, baseY + 1.7f))
        colliders.add(WorldCollider(sx - 2.4f, sx + 2.4f, sz - 1.3f, sz + 1.3f, baseY + 2.8f))
        coverPoints.add(Vec3(bx, baseY + 0.3f, bz + 1.6f))
        coverPoints.add(Vec3(sx, baseY + 0.3f, sz + 2.2f))

        minimapLandmarks.add(
            MinimapLandmark(
                normX = (cx + worldHalfSize) / (worldHalfSize * 2f),
                normZ = (cz + worldHalfSize) / (worldHalfSize * 2f),
                normW = 13f / (worldHalfSize * 2f),
                normH = 13f / (worldHalfSize * 2f)
            )
        )
    }

    private fun addAbandonedWarehouse(b: MeshDataBuilder?, cx: Float, cz: Float) {
        val baseY = getRawTerrainHeight(cx, cz) - 0.3f
        if (b != null) {
            // Floor slab
            b.addBox(cx, baseY + 0.15f, cz, 8.6f, 0.3f, 7.0f, 0.42f, 0.41f, 0.40f)
            // Broken perimeter walls (with openings)
            b.addBox(cx - 4.0f, baseY + 1.7f, cz, 0.6f, 3.4f, 7.0f, 0.52f, 0.50f, 0.47f)
            b.addBox(cx + 4.0f, baseY + 1.4f, cz - 1.4f, 0.6f, 2.8f, 4.2f, 0.52f, 0.50f, 0.47f)
            b.addBox(cx, baseY + 1.5f, cz - 3.2f, 8.2f, 3.0f, 0.6f, 0.48f, 0.46f, 0.44f)
            // Partial collapsed roof beam
            b.addBox(cx - 1.4f, baseY + 3.5f, cz - 1.2f, 5.4f, 0.35f, 3.6f, 0.36f, 0.32f, 0.30f)
        }

        colliders.add(WorldCollider(cx - 4.3f, cx - 3.7f, cz - 3.5f, cz + 3.5f, baseY + 3.5f))
        colliders.add(WorldCollider(cx + 3.7f, cx + 4.3f, cz - 3.5f, cz + 0.7f, baseY + 3.0f))
        colliders.add(WorldCollider(cx - 4.1f, cx + 4.1f, cz - 3.5f, cz - 2.9f, baseY + 3.0f))
        coverPoints.add(Vec3(cx, baseY + 0.3f, cz))
        minimapLandmarks.add(
            MinimapLandmark(
                normX = (cx + worldHalfSize) / (worldHalfSize * 2f),
                normZ = (cz + worldHalfSize) / (worldHalfSize * 2f),
                normW = 8.6f / (worldHalfSize * 2f),
                normH = 7.0f / (worldHalfSize * 2f)
            )
        )
    }

    private fun addTree(b: MeshDataBuilder?, x: Float, y: Float, z: Float, scale: Float, isPine: Boolean) {
        val trunkH = 2.6f * scale
        if (b != null) {
            b.addCylinder(x, y - 0.2f, z, 0.32f * scale, 0.20f * scale, trunkH, 6, 0.40f, 0.27f, 0.18f)
            val f1 = palette.treeFoliage1
            val f2 = palette.treeFoliage2
            if (isPine) {
                b.addCylinder(x, y + trunkH * 0.65f, z, 1.8f * scale, 0.2f, 2.4f * scale, 7, f1[0], f1[1], f1[2])
                b.addCylinder(x, y + trunkH * 1.15f, z, 1.35f * scale, 0.05f, 2.2f * scale, 7, f2[0], f2[1], f2[2])
            } else {
                b.addSphere(x, y + trunkH + 0.8f * scale, z, 1.65f * scale, 5, 7, f1[0], f1[1], f1[2])
                b.addSphere(x + 0.55f * scale, y + trunkH + 0.5f * scale, z + 0.4f * scale, 1.1f * scale, 4, 6, f2[0], f2[1], f2[2])
            }
        }
        colliders.add(WorldCollider(x - 0.45f * scale, x + 0.45f * scale, z - 0.45f * scale, z + 0.45f * scale, y + trunkH + 2f, isTree = true))
        coverPoints.add(Vec3(x + 1.4f, y, z + 1.4f))
    }

    private fun addRock(b: MeshDataBuilder?, x: Float, y: Float, z: Float, scale: Float) {
        if (b != null) {
            b.addBox(x, y + 0.55f * scale, z, 2.0f * scale, 1.4f * scale, 1.7f * scale, 0.48f, 0.47f, 0.46f)
            b.addBox(x + 0.4f * scale, y + 0.9f * scale, z - 0.2f * scale, 1.2f * scale, 1.0f * scale, 1.1f * scale, 0.54f, 0.52f, 0.50f)
        }
        colliders.add(WorldCollider(x - 1.0f * scale, x + 1.0f * scale, z - 0.85f * scale, z + 0.85f * scale, y + 1.4f * scale))
        coverPoints.add(Vec3(x - 1.5f * scale, y, z))
    }
}
