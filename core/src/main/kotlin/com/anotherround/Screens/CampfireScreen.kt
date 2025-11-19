package com.anotherround.Screens

import com.anotherround.GameLogic
import com.anotherround.Main
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.graphics.use

class CampfireScreen(val game: Main) : KtxScreen {
    private val uiStage = Stage(game.uiViewport)

    private var font = BitmapFont()
    private val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    private val tiledMap by lazy {
        val mapLoader = TmxMapLoader()
        mapLoader.load("world/test.tmx")
    }
    private val tiledMapCamera = OrthographicCamera()
    private val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap, Main.UNIT_SCALE)

    private var accumulator = 0f

    override fun show() {
        GameLogic.screen = this
        GameLogic.gameState = GameLogic.GameState.SHOP
        Gdx.input.inputProcessor = uiStage
    }

    override fun render(delta: Float) {
        input(delta)
        logic(delta)
        draw(delta)
    }

    override fun resize(width: Int, height: Int) {
        game.worldViewport.update(width, height, true)
        tiledMapCamera.update()
        updateFont()
    }

    override fun pause() {

    }

    override fun hide() {
        super.hide()
    }

    override fun dispose() {
        font.dispose()
        generator.dispose()
        uiStage.dispose()
        tiledMap.dispose()
        tiledMapRenderer.dispose()
    }

    private fun input(delta: Float) {

    }

    private fun logic(delta: Float) {
        if (accumulator >= 3f) {
            game.setScreen<BattleScreen>()
            hide()
        }
        accumulator += delta
    }

    private fun draw(delta: Float) {
        // Draw world
        game.worldViewport.apply()
        game.batch.projectionMatrix = game.worldViewport.camera.combined

        game.batch.use {
            // Draw tilemap
            tiledMapCamera.setToOrtho(false, 10f, 20f)
            tiledMapCamera.update()
            tiledMapRenderer.setView(tiledMapCamera)
            tiledMapRenderer.render()
        }

        // TODO: draw ui
    }

    private fun updateFont() {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f
        val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }
        font.disposeSafely()
        font = generator.generateFont(param).apply { color = Color.BLACK }
    }
}
