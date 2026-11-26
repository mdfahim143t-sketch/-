package com.example.engine

import android.opengl.GLES20

class ShaderProgram(vertexCode: String, fragmentCode: String) {
    val programId: Int

    init {
        val vShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexCode)
        val fShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode)
        programId = GLES20.glCreateProgram().also { prog ->
            GLES20.glAttachShader(prog, vShader)
            GLES20.glAttachShader(prog, fShader)
            GLES20.glLinkProgram(prog)
        }
        GLES20.glDeleteShader(vShader)
        GLES20.glDeleteShader(fShader)
    }

    private fun compileShader(type: Int, code: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, code)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun use() {
        GLES20.glUseProgram(programId)
    }

    fun attrib(name: String): Int = GLES20.glGetAttribLocation(programId, name)
    fun uniform(name: String): Int = GLES20.glGetUniformLocation(programId, name)
}

object GameShaders {
    const val WORLD_VERTEX = """
        uniform mat4 uMVPMatrix;
        uniform mat4 uModelMatrix;
        uniform float uTime;
        uniform float uWindFactor;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        varying vec3 vNormal;
        varying vec4 vColor;
        varying vec3 vWorldPos;
        void main() {
            vec3 pos = aPosition;
            if (uWindFactor > 0.0 && pos.y > 0.05) {
                float sway = sin(uTime * 2.4 + pos.x * 0.6 + pos.z * 0.6) * 0.14 * uWindFactor * pos.y;
                pos.x += sway;
                pos.z += sway * 0.5;
            }
            vec4 worldPos = uModelMatrix * vec4(pos, 1.0);
            vWorldPos = worldPos.xyz;
            vNormal = normalize(mat3(uModelMatrix) * aNormal);
            vColor = aColor;
            gl_Position = uMVPMatrix * vec4(pos, 1.0);
        }
    """

    const val WORLD_FRAGMENT = """
        precision mediump float;
        uniform vec3 uSunDir;
        uniform vec3 uSunColor;
        uniform vec3 uAmbientSky;
        uniform vec3 uAmbientGround;
        uniform vec3 uFogColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform vec3 uCameraPos;
        uniform vec4 uTint;
        uniform float uUnlit;
        varying vec3 vNormal;
        varying vec4 vColor;
        varying vec3 vWorldPos;
        void main() {
            vec4 baseColor = vColor * uTint;
            if (uUnlit > 0.5) {
                gl_FragColor = baseColor;
                return;
            }
            vec3 n = normalize(vNormal);
            float ndotl = max(dot(n, normalize(uSunDir)), 0.0);
            float hemi = n.y * 0.5 + 0.5;
            vec3 ambient = mix(uAmbientGround, uAmbientSky, hemi);
            vec3 lit = baseColor.rgb * (ambient * 0.72 + uSunColor * ndotl * 0.78);
            float dist = length(vWorldPos - uCameraPos);
            float fogFactor = clamp((dist - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 1.0);
            vec3 finalRgb = mix(lit, uFogColor, fogFactor * fogFactor);
            gl_FragColor = vec4(finalRgb, baseColor.a);
        }
    """

    const val SKY_VERTEX = """
        uniform mat4 uMVPMatrix;
        attribute vec3 aPosition;
        varying vec3 vDir;
        void main() {
            vDir = normalize(aPosition);
            gl_Position = uMVPMatrix * vec4(aPosition, 1.0);
        }
    """

    const val SKY_FRAGMENT = """
        precision mediump float;
        uniform vec3 uZenithColor;
        uniform vec3 uHorizonColor;
        uniform vec3 uSunDir;
        uniform vec3 uSunColor;
        uniform float uTime;
        varying vec3 vDir;

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }
        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            vec2 u = f * f * (3.0 - 2.0 * f);
            return mix(
                mix(hash(i), hash(i + vec2(1.0, 0.0)), u.x),
                mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
                u.y
            );
        }
        void main() {
            vec3 d = normalize(vDir);
            float h = clamp(d.y, 0.0, 1.0);
            vec3 sky = mix(uHorizonColor, uZenithColor, pow(h, 0.58));
            float sunDot = max(dot(d, normalize(uSunDir)), 0.0);
            sky += uSunColor * pow(sunDot, 22.0) * 0.55;
            sky += uSunColor * smoothstep(0.996, 0.9992, sunDot) * 0.95;

            if (d.y > 0.04) {
                vec2 uv = d.xz / (d.y + 0.18) * 1.4 + vec2(uTime * 0.018, uTime * 0.008);
                float c = noise(uv) * 0.6 + noise(uv * 2.3 + 4.1) * 0.4;
                float cloudMask = smoothstep(0.54, 0.80, c) * smoothstep(0.04, 0.25, d.y);
                vec3 cloudCol = mix(vec3(0.96, 0.97, 1.0), uSunColor, 0.22);
                sky = mix(sky, cloudCol, cloudMask * 0.65);
            }
            gl_FragColor = vec4(sky, 1.0);
        }
    """

    const val WATER_VERTEX = """
        uniform mat4 uMVPMatrix;
        uniform float uTime;
        attribute vec3 aPosition;
        attribute vec3 aNormal;
        attribute vec4 aColor;
        varying vec3 vWorldPos;
        varying vec4 vColor;
        void main() {
            vec3 pos = aPosition;
            pos.y += sin(pos.x * 0.7 + uTime * 2.1) * 0.04 + cos(pos.z * 0.6 + uTime * 1.7) * 0.04;
            vWorldPos = pos;
            vColor = aColor;
            gl_Position = uMVPMatrix * vec4(pos, 1.0);
        }
    """

    const val WATER_FRAGMENT = """
        precision mediump float;
        uniform float uTime;
        uniform vec3 uFogColor;
        uniform float uFogStart;
        uniform float uFogEnd;
        uniform vec3 uCameraPos;
        uniform vec3 uSunColor;
        varying vec3 vWorldPos;
        varying vec4 vColor;
        void main() {
            float ripple = sin(vWorldPos.x * 1.8 + uTime * 2.6) * cos(vWorldPos.z * 1.5 - uTime * 2.0);
            vec3 base = vColor.rgb + vec3(0.06, 0.12, 0.14) * ripple + uSunColor * max(ripple, 0.0) * 0.14;
            float dist = length(vWorldPos - uCameraPos);
            float fogFactor = clamp((dist - uFogStart) / max(uFogEnd - uFogStart, 1.0), 0.0, 1.0);
            vec3 finalRgb = mix(base, uFogColor, fogFactor * fogFactor);
            gl_FragColor = vec4(finalRgb, vColor.a);
        }
    """
}
