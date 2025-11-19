package com.anotherround.CharacterClasses

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport

interface SpritesInterface: Disposable {
    val viewport: Viewport
    val enemies: Enemies
    val idlePath: String
    val damageRowPath: String
    val deathRowPath: String
    val attackRowPath: String

    val drawWidth: Float
    val drawHeight: Float
    val framesAreReversed: Boolean
    val frameSize: Int
    val hurtFps: Float
    val deathFps: Float
    val attackFps: Float

    val idleTex: Texture
    val idleRegion: TextureRegion
    val hurtTex: Texture
    val hurtAnim: Animation<TextureRegion>
    val deathTex: Texture
    val deathAnim: Animation<TextureRegion>
    val attackTex: Texture
    val attackAnim: Animation<TextureRegion>

    val endHoldAttack: Float
    val endHoldHurt: Float
    val endHoldDeath: Float
    fun hurtDuration(): Float = hurtAnim.animationDuration + endHoldHurt
    fun deathDuration(): Float = deathAnim.animationDuration + endHoldDeath

    enum class State { Idle, Hurt, Dead, Attacking }
    var state: State
    var stateTime: Float

    fun playHurt() {
        if (state == State.Dead) return
        state = State.Hurt
        stateTime = 0f
    }

    fun playDeath() {
        state = State.Dead
        stateTime = 0f
    }

    fun playAttack() {
        if (state == State.Dead) return
        state = State.Attacking
        stateTime = 0f
    }

    fun update(delta: Float) {
        stateTime += delta
        when (state) {
            State.Hurt -> {
                val endTime = hurtAnim.animationDuration + endHoldHurt
                if (stateTime >= endTime) {
                    state = State.Idle
                    stateTime = 0f
                }
            }
            State.Attacking -> {
                val endTime = attackAnim.animationDuration + endHoldAttack
                if (stateTime >= endTime) { state = State.Idle; stateTime = 0f }
            }
            State.Dead -> {

            }
            else -> {}
        }
    }

    fun draw(batch: Batch) {
        val cx = viewport.worldWidth * 0.5f
        val cy = viewport.worldHeight * 0.5f
        val drawX = cx + 1.5f
        val drawY = cy - (drawHeight * 0.5f)

        val region: TextureRegion = when (state) {
            State.Idle -> idleRegion
            State.Hurt -> {
                val frames = hurtAnim.keyFrames
                val idx = minOf(hurtAnim.getKeyFrameIndex(stateTime), frames.size - 1)
                frames[idx]
            }
            State.Attacking -> {
                val frames = attackAnim.keyFrames
                val idx = minOf(attackAnim.getKeyFrameIndex(stateTime), frames.size - 1)
                frames[idx]
            }
            State.Dead -> {
                val frames = deathAnim.keyFrames
                val idx = minOf(deathAnim.getKeyFrameIndex(stateTime), frames.size - 1)
                frames[idx]
            }
        }
        batch.draw(region, drawX, drawY, drawWidth, drawHeight)
    }

    override fun dispose() {
        idleTex.dispose()
        hurtTex.dispose()
        deathTex.dispose()
        attackTex.dispose()
    }
}
