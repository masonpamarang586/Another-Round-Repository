package com.anotherround.render

import com.anotherround.CharacterClasses.Player
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

class PlayerSprite(
    private val viewport: Viewport,
    val player: Player = Player(name = "Hero"),
    idlePath: String = "generic_char_v0.2/png/blue/char_blue_1.png",
    attackRowPath: String = "generic_char_v0.2/png/blue/blue_attack1.png",
    damageRowPath: String = "generic_char_v0.2/png/blue/char_blue_1_damage.png",
    private val drawWidth: Float = 3f,
    private val drawHeight: Float = 3f,
    private val frameSize: Int = 60,
    private val attackFps: Float = 10f,
    private val hurtFps: Float = 12f,
    private val damageFramesAreReversed: Boolean = false
) : Disposable {

    companion object {
        private const val FRAME = 60 // frame size for both idle & attack
        private const val ATTACK_FPS = 10f
        private val FRAME_DT = 1f / ATTACK_FPS
        private val END_HOLD = FRAME_DT
    }

    private val idleTex = Texture(Gdx.files.internal(idlePath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val idleRegion = TextureRegion(idleTex, 0, 0, idleTex.width, idleTex.height)

    private val attackTex = Texture(Gdx.files.internal(attackRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val attackAnim: Animation<TextureRegion> = run {
        val cols = max(1, attackTex.width / FRAME)
        val row = TextureRegion.split(attackTex, FRAME, FRAME)[0]
        val frames = Array(cols) { i -> row[i] }
        Animation(1f / ATTACK_FPS, *frames).apply {
            playMode = Animation.PlayMode.NORMAL
        }
    }

    private val hurtTex = Texture(Gdx.files.internal(damageRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val hurtAnim: Animation<TextureRegion> = run {
        val cols = max(1, hurtTex.width / frameSize)
        val row = TextureRegion.split(hurtTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (damageFramesAreReversed) frames.reversedArray() else frames
        Animation(1f / hurtFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }
    private val endHoldHurt = (1f / hurtFps) * 0.5f
    fun hurtDuration(): Float = hurtAnim.animationDuration + endHoldHurt

    enum class State { Idle, Attacking, Hurt }
    private var state = State.Idle
    private var stateTime = 0f

    fun attackDuration(): Float = attackAnim.animationDuration + END_HOLD
    fun playHurt() {
        state = State.Hurt;
        stateTime = 0f
    }
    fun playAttack() {
        state = State.Attacking
        stateTime = 0f
    }

    fun update(delta: Float) {
        stateTime += delta
        when (state) {
            State.Attacking -> {
                val end = attackAnim.animationDuration + (1f / attackFps) * 0.5f
                if (stateTime >= end) { state = State.Idle; stateTime = 0f }
            }
            State.Hurt -> {
                val end = hurtAnim.animationDuration + endHoldHurt
                if (stateTime >= end) { state = State.Idle; stateTime = 0f }
            }
            else -> Unit
        }
    }

    fun draw(batch: Batch) {
        // keep left-of-center offset as before
        val cx = viewport.worldWidth * 0.5f
        val cy = viewport.worldHeight * 0.5f
        val drawX = cx - (drawWidth * 0.5f) - 2f
        val drawY = cy - (drawHeight * 0.5f)

        val region = when (state) {
            State.Idle -> idleRegion
            State.Attacking -> {
                val frames = attackAnim.keyFrames
                val idx = minOf(attackAnim.getKeyFrameIndex(stateTime), frames.size - 1)
                frames[idx]
            }
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
        attackTex.dispose()
        hurtTex.dispose()
    }
}
