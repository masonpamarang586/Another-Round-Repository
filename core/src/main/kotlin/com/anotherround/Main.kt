/**
 * INFORMATION:
 *
 * Item assets: https://merchant-shade.itch.io/16x16-mixed-rpg-icons
 *
 *
 */

package com.anotherround

import com.anotherround.CharacterClasses.Enemy
import com.anotherround.CharacterClasses.Player
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
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.style.addStyle
import com.anotherround.PauseScreenUI
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.actors.onTouchDown

class Main : KtxGame<KtxScreen>() {
    companion object {
        // 1/16 because tiles are 16x16
        const val UNIT_SCALE = 1f / 16f
    }

    val batch by lazy { SpriteBatch() }
    val camera by lazy { OrthographicCamera() }
    val worldViewport by lazy { FitViewport(10f, 20f, camera) }
    val uiViewport by lazy { ScreenViewport() }
    val player by lazy { Player(name = "Player") }
    val enemy by lazy { Enemy(name = "Slime") }

    override fun create() {
        KtxAsync.initiate()

        addScreen(BattleScreen(this))
        setScreen<BattleScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}

class BattleScreen(val game: Main) : KtxScreen {
    // TODO: Use this.
    private val worldStage = Stage(game.worldViewport)
    // TODO: Use this.
    private val uiStage = Stage(game.uiViewport)

    //ui
    private val pauseUI by lazy { PauseScreenUI(game.uiViewport) }

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
    var attackButton: TextButton? = null
    var itemsButton: TextButton? = null
    private val menuTable by lazy {
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
        this.attackButton = attackButton
        table.add(attackButton).width(400f).height(200f)
        table.row()

        val itemsButton = TextButton("Items", skin)
        this.itemsButton = itemsButton
        table.add(itemsButton).pad(100f).width(400f).height(200f)

        table
    }

    private val isViewingItems = true

    private val potionTexture by lazy {
        val texture = Texture(Gdx.files.internal("items/potions.png"))
        val region = TextureRegion(texture, 48, 32, 16,16)
        region
    }

    private val itemsTable by lazy {
        val table = Table()

        val skin = Skin(Gdx.files.internal("atlas/ui.json"))

        val style = TextButton.TextButtonStyle()
        style.font = font
        style.fontColor = Color.BLACK
        style.up = skin.getDrawable("button-normal")
        style.down = skin.getDrawable("button-normal-pressed")
        style.over = skin.getDrawable("button-normal-over")
        skin.addStyle("default", style)

        val potionImage = Image(potionTexture)
        table.add(potionImage).width(100f).height(100f)

        val buyButton = TextButton("Use (NaN)", skin)
        table.add(buyButton).width(400f).height(200f)

        table
    }

    override fun show() {
        GameLogic.screen = this
    }

    // TODO: Fix font turning into squares when reopening the application
    override fun resume() {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        font.dispose()
        val font = generator.generateFont(parameter)
        font.color = Color.BLACK
        this.font = font
    }

    override fun resize(width: Int, height: Int) {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        font.dispose()
        val font = generator.generateFont(parameter)
        font.color = Color.BLACK
        this.font = font

        game.worldViewport.update(width, height, true)
        game.worldViewport.camera.update()
        game.uiViewport.update(width, height, true)
        game.uiViewport.camera.update()

        //ui
        pauseUI.updateFont(this.font)
        //pauseUI.pauseButtonHeightFraction = 0.10f
        pauseUI.onResize()
    }

    override fun render(delta: Float) {
        input(delta)
        logic(delta)
        draw(delta)
    }

    /**
     * TODO: Handles the user's input.
     */
    fun input(delta: Float) {
        if (pauseUI.isPaused) {
            return
        }

        val touchInput: Vector2? = if (Gdx.input.isTouched) Vector2(Gdx.input.x.toFloat(), Gdx.input.y.toFloat()) else null
        if (touchInput == null) {
            return
        }

        if (isViewingItems) {

        } else {

        }
    }

    /**
     * TODO: Handles the game logic.
     */
    fun logic(delta: Float) {
        GameLogic.accumulator += delta
    }

    /**
     * Draws everything.
     */
    fun draw(delta: Float) {
        drawGame(delta)
        drawUI(delta)
    }

    /**
     * Draws the game.
     */
    fun drawGame(delta: Float) {
        game.worldViewport.apply()
        game.batch.projectionMatrix = game.worldViewport.camera.combined

        game.batch.use {
            // Draw the world
            tiledMapCamera.setToOrtho(false, 10f, 20f)
            tiledMapCamera.update()
            tiledMapRenderer.setView(tiledMapCamera)
            tiledMapRenderer.render()

            // TODO: Draw the sprites

            //TODO: Draw health bar
            /*health = new NinePatch(gradient, 0, 0, 0, 0)
            width = currentHealth / totalHealth * totalBarWidth;
            container = new NinePatch(containerRegion, 5, 5, 2, 2);
            container.draw(batch, 5, 8, totalBarWidth + 10, 8);
            health.draw(batch, 10, 10, width, 4)
            */
        }
    }

    /**
     * Draws the UI.
     */
    fun drawUI(delta: Float) {
        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        game.batch.use {
//            // Why this has to be done: https://gamedev.stackexchange.com/questions/73688/why-is-my-text-is-too-large-even-when-scaled-to-05f-in-libgdx
//            val originalMatrix = it.projectionMatrix.cpy()
//            it.projectionMatrix = originalMatrix.cpy().scale(getWidthInPixels(), getHeightInPixels(), 1f)
//            game.font.draw(it, "Hello World", 0f * getWidthInPixels(), game.worldViewport.screenHeight / 2f)
//            it.projectionMatrix = originalMatrix

            // TODO: Use the font from the FirstScreen class
            pauseUI.drawAndHandleInput(game.batch)

            if (!pauseUI.isPaused) {
                if (isViewingItems) {
                    itemsTable.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f)
                    itemsTable.draw(game.batch, 1f)
                } else {
                    menuTable.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f * 0.1f)
                    menuTable.bottom()
                    menuTable.draw(game.batch, 1f)
                }
            }
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
        pauseUI.dispose()
    }
}
