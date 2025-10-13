package com.comp362.anotherround

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.FitViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.async.KtxAsync

class Main : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val viewport by lazy {
        FitViewport(10f, 20f) }
    val shape by lazy { ShapeRenderer() }

    override fun create() {
        KtxAsync.initiate()

        addScreen(GameScreen(this))
        setScreen<GameScreen>()
    }

    override fun dispose() {
        batch.dispose()
        shape.dispose()
    }
}

class GameScreen(val game: Main) : KtxScreen {
    private val backgroundTexture = Texture("background.jpg")
    private val attackTexture = Texture("menu/attack.png")
    private val itemsTexture = Texture("menu/items.png")

    override fun render(delta: Float) {
        input()
        logic()
        draw()
    }

    override fun resize(width: Int, height: Int) {
        game.viewport.update(width, height, true)
    }

    override fun dispose() {

    }

    fun input() {
        if (Gdx.input.isTouched) {
            // TODO: handle input
        }
    }

    fun logic() {

    }

    fun draw() {
        // Applies viewport to the (uncentered) camera
        game.viewport.apply()

        // Necessary to get sprites to draw correctly
        game.batch.projectionMatrix = game.viewport.camera.combined

        // Combines draw calls together for performance
        game.batch.begin()

        val worldWidth = game.viewport.worldWidth
        val worldHeight = game.viewport.worldHeight

        game.batch.draw(backgroundTexture, 0f, 0f, worldWidth, worldHeight)

        game.batch.end()

        // TODO: draw sprites
        drawMenu()
        drawSprites()
    }

    fun drawMenu() {
        val worldWidth = game.viewport.worldWidth
        val worldHeight = game.viewport.worldHeight

        game.shape.projectionMatrix = game.viewport.camera.combined

        // Outer rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = Color.BLACK
        game.shape.rect(1f, 1f, 8f, 5f)
        game.shape.end()

        // Inner rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = Color.WHITE
        game.shape.rect(1f + 0.25f, 1f + 0.25f, 8f - 0.5f, 5f - 0.5f)
        game.shape.end()

        game.batch.begin()
        game.batch.draw(attackTexture, 1.5f, 4f, 2f, 1f)
        game.batch.draw(itemsTexture, 6f, 4f, 2f, 1f)
        game.batch.end()
    }

    fun drawSprites() {

    }
}
