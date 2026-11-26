package com.example.engine

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class Mesh(vertices: FloatArray) {
    val vertexCount: Int = vertices.size / STRIDE_FLOATS
    private var vboId: Int = 0
    private val localBuffer: FloatBuffer?

    init {
        val bb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder())
        val fb = bb.asFloatBuffer().apply {
            put(vertices)
            position(0)
        }
        val buffers = IntArray(1)
        GLES20.glGenBuffers(1, buffers, 0)
        if (buffers[0] != 0) {
            vboId = buffers[0]
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertices.size * 4, fb, GLES20.GL_STATIC_DRAW)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            localBuffer = null
        } else {
            localBuffer = fb
        }
    }

    fun draw(posHandle: Int, normHandle: Int, colHandle: Int) {
        if (vertexCount == 0) return
        if (vboId != 0) {
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId)
            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 0)
            if (normHandle >= 0) {
                GLES20.glEnableVertexAttribArray(normHandle)
                GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, 3 * 4)
            }
            if (colHandle >= 0) {
                GLES20.glEnableVertexAttribArray(colHandle)
                GLES20.glVertexAttribPointer(colHandle, 4, GLES20.GL_FLOAT, false, STRIDE_BYTES, 6 * 4)
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(posHandle)
            if (normHandle >= 0) GLES20.glDisableVertexAttribArray(normHandle)
            if (colHandle >= 0) GLES20.glDisableVertexAttribArray(colHandle)
            GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        } else if (localBuffer != null) {
            localBuffer.position(0)
            GLES20.glEnableVertexAttribArray(posHandle)
            GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, localBuffer)
            if (normHandle >= 0) {
                localBuffer.position(3)
                GLES20.glEnableVertexAttribArray(normHandle)
                GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, STRIDE_BYTES, localBuffer)
            }
            if (colHandle >= 0) {
                localBuffer.position(6)
                GLES20.glEnableVertexAttribArray(colHandle)
                GLES20.glVertexAttribPointer(colHandle, 4, GLES20.GL_FLOAT, false, STRIDE_BYTES, localBuffer)
            }
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
            GLES20.glDisableVertexAttribArray(posHandle)
            if (normHandle >= 0) GLES20.glDisableVertexAttribArray(normHandle)
            if (colHandle >= 0) GLES20.glDisableVertexAttribArray(colHandle)
        }
    }

    fun release() {
        if (vboId != 0) {
            GLES20.glDeleteBuffers(1, intArrayOf(vboId), 0)
            vboId = 0
        }
    }

    companion object {
        const val STRIDE_FLOATS = 10 // x,y,z, nx,ny,nz, r,g,b,a
        const val STRIDE_BYTES = STRIDE_FLOATS * 4
    }
}

class MeshDataBuilder {
    private val data = ArrayList<Float>(4096)

    fun addVertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addTri(
        x1: Float, y1: Float, z1: Float, nx1: Float, ny1: Float, nz1: Float, r1: Float, g1: Float, b1: Float, a1: Float,
        x2: Float, y2: Float, z2: Float, nx2: Float, ny2: Float, nz2: Float, r2: Float, g2: Float, b2: Float, a2: Float,
        x3: Float, y3: Float, z3: Float, nx3: Float, ny3: Float, nz3: Float, r3: Float, g3: Float, b3: Float, a3: Float
    ) {
        addVertex(x1, y1, z1, nx1, ny1, nz1, r1, g1, b1, a1)
        addVertex(x2, y2, z2, nx2, ny2, nz2, r2, g2, b2, a2)
        addVertex(x3, y3, z3, nx3, ny3, nz3, r3, g3, b3, a3)
    }

    fun addBox(cx: Float, cy: Float, cz: Float, sx: Float, sy: Float, sz: Float, r: Float, g: Float, b: Float, a: Float = 1f) {
        val hx = sx * 0.5f
        val hy = sy * 0.5f
        val hz = sz * 0.5f
        val x0 = cx - hx; val x1 = cx + hx
        val y0 = cy - hy; val y1 = cy + hy
        val z0 = cz - hz; val z1 = cz + hz

        // Front (+Z)
        addQuad(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f, r, g, b, a)
        // Back (-Z)
        addQuad(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f, r * 0.92f, g * 0.92f, b * 0.92f, a)
        // Left (-X)
        addQuad(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f, r * 0.95f, g * 0.95f, b * 0.95f, a)
        // Right (+X)
        addQuad(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f, r * 0.95f, g * 0.95f, b * 0.95f, a)
        // Top (+Y)
        addQuad(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f, minOf(1f, r * 1.06f), minOf(1f, g * 1.06f), minOf(1f, b * 1.06f), a)
        // Bottom (-Y)
        addQuad(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f, r * 0.8f, g * 0.8f, b * 0.8f, a)
    }

    fun addQuad(
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        x2: Float, y2: Float, z2: Float,
        x3: Float, y3: Float, z3: Float,
        nx: Float, ny: Float, nz: Float,
        r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        addTri(
            x0, y0, z0, nx, ny, nz, r, g, b, a,
            x1, y1, z1, nx, ny, nz, r, g, b, a,
            x2, y2, z2, nx, ny, nz, r, g, b, a
        )
        addTri(
            x0, y0, z0, nx, ny, nz, r, g, b, a,
            x2, y2, z2, nx, ny, nz, r, g, b, a,
            x3, y3, z3, nx, ny, nz, r, g, b, a
        )
    }

    fun addCylinder(
        cx: Float, bottomY: Float, cz: Float,
        radiusBottom: Float, radiusTop: Float, height: Float,
        segments: Int, r: Float, g: Float, b: Float, a: Float = 1f
    ) {
        val topY = bottomY + height
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            val c0 = cos(a0); val s0 = sin(a0)
            val c1 = cos(a1); val s1 = sin(a1)

            val x0b = cx + c0 * radiusBottom; val z0b = cz + s0 * radiusBottom
            val x1b = cx + c1 * radiusBottom; val z1b = cz + s1 * radiusBottom
            val x0t = cx + c0 * radiusTop; val z0t = cz + s0 * radiusTop
            val x1t = cx + c1 * radiusTop; val z1t = cz + s1 * radiusTop

            addTri(
                x0b, bottomY, z0b, c0, 0.25f, s0, r, g, b, a,
                x1b, bottomY, z1b, c1, 0.25f, s1, r, g, b, a,
                x1t, topY, z1t, c1, 0.25f, s1, r, g, b, a
            )
            addTri(
                x0b, bottomY, z0b, c0, 0.25f, s0, r, g, b, a,
                x1t, topY, z1t, c1, 0.25f, s1, r, g, b, a,
                x0t, topY, z0t, c0, 0.25f, s0, r, g, b, a
            )
            if (radiusTop > 0.01f) {
                addTri(
                    cx, topY, cz, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, a,
                    x0t, topY, z0t, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, a,
                    x1t, topY, z1t, 0f, 1f, 0f, r * 1.05f, g * 1.05f, b * 1.05f, a
                )
            }
        }
    }

    fun addSphere(cx: Float, cy: Float, cz: Float, radius: Float, rings: Int, sectors: Int, r: Float, g: Float, b: Float, a: Float = 1f) {
        for (i in 0 until rings) {
            val phi0 = PI.toFloat() * i / rings
            val phi1 = PI.toFloat() * (i + 1) / rings
            for (j in 0 until sectors) {
                val theta0 = 2f * PI.toFloat() * j / sectors
                val theta1 = 2f * PI.toFloat() * (j + 1) / sectors

                val p00x = sin(phi0) * cos(theta0); val p00y = cos(phi0); val p00z = sin(phi0) * sin(theta0)
                val p10x = sin(phi1) * cos(theta0); val p10y = cos(phi1); val p10z = sin(phi1) * sin(theta0)
                val p11x = sin(phi1) * cos(theta1); val p11y = cos(phi1); val p11z = sin(phi1) * sin(theta1)
                val p01x = sin(phi0) * cos(theta1); val p01y = cos(phi0); val p01z = sin(phi0) * sin(theta1)

                addTri(
                    cx + p00x * radius, cy + p00y * radius, cz + p00z * radius, p00x, p00y, p00z, r, g, b, a,
                    cx + p10x * radius, cy + p10y * radius, cz + p10z * radius, p10x, p10y, p10z, r, g, b, a,
                    cx + p11x * radius, cy + p11y * radius, cz + p11z * radius, p11x, p11y, p11z, r, g, b, a
                )
                addTri(
                    cx + p00x * radius, cy + p00y * radius, cz + p00z * radius, p00x, p00y, p00z, r, g, b, a,
                    cx + p11x * radius, cy + p11y * radius, cz + p11z * radius, p11x, p11y, p11z, r, g, b, a,
                    cx + p01x * radius, cy + p01y * radius, cz + p01z * radius, p01x, p01y, p01z, r, g, b, a
                )
            }
        }
    }

    fun addShadowDisc(radius: Float, segments: Int = 14) {
        for (i in 0 until segments) {
            val a0 = (i.toFloat() / segments) * 2f * PI.toFloat()
            val a1 = ((i + 1).toFloat() / segments) * 2f * PI.toFloat()
            addTri(
                0f, 0.03f, 0f, 0f, 1f, 0f, 0.02f, 0.03f, 0.05f, 0.42f,
                cos(a0) * radius, 0.03f, sin(a0) * radius, 0f, 1f, 0f, 0.02f, 0.03f, 0.05f, 0.0f,
                cos(a1) * radius, 0.03f, sin(a1) * radius, 0f, 1f, 0f, 0.02f, 0.03f, 0.05f, 0.0f
            )
        }
    }

    fun build(): Mesh {
        val arr = FloatArray(data.size)
        for (i in data.indices) arr[i] = data[i]
        return Mesh(arr)
    }
}
