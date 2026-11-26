package com.example.engine

import android.content.Context
import android.opengl.GLSurfaceView
import com.example.model.GraphicsQuality

class ArenaGLSurfaceView(
    context: Context,
    val renderer: ArenaRenderer
) : GLSurfaceView(context) {

    private var baseWidth = 0
    private var baseHeight = 0
    private var appliedScale = 1.0f

    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
        preserveEGLContextOnPause = true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            baseWidth = w
            baseHeight = h
            applyResolutionScale(appliedScale)
        }
    }

    fun applyQuality(quality: GraphicsQuality) {
        appliedScale = quality.resolutionScale
        applyResolutionScale(appliedScale)
    }

    private fun applyResolutionScale(scale: Float) {
        if (baseWidth <= 0 || baseHeight <= 0) return
        val sw = (baseWidth * scale).toInt().coerceAtLeast(320)
        val sh = (baseHeight * scale).toInt().coerceAtLeast(320)
        holder.setFixedSize(sw, sh)
    }
}
