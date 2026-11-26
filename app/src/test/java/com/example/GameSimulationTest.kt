package com.example

import com.example.engine.GameSimulation
import com.example.engine.WorldMap
import com.example.model.GameMapId
import com.example.model.GameMode
import com.example.model.GameSettings
import com.example.model.OutfitConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSimulationTest {

    @Test
    fun soloSurvivalInitializesEnemiesLandmarksAndMovement() {
        val sim = GameSimulation(soundEngine = null)
        val map = WorldMap(GameMapId.EMERALD_ISLE)
        map.generateLayoutOnly()

        assertTrue(map.colliders.isNotEmpty())
        assertTrue(map.lootSpawns.isNotEmpty())
        assertTrue(map.minimapLandmarks.isNotEmpty())

        sim.startNewMatch(map, GameMode.SOLO_SURVIVAL, "kai", OutfitConfig(), GameSettings())
        assertEquals(9, sim.enemies.size)
        assertFalse(sim.matchFinished)

        val startX = sim.playerX
        val startZ = sim.playerZ
        sim.joyY = -1.0f
        repeat(20) { sim.stepMatch(0.03f) }
        val movedDist = kotlin.math.hypot((sim.playerX - startX).toDouble(), (sim.playerZ - startZ).toDouble())
        assertTrue("Player should move forward on joystick input", movedDist > 1.0)
    }

    @Test
    fun practiceModeSpawnsZeroEnemiesAndCompletesCleanly() {
        val sim = GameSimulation(soundEngine = null)
        val map = WorldMap(GameMapId.TWILIGHT_PINES)
        map.generateLayoutOnly()

        sim.startNewMatch(map, GameMode.PRACTICE, "nova", OutfitConfig(), GameSettings())
        assertEquals(0, sim.enemies.size)

        sim.finishPracticeMatch()
        val hud = sim.buildHudSnapshot()
        assertTrue(hud.matchFinished)
        assertTrue(hud.isVictory)
    }

    @Test
    fun soloSurvivalVictoryTriggersWhenAllEnemiesEliminated() {
        val sim = GameSimulation(soundEngine = null)
        val map = WorldMap(GameMapId.CANYON_OUTPOST)
        map.generateLayoutOnly()

        sim.startNewMatch(map, GameMode.SOLO_SURVIVAL, "rex", OutfitConfig(), GameSettings())
        sim.enemies.forEach { it.alive = false }
        sim.stepMatch(0.02f)

        val hud = sim.buildHudSnapshot()
        assertTrue(hud.matchFinished)
        assertTrue(hud.isVictory)
        assertEquals(1, hud.placementRank)
    }
}
