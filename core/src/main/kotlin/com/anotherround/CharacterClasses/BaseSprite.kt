package com.anotherround.CharacterClasses

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

open class BaseSprite(
    val viewport: Viewport,
    val cfg: SpriteConfig
) : Disposable {

    enum class State { Idle, Hurt, Dead, Attacking }

    private val idleTex = Texture(Gdx.files.internal(cfg.idlePath))
    private val idleRegion = TextureRegion(idleTex)

    private val hurtTex = Texture(Gdx.files.internal(cfg.damageRowPath))
    private val deathTex = Texture(Gdx.files.internal(cfg.deathRowPath))
    private val attackTex = Texture(Gdx.files.internal(cfg.attackRowPath))

    private val hurtAnim = buildRowAnim(hurtTex, cfg.frameSize, cfg.hurtFps)
    private val deathAnim = buildRowAnim(deathTex, cfg.frameSize, cfg.deathFps)
    private val attackAnim = buildRowAnim(attackTex, cfg.frameSize, cfg.attackFps)

    var state = State.Idle
    var stateTime = 0f

    fun attackDuration(): Float = attackAnim.animationDuration + cfg.endHoldAttack
    fun hurtDuration():   Float = hurtAnim.animationDuration   + cfg.endHoldHurt
    fun deathDuration():  Float = deathAnim.animationDuration  + cfg.endHoldDeath

    fun playIdle() { if (state != State.Dead) { state = State.Idle; stateTime = 0f } }
    fun playHurt() { if (state != State.Dead) { state = State.Hurt; stateTime = 0f } }
    fun playDeath(){ state = State.Dead; stateTime = 0f }
    fun playAttack(){ if (state != State.Dead) { state = State.Attacking; stateTime = 0f } }

    fun update(delta: Float) {
        stateTime += delta
        when (state) {
            State.Hurt -> if (stateTime >= hurtDuration()) playIdle()
            State.Attacking -> if (stateTime >= attackDuration()) playIdle()
            else -> {}
        }
    }

    fun draw(batch: Batch) {
        val x = viewport.worldWidth * 0.5f + cfg.offsetX - cfg.drawWidth * 0.5f
        val y = viewport.worldHeight * 0.5f + cfg.offsetY - cfg.drawHeight * 0.5f

        val reg = when(state) {
            State.Idle -> idleRegion
            State.Hurt -> frameAt(hurtAnim)
            State.Attacking -> frameAt(attackAnim)
            State.Dead -> frameAt(deathAnim)
        }
        batch.draw(reg, x, y, cfg.drawWidth, cfg.drawHeight)
    }

    override fun dispose() {
        idleTex.dispose()
        hurtTex.dispose()
        deathTex.dispose()
        attackTex.dispose()
    }

    private fun buildRowAnim(tex: Texture, size: Int, fps: Float): Animation<TextureRegion> {
        val cols = max(1, tex.width / size)
        val row = TextureRegion.split(tex, size, size)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (cfg.framesAreReversed) frames.reversedArray() else frames
        return Animation(1f / fps, *ordered)
    }

    private fun frameAt(anim: Animation<TextureRegion>): TextureRegion {
        val frames = anim.keyFrames
        val idx = minOf(anim.getKeyFrameIndex(stateTime), frames.size - 1)
        return frames[idx]
    }
}
