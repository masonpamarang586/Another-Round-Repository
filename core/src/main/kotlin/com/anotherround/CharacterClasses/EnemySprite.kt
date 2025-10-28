package com.anotherround.render

import com.anotherround.CharacterClasses.Enemy
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.viewport.Viewport

/**
 * Displays an enemy sprite (flipped horizontally) centered-right of the screen.
 * Default path: assets/generic_char_v0.2/png/blue/char_blue_enemy.png
 */
class EnemySprite(
    private val viewport: Viewport,
    val enemy: Enemy = Enemy(name = "Enemy"),
    spritesheetPath: String = "generic_char_v0.2/png/red/char_red_1_index10.png",
    private val drawWidth: Float = 3f,
    private val drawHeight: Float = 3f
) : Disposable {

    private val texture = Texture(Gdx.files.internal(spritesheetPath)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    private val region = TextureRegion(texture, 0, 0, texture.width, texture.height).apply {}

    fun draw(batch: Batch) {
        val cx = viewport.worldWidth * 0.5f
        val cy = viewport.worldHeight * 0.5f
        val drawX = cx + 1.5f // offset to right side of screen
        val drawY = cy - (drawHeight * 0.5f)
        batch.draw(region, drawX, drawY, drawWidth, drawHeight)
    }

    override fun dispose() = texture.dispose()
}
