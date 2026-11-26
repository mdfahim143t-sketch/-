package com.example.engine

import android.opengl.GLES20
import android.opengl.Matrix
import com.example.model.ArcadeWeapon
import com.example.model.CharacterCatalog
import com.example.model.OutfitConfig
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class CharacterAnimState {
    IDLE,
    WALK,
    RUN,
    JUMP,
    CROUCH,
    VICTORY
}

class CharacterRenderer {
    private val unitCube = MeshDataBuilder().apply {
        addBox(0f, 0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f)
    }.build()

    private val unitSphere = MeshDataBuilder().apply {
        addSphere(0f, 0f, 0f, 0.5f, 6, 8, 1f, 1f, 1f)
    }.build()

    private val shadowDisc = MeshDataBuilder().apply {
        addShadowDisc(0.75f, 16)
    }.build()

    private val modelMat = FloatArray(16)
    private val partMat = FloatArray(16)
    private val mvpMat = FloatArray(16)

    fun release() {
        unitCube.release()
        unitSphere.release()
        shadowDisc.release()
    }

    fun drawCharacter(
        vpMatrix: FloatArray,
        posHandle: Int,
        normHandle: Int,
        colHandle: Int,
        mvpHandle: Int,
        modelHandle: Int,
        tintHandle: Int,
        x: Float,
        y: Float,
        z: Float,
        yawRad: Float,
        animState: CharacterAnimState,
        animTime: Float,
        recoil: Float,
        characterId: String,
        outfit: OutfitConfig,
        weapon: ArcadeWeapon,
        drawShadow: Boolean,
        isEnemy: Boolean = false
    ) {
        val charInfo = CharacterCatalog.byId(characterId)
        val skinColor = Math3D.colorToFloats(charInfo.skinToneHex)

        val jacketColor = when (outfit.jacketStyle) {
            0 -> floatArrayOf(0.18f, 0.25f, 0.34f, 1f) // Vanguard Slate
            1 -> floatArrayOf(0.68f, 0.56f, 0.38f, 1f) // Nomad Sand
            2 -> floatArrayOf(0.14f, 0.38f, 0.72f, 1f) // Cobalt Striker
            else -> floatArrayOf(0.68f, 0.16f, 0.22f, 1f) // Crimson Apex
        }
        val pantsColor = if (isEnemy) {
            floatArrayOf(0.24f, 0.20f, 0.22f, 1f)
        } else {
            floatArrayOf(0.16f, 0.19f, 0.24f, 1f)
        }
        val bootsColor = when (outfit.bootsStyle) {
            0 -> floatArrayOf(0.28f, 0.20f, 0.14f, 1f)
            1 -> floatArrayOf(0.22f, 0.26f, 0.32f, 1f)
            else -> floatArrayOf(0.10f, 0.12f, 0.15f, 1f)
        }
        val glovesColor = when (outfit.glovesStyle) {
            0 -> floatArrayOf(0.24f, 0.22f, 0.20f, 1f)
            1 -> floatArrayOf(0.85f, 0.55f, 0.15f, 1f)
            else -> floatArrayOf(0.12f, 0.14f, 0.18f, 1f)
        }
        val accentColor = if (isEnemy) {
            floatArrayOf(0.95f, 0.26f, 0.24f, 1f)
        } else {
            Math3D.colorToFloats(charInfo.accentHex)
        }
        val hairColor = when (characterId) {
            "nova" -> floatArrayOf(0.14f, 0.18f, 0.28f, 1f)
            "rex" -> floatArrayOf(0.14f, 0.11f, 0.10f, 1f)
            "mira" -> floatArrayOf(0.46f, 0.24f, 0.56f, 1f)
            else -> floatArrayOf(0.22f, 0.16f, 0.12f, 1f)
        }

        // Ground shadow disc
        if (drawShadow) {
            Matrix.setIdentityM(modelMat, 0)
            Matrix.translateM(modelMat, 0, x, y + 0.02f, z)
            drawPart(shadowDisc, vpMatrix, modelMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(1f, 1f, 1f, 1f))
        }

        // Compute procedural joint angles
        var bobY = 0f
        var torsoPitch = 0f
        var legSwing = 0f
        var armSwing = 0f
        var crouchDrop = 0f
        var victoryArmLift = 0f
        var extraYaw = 0f

        when (animState) {
            CharacterAnimState.IDLE -> {
                bobY = sin(animTime * 2.2f) * 0.025f
                armSwing = sin(animTime * 2.2f) * 3f
            }
            CharacterAnimState.WALK -> {
                bobY = abs(sin(animTime * 7.5f)) * 0.06f
                legSwing = sin(animTime * 7.5f) * 32f
                armSwing = -legSwing * 0.65f
                torsoPitch = 6f
            }
            CharacterAnimState.RUN -> {
                bobY = abs(sin(animTime * 11.5f)) * 0.10f
                legSwing = sin(animTime * 11.5f) * 52f
                armSwing = -legSwing * 0.8f
                torsoPitch = 15f
            }
            CharacterAnimState.JUMP -> {
                bobY = 0.06f
                legSwing = 26f
                armSwing = -35f
                torsoPitch = -6f
            }
            CharacterAnimState.CROUCH -> {
                crouchDrop = -0.34f
                torsoPitch = 20f
                legSwing = sin(animTime * 6.0f) * 14f
            }
            CharacterAnimState.VICTORY -> {
                bobY = abs(sin(animTime * 4.5f)) * 0.12f
                victoryArmLift = -145f + sin(animTime * 6f) * 14f
                extraYaw = sin(animTime * 1.5f) * 18f
            }
        }

        val yawDeg = yawRad * Math3D.RAD_TO_DEG + extraYaw
        val hipHeight = 0.86f + crouchDrop + bobY

        // Helper for local character space -> world
        fun setupPart(lx: Float, ly: Float, lz: Float, pitchDeg: Float, rollDeg: Float, sx: Float, sy: Float, sz: Float, extraPartYaw: Float = 0f) {
            Matrix.setIdentityM(partMat, 0)
            Matrix.translateM(partMat, 0, x, y, z)
            Matrix.rotateM(partMat, 0, yawDeg, 0f, 1f, 0f)
            Matrix.translateM(partMat, 0, lx, ly, lz)
            if (extraPartYaw != 0f) Matrix.rotateM(partMat, 0, extraPartYaw, 0f, 1f, 0f)
            if (pitchDeg != 0f) Matrix.rotateM(partMat, 0, pitchDeg, 1f, 0f, 0f)
            if (rollDeg != 0f) Matrix.rotateM(partMat, 0, rollDeg, 0f, 0f, 1f)
            Matrix.scaleM(partMat, 0, sx, sy, sz)
        }

        // Legs (Left & Right) + Boots
        val kneeBend = if (animState == CharacterAnimState.CROUCH) -32f else 0f
        for (side in listOf(-1f, 1f)) {
            val swing = if (side < 0f) legSwing else -legSwing
            setupPart(side * 0.14f, hipHeight - 0.24f, 0f, swing + kneeBend, 0f, 0.20f, 0.44f, 0.21f)
            drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, pantsColor)

            // Boot lower leg
            val rad = swing * Math3D.DEG_TO_RAD
            val bootOffsetZ = sin(rad) * 0.34f
            val bootOffsetY = hipHeight - 0.62f + (1f - cos(rad)) * 0.12f
            setupPart(side * 0.14f, maxOf(0.16f, bootOffsetY), bootOffsetZ, swing * 0.5f, 0f, 0.22f, 0.36f, 0.26f)
            drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, bootsColor)
        }

        // Torso / Jacket
        val torsoY = hipHeight + 0.26f
        setupPart(0f, torsoY, 0f, torsoPitch, 0f, 0.50f, 0.58f, 0.28f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, jacketColor)

        // Tactical Chest Trim
        setupPart(0f, torsoY + 0.06f, 0.15f, torsoPitch, 0f, 0.38f, 0.22f, 0.05f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)

        // Backpack
        when (outfit.backpackStyle) {
            0 -> {
                setupPart(0f, torsoY + 0.04f, -0.20f, torsoPitch, 0f, 0.30f, 0.36f, 0.14f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.24f, 0.26f, 0.30f, 1f))
            }
            1 -> {
                setupPart(0f, torsoY + 0.05f, -0.23f, torsoPitch, 0f, 0.36f, 0.44f, 0.22f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.32f, 0.28f, 0.22f, 1f))
                setupPart(0f, torsoY + 0.22f, -0.24f, torsoPitch, 0f, 0.32f, 0.12f, 0.24f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)
            }
            else -> {
                setupPart(-0.12f, torsoY + 0.06f, -0.22f, torsoPitch, 0f, 0.16f, 0.44f, 0.18f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.18f, 0.22f, 0.28f, 1f))
                setupPart(0.12f, torsoY + 0.06f, -0.22f, torsoPitch, 0f, 0.16f, 0.44f, 0.18f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.18f, 0.22f, 0.28f, 1f))
                setupPart(0f, torsoY + 0.10f, -0.24f, torsoPitch, 0f, 0.18f, 0.18f, 0.10f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)
            }
        }

        // Head
        val headY = torsoY + 0.48f
        setupPart(0f, headY, 0.03f, torsoPitch * 0.4f, 0f, 0.34f, 0.35f, 0.34f)
        drawPart(unitSphere, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, skinColor)

        // Mask / Visor
        when (outfit.maskStyle) {
            1 -> { // Glowing Visor
                setupPart(0f, headY + 0.03f, 0.16f, torsoPitch * 0.4f, 0f, 0.30f, 0.10f, 0.10f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)
            }
            2 -> { // Tactical Half Respirator
                setupPart(0f, headY - 0.08f, 0.14f, torsoPitch * 0.4f, 0f, 0.28f, 0.15f, 0.14f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.16f, 0.18f, 0.22f, 1f))
            }
            3 -> { // Recon Helmet
                setupPart(0f, headY + 0.04f, 0.01f, torsoPitch * 0.4f, 0f, 0.39f, 0.36f, 0.39f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.18f, 0.22f, 0.26f, 1f))
                setupPart(0f, headY + 0.04f, 0.19f, torsoPitch * 0.4f, 0f, 0.31f, 0.11f, 0.06f)
                drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)
            }
        }

        // Hair (shown when not wearing full Recon Helmet)
        if (outfit.maskStyle != 3) {
            when (outfit.hairStyle) {
                0 -> { // Tactical Crop
                    setupPart(0f, headY + 0.14f, -0.01f, torsoPitch * 0.4f, 0f, 0.35f, 0.13f, 0.35f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, hairColor)
                }
                1 -> { // Swept Crest
                    setupPart(0f, headY + 0.16f, -0.02f, -10f, 0f, 0.34f, 0.16f, 0.38f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, hairColor)
                }
                2 -> { // Mohawk Ridge
                    setupPart(0f, headY + 0.20f, 0.0f, 0f, 0f, 0.12f, 0.20f, 0.38f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, accentColor)
                }
                else -> { // Twin Tail
                    setupPart(0f, headY + 0.13f, -0.02f, 0f, 0f, 0.34f, 0.13f, 0.34f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, hairColor)
                    setupPart(-0.18f, headY - 0.02f, -0.14f, 15f, -10f, 0.10f, 0.28f, 0.10f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, hairColor)
                    setupPart(0.18f, headY - 0.02f, -0.14f, 15f, 10f, 0.10f, 0.28f, 0.10f)
                    drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, hairColor)
                }
            }
        }

        // Left Arm + Glove
        val leftPitch = if (animState == CharacterAnimState.VICTORY) victoryArmLift else armSwing
        setupPart(-0.33f, torsoY + 0.04f, 0.04f, leftPitch, 8f, 0.15f, 0.44f, 0.15f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, jacketColor)
        setupPart(-0.34f, torsoY - 0.17f, 0.06f, leftPitch, 8f, 0.16f, 0.14f, 0.16f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, glovesColor)

        // Right Arm (Holding Blaster) + Weapon
        val rightPitch = when {
            animState == CharacterAnimState.VICTORY -> victoryArmLift * 0.9f
            recoil > 0.01f -> -78f - recoil * 18f
            else -> -62f + armSwing * 0.25f
        }
        setupPart(0.33f, torsoY + 0.06f, 0.12f, rightPitch, -6f, 0.15f, 0.42f, 0.15f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, jacketColor)
        setupPart(0.33f, torsoY + 0.02f, 0.26f, rightPitch, -6f, 0.16f, 0.14f, 0.16f)
        drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, glovesColor)

        // Stylized Fictional Arcade Blaster in Right Hand
        if (animState != CharacterAnimState.VICTORY) {
            val wCol = Math3D.colorToFloats(weapon.colorHex)
            val wLen = when (weapon) {
                ArcadeWeapon.PLASMA_LANCE -> 0.85f
                ArcadeWeapon.ARC_SCATTER -> 0.56f
                ArcadeWeapon.VOLT_CARBINE -> 0.48f
                ArcadeWeapon.PULSE_RIFLE -> 0.66f
            }
            val kickZ = -recoil * 0.08f
            setupPart(0.32f, torsoY + 0.08f, 0.46f + kickZ, 0f, 0f, 0.10f, 0.14f, wLen)
            drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, floatArrayOf(0.15f, 0.17f, 0.20f, 1f))
            // Glowing Energy Core Strip
            setupPart(0.32f, torsoY + 0.13f, 0.52f + kickZ, 0f, 0f, 0.06f, 0.06f, wLen * 0.55f)
            drawPart(unitCube, vpMatrix, partMat, posHandle, normHandle, colHandle, mvpHandle, modelHandle, tintHandle, wCol)
        }
    }

    private fun drawPart(
        mesh: Mesh,
        vpMatrix: FloatArray,
        model: FloatArray,
        posHandle: Int,
        normHandle: Int,
        colHandle: Int,
        mvpHandle: Int,
        modelHandle: Int,
        tintHandle: Int,
        tint: FloatArray
    ) {
        Matrix.multiplyMM(mvpMat, 0, vpMatrix, 0, model, 0)
        GLES20.glUniformMatrix4fv(mvpHandle, 1, false, mvpMat, 0)
        GLES20.glUniformMatrix4fv(modelHandle, 1, false, model, 0)
        GLES20.glUniform4fv(tintHandle, 1, tint, 0)
        mesh.draw(posHandle, normHandle, colHandle)
    }
}
