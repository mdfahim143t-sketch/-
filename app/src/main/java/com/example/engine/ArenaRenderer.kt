package com.example.engine

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.model.ArcadeWeapon
import com.example.model.GameMapId
import com.example.model.GameMode
import com.example.model.GraphicsQuality
import com.example.model.OutfitConfig
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ArenaRenderer(
    private val simulation: GameSimulation,
    private val onHudTick: () -> Unit
) : GLSurfaceView.Renderer {

    private lateinit var worldShader: ShaderProgram
    private lateinit var skyShader: ShaderProgram
    private lateinit var waterShader: ShaderProgram
    private lateinit var charRenderer: CharacterRenderer

    private var skyDomeMesh: Mesh? = null
    private var terrainMesh: Mesh? = null
    private var waterMesh: Mesh? = null
    private var grassMesh: Mesh? = null
    private var crateMesh: Mesh? = null
    private var boltMesh: Mesh? = null
    private var pedestalMesh: Mesh? = null
    private var zoneWallMesh: Mesh? = null
    private var targetDroneMesh: Mesh? = null

    private var worldMap: WorldMap? = null

    @Volatile var isLobbyScene: Boolean = true
    @Volatile var isPaused: Boolean = false
    @Volatile var isVictoryScene: Boolean = false
    @Volatile var lobbyCharacterId: String = "kai"
    @Volatile var lobbyOutfit: OutfitConfig = OutfitConfig()
    @Volatile var lobbyWeapon: ArcadeWeapon = ArcadeWeapon.PULSE_RIFLE

    @Volatile private var desiredMapId: GameMapId = GameMapId.EMERALD_ISLE
    @Volatile private var desiredQuality: GraphicsQuality = GraphicsQuality.MEDIUM
    @Volatile private var rebuildWorldRequested: Boolean = true

    private val projMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)

    private var lastFrameNs = System.nanoTime()
    private var elapsedSec = 0f
    private var hudAccumulator = 0f
    private var screenAspect = 1.6f

    fun requestMapAndQuality(mapId: GameMapId, quality: GraphicsQuality) {
        if (desiredMapId != mapId || desiredQuality != quality) {
            desiredMapId = mapId
            desiredQuality = quality
            rebuildWorldRequested = true
        }
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        worldShader = ShaderProgram(GameShaders.WORLD_VERTEX, GameShaders.WORLD_FRAGMENT)
        skyShader = ShaderProgram(GameShaders.SKY_VERTEX, GameShaders.SKY_FRAGMENT)
        waterShader = ShaderProgram(GameShaders.WATER_VERTEX, GameShaders.WATER_FRAGMENT)
        charRenderer = CharacterRenderer()

        // Sky dome sphere
        skyDomeMesh = MeshDataBuilder().apply {
            addSphere(0f, 0f, 0f, 180f, 10, 16, 1f, 1f, 1f)
        }.build()

        // Loot crate / crystal
        crateMesh = MeshDataBuilder().apply {
            addBox(0f, 0.35f, 0f, 0.72f, 0.56f, 0.72f, 0.24f, 0.28f, 0.34f)
            addBox(0f, 0.65f, 0f, 0.78f, 0.10f, 0.78f, 0.95f, 0.65f, 0.15f)
        }.build()

        // Energy bolt
        boltMesh = MeshDataBuilder().apply {
            addBox(0f, 0f, 0f, 0.16f, 0.16f, 0.75f, 1f, 1f, 1f)
        }.build()

        // Lobby tactical pedestal
        pedestalMesh = MeshDataBuilder().apply {
            addCylinder(0f, -0.35f, 0f, 2.2f, 1.85f, 0.35f, 18, 0.14f, 0.20f, 0.28f)
            addCylinder(0f, -0.02f, 0f, 1.75f, 1.75f, 0.06f, 18, 0.18f, 0.75f, 0.92f)
        }.build()

        // Safe zone ring wall (unit radius cylinder scaled at draw time)
        zoneWallMesh = MeshDataBuilder().apply {
            val segs = 40
            for (i in 0 until segs) {
                val a0 = (i.toFloat() / segs) * 2f * PI.toFloat()
                val a1 = ((i + 1).toFloat() / segs) * 2f * PI.toFloat()
                val x0 = cos(a0); val z0 = sin(a0)
                val x1 = cos(a1); val z1 = sin(a1)
                addQuad(
                    x0, -4f, z0,
                    x1, -4f, z1,
                    x1, 36f, z1,
                    x0, 36f, z0,
                    0f, 1f, 0f,
                    0.15f, 0.78f, 0.96f, 0.24f
                )
            }
        }.build()

        // Practice Target Drone
        targetDroneMesh = MeshDataBuilder().apply {
            addBox(0f, 0.9f, 0f, 0.22f, 1.8f, 0.22f, 0.32f, 0.35f, 0.40f)
            addSphere(0f, 1.45f, 0f, 0.55f, 6, 10, 0.18f, 0.85f, 0.95f)
            addBox(0f, 1.45f, 0f, 1.35f, 0.18f, 0.18f, 1.0f, 0.65f, 0.12f)
        }.build()

        rebuildWorldRequested = true
        lastFrameNs = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        screenAspect = width.toFloat() / maxOf(1, height).toFloat()
        val fov = if (screenAspect < 1f) 66f else 56f
        Matrix.perspectiveM(projMatrix, 0, fov, screenAspect, 0.25f, 260f)
    }

    private fun ensureWorldBuilt() {
        if (!rebuildWorldRequested) return
        rebuildWorldRequested = false

        terrainMesh?.release()
        waterMesh?.release()
        grassMesh?.release()

        val map = WorldMap(desiredMapId)
        val (terrain, water, grass) = map.buildStaticGeometry(desiredQuality.grassDensity)
        worldMap = map
        terrainMesh = terrain
        waterMesh = water
        grassMesh = grass
        if (simulation.worldMap == null || simulation.worldMap?.mapId != desiredMapId) {
            simulation.worldMap = map
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        ensureWorldBuilt()
        val map = worldMap ?: return

        val now = System.nanoTime()
        val dt = ((now - lastFrameNs) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastFrameNs = now
        elapsedSec += dt

        if (isLobbyScene) {
            simulation.stepLobby(dt)
        } else if (!isPaused) {
            simulation.stepMatch(dt)
            hudAccumulator += dt
            if (hudAccumulator >= 0.05f) {
                hudAccumulator = 0f
                onHudTick()
            }
        }

        val pal = map.palette
        GLES20.glClearColor(pal.horizonSky[0], pal.horizonSky[1], pal.horizonSky[2], 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        val camX: Float
        val camY: Float
        val camZ: Float
        val lookX: Float
        val lookY: Float
        val lookZ: Float

        if (isLobbyScene) {
            val lobbyX = -10f
            val lobbyZ = -6f
            val lobbyY = map.getWalkableHeight(lobbyX, lobbyZ) + 0.35f
            val dist = if (screenAspect < 1f) 4.6f else 3.8f
            camX = lobbyX + sin(simulation.lobbyYawRad) * dist
            camZ = lobbyZ + cos(simulation.lobbyYawRad) * dist
            camY = lobbyY + 1.55f
            lookX = lobbyX
            lookY = lobbyY + 1.05f
            lookZ = lobbyZ
            Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        } else {
            // Third-person over-the-shoulder tactical camera
            val dist = 4.6f
            val shoulderRight = 0.62f
            val horizDist = dist * cos(simulation.camPitchRad)
            val vertDist = dist * sin(simulation.camPitchRad)
            val targetX = simulation.playerX + cos(simulation.camYawRad) * shoulderRight
            val targetZ = simulation.playerZ - sin(simulation.camYawRad) * shoulderRight
            val targetY = simulation.playerY + (if (simulation.isCrouching) 1.05f else 1.48f)

            var rawCamX = targetX - sin(simulation.camYawRad) * horizDist
            var rawCamZ = targetZ - cos(simulation.camYawRad) * horizDist
            var rawCamY = targetY + vertDist

            val minCamGround = map.getWalkableHeight(rawCamX, rawCamZ) + 0.65f
            if (rawCamY < minCamGround) rawCamY = minCamGround

            camX = rawCamX
            camY = rawCamY
            camZ = rawCamZ
            lookX = targetX + sin(simulation.camYawRad) * 6f
            lookY = targetY - sin(simulation.camPitchRad) * 2.5f
            lookZ = targetZ + cos(simulation.camYawRad) * 6f
            Matrix.setLookAtM(viewMatrix, 0, camX, camY, camZ, lookX, lookY, lookZ, 0f, 1f, 0f)
        }

        Matrix.multiplyMM(vpMatrix, 0, projMatrix, 0, viewMatrix, 0)

        // 1. Draw Animated Sky Dome centered on Camera
        GLES20.glDepthMask(false)
        skyShader.use()
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, camX, camY, camZ)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(skyShader.uniform("uMVPMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniform3fv(skyShader.uniform("uZenithColor"), 1, pal.zenithSky, 0)
        GLES20.glUniform3fv(skyShader.uniform("uHorizonColor"), 1, pal.horizonSky, 0)
        GLES20.glUniform3fv(skyShader.uniform("uSunDir"), 1, pal.sunDir, 0)
        GLES20.glUniform3fv(skyShader.uniform("uSunColor"), 1, pal.sunColor, 0)
        GLES20.glUniform1f(skyShader.uniform("uTime"), elapsedSec)
        skyDomeMesh?.draw(skyShader.attrib("aPosition"), -1, -1)
        GLES20.glDepthMask(true)

        // 2. Draw World Terrain, Buildings, Trees & Props
        worldShader.use()
        val uMvp = worldShader.uniform("uMVPMatrix")
        val uModel = worldShader.uniform("uModelMatrix")
        val uTime = worldShader.uniform("uTime")
        val uWind = worldShader.uniform("uWindFactor")
        val uTint = worldShader.uniform("uTint")
        val uUnlit = worldShader.uniform("uUnlit")
        val aPos = worldShader.attrib("aPosition")
        val aNorm = worldShader.attrib("aNormal")
        val aCol = worldShader.attrib("aColor")

        val drawDist = desiredQuality.drawDistance
        GLES20.glUniform3fv(worldShader.uniform("uSunDir"), 1, pal.sunDir, 0)
        GLES20.glUniform3fv(worldShader.uniform("uSunColor"), 1, pal.sunColor, 0)
        GLES20.glUniform3fv(worldShader.uniform("uAmbientSky"), 1, pal.ambientSky, 0)
        GLES20.glUniform3fv(worldShader.uniform("uAmbientGround"), 1, pal.ambientGround, 0)
        GLES20.glUniform3fv(worldShader.uniform("uFogColor"), 1, pal.fogColor, 0)
        GLES20.glUniform1f(worldShader.uniform("uFogStart"), drawDist * 0.48f)
        GLES20.glUniform1f(worldShader.uniform("uFogEnd"), drawDist)
        GLES20.glUniform3f(worldShader.uniform("uCameraPos"), camX, camY, camZ)
        GLES20.glUniform1f(uTime, elapsedSec)
        GLES20.glUniform1f(uWind, 0f)
        GLES20.glUniform1f(uUnlit, 0f)
        GLES20.glUniform4f(uTint, 1f, 1f, 1f, 1f)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
        terrainMesh?.draw(aPos, aNorm, aCol)

        // Draw swaying grass if Medium/High
        if (desiredQuality.grassDensity > 0) {
            GLES20.glUniform1f(uWind, 1.0f)
            grassMesh?.draw(aPos, aNorm, aCol)
            GLES20.glUniform1f(uWind, 0f)
        }

        val showShadows = desiredQuality.shadowsEnabled

        if (isLobbyScene) {
            val lobbyX = -10f
            val lobbyZ = -6f
            val lobbyY = map.getWalkableHeight(lobbyX, lobbyZ) + 0.35f

            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, lobbyX, lobbyY, lobbyZ)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            GLES20.glUniform4f(uTint, 1f, 1f, 1f, 1f)
            pedestalMesh?.draw(aPos, aNorm, aCol)

            charRenderer.drawCharacter(
                vpMatrix, aPos, aNorm, aCol, uMvp, uModel, uTint,
                x = lobbyX, y = lobbyY, z = lobbyZ,
                yawRad = 0.15f,
                animState = if (isVictoryScene) CharacterAnimState.VICTORY else CharacterAnimState.IDLE,
                animTime = elapsedSec,
                recoil = 0f,
                characterId = lobbyCharacterId,
                outfit = lobbyOutfit,
                weapon = lobbyWeapon,
                drawShadow = showShadows,
                isEnemy = false
            )
        } else {
            // Draw Loot Crates
            for (loot in map.lootSpawns) {
                if (loot.collected) continue
                if (Math3D.dist2D(camX, camZ, loot.x, loot.z) > drawDist) continue
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, loot.x, loot.y + sin(elapsedSec * 2.5f + loot.id) * 0.08f, loot.z)
                Matrix.rotateM(modelMatrix, 0, elapsedSec * 45f, 0f, 1f, 0f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
                val tint = when (loot.type) {
                    LootType.MED_CELL -> floatArrayOf(0.35f, 1f, 0.55f, 1f)
                    LootType.SHIELD_CORE -> floatArrayOf(0.35f, 0.85f, 1f, 1f)
                    LootType.AMMO_PACK -> floatArrayOf(1f, 0.9f, 0.45f, 1f)
                    else -> floatArrayOf(1f, 0.72f, 0.25f, 1f)
                }
                GLES20.glUniform4fv(uTint, 1, tint, 0)
                crateMesh?.draw(aPos, aNorm, aCol)
            }

            // Draw Practice Target Drones
            if (simulation.gameMode == GameMode.PRACTICE) {
                for (i in map.practiceTargets.indices) {
                    if (!simulation.practiceTargetAlive[i]) continue
                    val pt = map.practiceTargets[i]
                    Matrix.setIdentityM(modelMatrix, 0)
                    Matrix.translateM(modelMatrix, 0, pt.x, pt.y + sin(elapsedSec * 3f + i) * 0.14f, pt.z)
                    Matrix.rotateM(modelMatrix, 0, elapsedSec * 90f, 0f, 1f, 0f)
                    Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                    GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
                    GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
                    GLES20.glUniform4f(uTint, 1f, 1f, 1f, 1f)
                    targetDroneMesh?.draw(aPos, aNorm, aCol)
                }
            }

            // Draw AI Enemies
            for (e in simulation.enemies) {
                if (!e.alive) continue
                if (Math3D.dist2D(camX, camZ, e.x, e.z) > drawDist) continue
                charRenderer.drawCharacter(
                    vpMatrix, aPos, aNorm, aCol, uMvp, uModel, uTint,
                    x = e.x, y = e.y, z = e.z,
                    yawRad = e.yawRad,
                    animState = e.animState,
                    animTime = e.animTime,
                    recoil = e.recoil,
                    characterId = e.characterId,
                    outfit = e.outfit,
                    weapon = e.weapon,
                    drawShadow = showShadows,
                    isEnemy = true
                )
            }

            // Draw Player Character
            val activeWeapon = if (simulation.activeSlot == 1 && simulation.secondaryWeapon != null) {
                simulation.secondaryWeapon!!
            } else {
                simulation.primaryWeapon
            }
            charRenderer.drawCharacter(
                vpMatrix, aPos, aNorm, aCol, uMvp, uModel, uTint,
                x = simulation.playerX, y = simulation.playerY, z = simulation.playerZ,
                yawRad = simulation.playerYawRad,
                animState = simulation.playerAnimState,
                animTime = simulation.playerAnimTime,
                recoil = simulation.playerRecoil,
                characterId = simulation.playerCharacterId,
                outfit = simulation.playerOutfit,
                weapon = activeWeapon,
                drawShadow = showShadows,
                isEnemy = false
            )

            // Draw Unlit Energy Bolts & Particles
            GLES20.glUniform1f(uUnlit, 1f)
            for (b in simulation.bolts) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, b.x, b.y, b.z)
                val yaw = kotlin.math.atan2(b.vx, b.vz) * Math3D.RAD_TO_DEG
                Matrix.rotateM(modelMatrix, 0, yaw, 0f, 1f, 0f)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
                GLES20.glUniform4fv(uTint, 1, b.color, 0)
                boltMesh?.draw(aPos, aNorm, aCol)
            }

            for (p in simulation.particles) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, p.x, p.y, p.z)
                val s = p.size * (p.life / p.maxLife).coerceIn(0.2f, 1f)
                Matrix.scaleM(modelMatrix, 0, s, s, s)
                Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
                GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
                GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
                GLES20.glUniform4fv(uTint, 1, p.color, 0)
                boltMesh?.draw(aPos, aNorm, aCol)
            }
            GLES20.glUniform1f(uUnlit, 0f)
        }

        // 3. Draw Animated Translucent River Water
        waterShader.use()
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
        GLES20.glUniformMatrix4fv(waterShader.uniform("uMVPMatrix"), 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(waterShader.uniform("uTime"), elapsedSec)
        GLES20.glUniform3fv(waterShader.uniform("uFogColor"), 1, pal.fogColor, 0)
        GLES20.glUniform1f(waterShader.uniform("uFogStart"), drawDist * 0.48f)
        GLES20.glUniform1f(waterShader.uniform("uFogEnd"), drawDist)
        GLES20.glUniform3f(waterShader.uniform("uCameraPos"), camX, camY, camZ)
        GLES20.glUniform3fv(waterShader.uniform("uSunColor"), 1, pal.sunColor, 0)
        waterMesh?.draw(waterShader.attrib("aPosition"), waterShader.attrib("aNormal"), waterShader.attrib("aColor"))

        // 4. Draw Safe Zone Energy Barrier in Survival / Arena Modes
        if (!isLobbyScene && simulation.gameMode != GameMode.PRACTICE) {
            GLES20.glDepthMask(false)
            worldShader.use()
            GLES20.glUniform1f(uUnlit, 1f)
            val pulse = 0.78f + 0.22f * sin(elapsedSec * 3.5f)
            GLES20.glUniform4f(uTint, 0.2f, 0.85f, 1.0f, pulse)
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, simulation.safeZoneCenterX, 0f, simulation.safeZoneCenterZ)
            Matrix.scaleM(modelMatrix, 0, simulation.safeZoneRadius, 1f, simulation.safeZoneRadius)
            Matrix.multiplyMM(mvpMatrix, 0, vpMatrix, 0, modelMatrix, 0)
            GLES20.glUniformMatrix4fv(uMvp, 1, false, mvpMatrix, 0)
            GLES20.glUniformMatrix4fv(uModel, 1, false, modelMatrix, 0)
            zoneWallMesh?.draw(aPos, aNorm, aCol)
            GLES20.glDepthMask(true)
        }
    }
}
