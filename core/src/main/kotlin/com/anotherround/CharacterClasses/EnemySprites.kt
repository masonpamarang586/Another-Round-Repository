package com.anotherround.render

import com.anotherround.CharacterClasses.RedEnemy
import com.anotherround.CharacterClasses.SpritesInterface
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.max

class RedEnemySprite(
    override val viewport: Viewport,
    override val redEnemy: RedEnemy = RedEnemy("Red Enemy"),
    override val idlePath: String = "generic_char_v0.2/png/red/char_red_1_index10.png",
    override val damageRowPath: String = "generic_char_v0.2/png/red/char_red_1_damage.png",
    override val deathRowPath: String = "generic_char_v0.2/png/red/char_red_1_death.png",
    override val attackRowPath: String = "generic_char_v0.2/png/red/char_red_1_attack.png",

    override val drawWidth: Float = 3f,
    override val drawHeight: Float = 3f,
    override val framesAreReversed: Boolean = true,
    override val frameSize: Int = 60,

    override val idleFps: Float = 4f,
    override val hurtFps: Float = 12f,
    override val deathFps: Float = 10f,
    override val attackFps: Float = 10f

) : SpritesInterface {

    override val idleTex = Texture(Gdx.files.internal(idlePath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val idleRegion = TextureRegion(idleTex, 0, 0, idleTex.width, idleTex.height)
    override val hurtTex = Texture(Gdx.files.internal(damageRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val hurtAnim: Animation<TextureRegion> = run {
        val cols = max(1, hurtTex.width / frameSize)
        val row = TextureRegion.split(hurtTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / hurtFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }

    override val deathTex = Texture(Gdx.files.internal(deathRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val deathAnim: Animation<TextureRegion> = run {
        val cols = max(1, deathTex.width / frameSize)
        val row = TextureRegion.split(deathTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / deathFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }

    override val attackTex = Texture(Gdx.files.internal(attackRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val attackAnim: Animation<TextureRegion> = run {
        val cols = max(1, attackTex.width / frameSize)
        val row = TextureRegion.split(attackTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / attackFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }
    override val endHoldAttack = (1f / attackFps) * 0.5f
    fun attackDuration(): Float = attackAnim.animationDuration + endHoldAttack


    override val endHoldHurt = (1f / hurtFps) * 0.5f
    override val endHoldDeath = (1f / deathFps) * 1.0f
    override fun hurtDuration(): Float = hurtAnim.animationDuration + endHoldHurt
    override fun deathDuration(): Float = deathAnim.animationDuration + endHoldDeath
    override var state = SpritesInterface.State.Idle
    override var stateTime = 0f
}

class PhantomSprite(
    override val viewport: Viewport,
    override val redEnemy: RedEnemy = RedEnemy("Phantom"),
    override val idlePath: String = "generic_char_v0.2/png/red/char_red_1_index10.png",
    override val damageRowPath: String = "generic_char_v0.2/png/red/char_red_1_damage.png",
    override val deathRowPath: String = "generic_char_v0.2/png/red/char_red_1_death.png",
    override val attackRowPath: String = "generic_char_v0.2/png/red/char_red_1_attack.png",

    override val drawWidth: Float = 3f,
    override val drawHeight: Float = 3f,
    override val framesAreReversed: Boolean = true,
    override val frameSize: Int = 60,

    override val idleFps: Float = 4f,
    override val hurtFps: Float = 12f,
    override val deathFps: Float = 20f,
    override val attackFps: Float = 13f

) : SpritesInterface {

    override val idleTex = Texture(Gdx.files.internal(idlePath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val idleRegion = TextureRegion(idleTex, 0, 0, idleTex.width, idleTex.height)
    override val hurtTex = Texture(Gdx.files.internal(damageRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val hurtAnim: Animation<TextureRegion> = run {
        val cols = max(1, hurtTex.width / frameSize)
        val row = TextureRegion.split(hurtTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / hurtFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }

    override val deathTex = Texture(Gdx.files.internal(deathRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val deathAnim: Animation<TextureRegion> = run {
        val cols = max(1, deathTex.width / frameSize)
        val row = TextureRegion.split(deathTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / deathFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }

    override val attackTex = Texture(Gdx.files.internal(attackRowPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    override val attackAnim: Animation<TextureRegion> = run {
        val cols = max(1, attackTex.width / frameSize)
        val row = TextureRegion.split(attackTex, frameSize, frameSize)[0]
        val frames = Array(cols) { i -> row[i] }
        val ordered = if (framesAreReversed) frames.reversedArray() else frames
        Animation(1f / attackFps, *ordered).apply { playMode = Animation.PlayMode.NORMAL }
    }
    override val endHoldAttack = (1f / attackFps) * 0.5f
    fun attackDuration(): Float = attackAnim.animationDuration + endHoldAttack


    override val endHoldHurt = (1f / hurtFps) * 0.5f
    override val endHoldDeath = (1f / deathFps) * 1.0f
    override fun hurtDuration(): Float = hurtAnim.animationDuration + endHoldHurt
    override fun deathDuration(): Float = deathAnim.animationDuration + endHoldDeath
    override var state = SpritesInterface.State.Idle
    override var stateTime = 0f
}
