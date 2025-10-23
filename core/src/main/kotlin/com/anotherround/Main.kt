/**
 * INFORMATION:
 *
 * Item assets: https://merchant-shade.itch.io/16x16-mixed-rpg-icons
 *
 *
 */

package com.anotherround

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.style.addStyle

class Main : KtxGame<KtxScreen>() {
    companion object {
        // 1/16 because tiles are 16x16
        const val UNIT_SCALE = 1f / 16f
    }

    val batch by lazy { SpriteBatch() }
    val camera by lazy { OrthographicCamera() }
    val worldViewport by lazy { FitViewport(10f, 20f, camera) }
    val uiViewport by lazy { ScreenViewport() }

    override fun create() {
        KtxAsync.initiate()

        addScreen(FirstScreen(this))
        setScreen<FirstScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}

class FirstScreen(val game: Main) : KtxScreen {
    // TODO: Use this.
    private val worldStage = Stage(game.worldViewport)
    // TODO: Use this.
    private val uiStage = Stage(game.uiViewport)

    var font = BitmapFont()
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    

    private val tiledMap by lazy {
        val mapLoader = TmxMapLoader()
        mapLoader.load("world/test.tmx")
    }
    private val tiledMapCamera = OrthographicCamera()
    private val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap, Main.UNIT_SCALE)

    // TODO:
    //  Add event/onClick listeners for the buttons.
    //  Add textures for button being hovered and being pressed.
    private val table by lazy {
        val table = Table()

        val skin = Skin(Gdx.files.internal("atlas/ui.json"))

        val style = TextButton.TextButtonStyle()
        style.font = font
        style.fontColor = Color.BLACK
        style.up = skin.getDrawable("button-normal")
        style.down = skin.getDrawable("button-normal-pressed")
        style.over = skin.getDrawable("button-normal-over")
        skin.addStyle("default", style)

        val attackButton = TextButton("Attack", skin)
        table.add(attackButton).width(400f).height(200f)
        table.row()

        val itemsButton = TextButton("Items", skin)
        table.add(itemsButton).pad(100f).width(400f).height(200f)

        table
    }

    override fun show() {

    }

    override fun resize(width: Int, height: Int) {
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = (Gdx.graphics.height * 0.05).toInt()
        parameter.minFilter = Texture.TextureFilter.Nearest
        parameter.magFilter = Texture.TextureFilter.Nearest

        val font = generator.generateFont(parameter)
        font.color = Color.BLACK
        this.font = font

        game.worldViewport.update(width, height, true)
        game.worldViewport.camera.update()
        game.uiViewport.update(width, height, true)
        game.uiViewport.camera.update()
    }

    override fun render(delta: Float) {
        input()
        logic()
        draw()
    }

    /**
     * TODO: Handles the user's input.
     */
    fun input() {

    }

    /**
     * TODO: Handles the game logic.
     */
    fun logic() {

    }

    /**
     * Draws everything.
     */
    fun draw() {
        drawGame()
        drawUI()
    }

    /**
     * Draws the game.
     */
    fun drawGame() {
        game.worldViewport.apply()
        game.batch.projectionMatrix = game.worldViewport.camera.combined

        game.batch.use {
            // Draw the world
            tiledMapCamera.setToOrtho(false, 10f, 20f)
            tiledMapCamera.update()
            tiledMapRenderer.setView(tiledMapCamera)
            tiledMapRenderer.render()

            // TODO: Draw the sprites
        }
    }

    /**
     * Draws the UI.
     */
    fun drawUI() {
        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        game.batch.use {
//            // Why this has to be done: https://gamedev.stackexchange.com/questions/73688/why-is-my-text-is-too-large-even-when-scaled-to-05f-in-libgdx
//            val originalMatrix = it.projectionMatrix.cpy()
//            it.projectionMatrix = originalMatrix.cpy().scale(getWidthInPixels(), getHeightInPixels(), 1f)
//            game.font.draw(it, "Hello World", 0f * getWidthInPixels(), game.worldViewport.screenHeight / 2f)
//            it.projectionMatrix = originalMatrix

            // TODO: Draw the pause button.

            // Draw the action menu.
            table.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f * 0.1f)
            table.center().bottom()
            table.draw(game.batch, 1f)
        }
    }

    /**
     * Gets the pixel ratio width.
     */
    fun getWidthInPixels(): Float {
        return game.worldViewport.worldWidth / game.worldViewport.screenWidth
    }

    /**
     * Gets the pixel ratio height.
     */
    fun getHeightInPixels(): Float {
        return game.worldViewport.worldHeight / game.worldViewport.screenHeight
    }

    override fun dispose() {
        font.dispose()
        generator.dispose()
        worldStage.dispose()
        uiStage.dispose()
        tiledMap.dispose()
        tiledMapRenderer.dispose()
    }
}
