package com.anotherround.render

import com.anotherround.CharacterClasses.Enemy
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

class EnemySprite(
    private val viewport: Viewport,
    val enemy: Enemy = Enemy(name = "Enemy"),
    idlePath: String = "generic_char_v0.2/png/red/char_red_1_index10.png",
    damageRowPath: String = "generic_char_v0.2/png/red/char_red_1_damage.png",
    private val drawWidth: Float = 3f,
    private val drawHeight: Float = 3f,
    private val framesAreReversed: Boolean = true,
    private val frameSize: Int = 60,
    private val hurtFps: Float = 12f
) : Disposable {

    private val idleTex = Texture(Gdx.files.internal(idlePath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val idleRegion = TextureRegion(idleTex, 0, 0, idleTex.width, idleTex.height)
    private val hurtTex = Texture(Gdx.files.internal(damageRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val hurtAnim: Animation<TextureRegion> = run {
        val cols = max(1, hurtTex.width / frameSize)
        val row = TextureRegion.split(hurtTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / hurtFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }

    private val endHold = (1f / hurtFps) * 0.5f   // show last hurt frame a bit
    fun hurtDuration(): Float = hurtAnim.animationDuration + endHold

    enum class State { Idle, Hurt }
    private var state = State.Idle
    private var stateTime = 0f

    fun playHurt() {
        state = State.Hurt
        stateTime = 0f
    }

    fun update(delta: Float) {
        stateTime += delta
        if (state == State.Hurt) {
            val endTime = hurtAnim.animationDuration + endHold
            if (stateTime >= endTime) {
                state = State.Idle
                stateTime = 0f
            }
        }
    }

    fun draw(batch: Batch) {
        val cx = viewport.worldWidth * 0.5f
        val cy = viewport.worldHeight * 0.5f
        val drawX = cx + 1.5f // offset to right side of screen
        val drawY = cy - (drawHeight * 0.5f)
        val region = when (state) {
            State.Idle -> idleRegion
            State.Hurt -> {
                val frames = hurtAnim.keyFrames
                val idx = minOf(hurtAnim.getKeyFrameIndex(stateTime), frames.size - 1)
                frames[idx]
            }
        }
        batch.draw(region, drawX, drawY, drawWidth, drawHeight)
    }

    override fun dispose() {
        idleTex.dispose()
        hurtTex.dispose()
    }
}
