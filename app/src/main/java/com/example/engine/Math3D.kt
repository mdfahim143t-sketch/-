package com.example.engine

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float): Vec3 {
        x = nx; y = ny; z = nz
        return this
    }

    fun copyFrom(other: Vec3): Vec3 {
        x = other.x; y = other.y; z = other.z
        return this
    }

    fun lengthSq(): Float = x * x + y * y + z * z
    fun length(): Float = sqrt(lengthSq())

    fun normalized(): Vec3 {
        val len = length()
        return if (len > 1e-5f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 1f)
    }

    operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
}

object Math3D {
    const val DEG_TO_RAD = (PI / 180.0).toFloat()
    const val RAD_TO_DEG = (180.0 / PI).toFloat()

    fun clamp(v: Float, minV: Float, maxV: Float): Float =
        if (v < minV) minV else if (v > maxV) maxV else v

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun dist2D(x1: Float, z1: Float, x2: Float, z2: Float): Float {
        val dx = x1 - x2
        val dz = z1 - z2
        return sqrt(dx * dx + dz * dz)
    }

    fun angleDiffRad(from: Float, to: Float): Float {
        var diff = (to - from) % (2f * PI.toFloat())
        if (diff > PI) diff -= 2f * PI.toFloat()
        if (diff < -PI) diff += 2f * PI.toFloat()
        return diff
    }

    private fun hash2D(ix: Int, iz: Int, seed: Int): Float {
        var n = ix * 374761393 + iz * 668265263 + seed * 1442695041
        n = (n xor (n shr 13)) * 1274126177
        n = n xor (n shr 16)
        return (n and 0x7FFFFFFF).toFloat() / 2147483647f
    }

    fun valueNoise2D(x: Float, z: Float, seed: Int): Float {
        val fx = floor(x).toInt()
        val fz = floor(z).toInt()
        val ux = x - fx
        val uz = z - fz
        val sx = ux * ux * (3f - 2f * ux)
        val sz = uz * uz * (3f - 2f * uz)

        val n00 = hash2D(fx, fz, seed)
        val n10 = hash2D(fx + 1, fz, seed)
        val n01 = hash2D(fx, fz + 1, seed)
        val n11 = hash2D(fx + 1, fz + 1, seed)

        val nx0 = lerp(n00, n10, sx)
        val nx1 = lerp(n01, n11, sx)
        return lerp(nx0, nx1, sz)
    }

    fun fbm2D(x: Float, z: Float, seed: Int, octaves: Int = 3): Float {
        var sum = 0f
        var amp = 0.5f
        var freq = 1f
        var norm = 0f
        for (i in 0 until octaves) {
            sum += valueNoise2D(x * freq, z * freq, seed + i * 31) * amp
            norm += amp
            amp *= 0.5f
            freq *= 2.05f
        }
        return if (norm > 0f) sum / norm else 0f
    }

    fun rayIntersectsSphere(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        cx: Float, cy: Float, cz: Float,
        radius: Float,
        maxDist: Float
    ): Float? {
        val fx = ox - cx
        val fy = oy - cy
        val fz = oz - cz
        val b = fx * dx + fy * dy + fz * dz
        val c = fx * fx + fy * fy + fz * fz - radius * radius
        val disc = b * b - c
        if (disc < 0f) return null
        val sqrtDisc = sqrt(disc)
        val t1 = -b - sqrtDisc
        val t2 = -b + sqrtDisc
        val hitT = when {
            t1 in 0.1f..maxDist -> t1
            t2 in 0.1f..maxDist -> t2
            else -> null
        }
        return hitT
    }

    fun rayIntersectsAABB(
        ox: Float, oy: Float, oz: Float,
        dx: Float, dy: Float, dz: Float,
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        maxDist: Float
    ): Float? {
        var tMin = 0.05f
        var tMax = maxDist

        if (kotlin.math.abs(dx) < 1e-5f) {
            if (ox < minX || ox > maxX) return null
        } else {
            val invD = 1f / dx
            var t1 = (minX - ox) * invD
            var t2 = (maxX - ox) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = maxOf(tMin, t1)
            tMax = minOf(tMax, t2)
            if (tMin > tMax) return null
        }

        if (kotlin.math.abs(dy) < 1e-5f) {
            if (oy < minY || oy > maxY) return null
        } else {
            val invD = 1f / dy
            var t1 = (minY - oy) * invD
            var t2 = (maxY - oy) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = maxOf(tMin, t1)
            tMax = minOf(tMax, t2)
            if (tMin > tMax) return null
        }

        if (kotlin.math.abs(dz) < 1e-5f) {
            if (oz < minZ || oz > maxZ) return null
        } else {
            val invD = 1f / dz
            var t1 = (minZ - oz) * invD
            var t2 = (maxZ - oz) * invD
            if (t1 > t2) { val tmp = t1; t1 = t2; t2 = tmp }
            tMin = maxOf(tMin, t1)
            tMax = minOf(tMax, t2)
            if (tMin > tMax) return null
        }

        return tMin
    }

    fun colorToFloats(argb: Long): FloatArray {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return floatArrayOf(r, g, b, a)
    }
}
