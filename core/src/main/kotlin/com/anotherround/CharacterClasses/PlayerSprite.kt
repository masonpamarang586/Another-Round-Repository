package com.anotherround.render

import com.anotherround.CharacterClasses.Player
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport
import kotlin.math.min
class PlayerSprite(
    private val viewport: Viewport,
    val player: Player = Player(name = "Hero"),
    spritesheetPath: String = "generic_char_v0.2/png/blue/char_blue_1_index00.png",
    private val drawWidth: Float = 3f,   // ~3 tiles wide (with 16px tiles, this is nicely sized)
    private val drawHeight: Float = 3f
) : Disposable {
    private val texture = Texture(Gdx.files.internal(spritesheetPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    private val region = TextureRegion(texture, 0, 0, texture.width, texture.height)

    fun draw(batch: Batch) {
        val cx = viewport.worldWidth * 0.5f
        val cy = viewport.worldHeight * 0.5f
        val drawX = cx - (drawWidth * 0.5f)
        val drawY = cy - (drawHeight * 0.5f)
        batch.draw(region, drawX, drawY, drawWidth, drawHeight)
    }

    override fun dispose() = texture.dispose()
}
