package com.comp362.anotherround.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Gdx.input
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.comp362.anotherround.AnotherRound
import com.comp362.anotherround.PauseScreen
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger
import java.awt.Color

class GameScreen(val game: AnotherRound) : KtxScreen{
    private val stage : Stage = Stage(ExtendViewport(16f, 9f))
    private val playerTexture: Texture = Texture("assets/player_spritesheet.png")

    private val batch = SpriteBatch();

    private val backgroundTexture = Texture("background.jpg")
    private val attackTexture = Texture("menu/attack.png")
    private val itemsTexture = Texture("menu/items.png")

    // UI parts
    private val skin by lazy { com.badlogic.gdx.scenes.scene2d.ui.Skin(Gdx.files.internal("skin/uiskin.json")) }
    private val hudStage by lazy { com.badlogic.gdx.scenes.scene2d.Stage(com.badlogic.gdx.utils.viewport.ScreenViewport()) }
    private var paused = false

    // Pause overlay
    private val pauseOverlay by lazy {
        PauseScreen(
            skin = skin,
            onResume = {
                paused = false
                Gdx.input.inputProcessor = hudStage
            },
            onNewGame = { /* TODO: */ },
            onSave = { /* TODO: */ }
        )
    }

    override fun show() {
        hudStage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        pauseOverlay.stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)

        // create pause button
        val pauseBtn = com.badlogic.gdx.scenes.scene2d.ui.TextButton("Pause", skin)
        val hudRoot = com.badlogic.gdx.scenes.scene2d.ui.Table().apply {
            setFillParent(true)
            top().right().pad(12f)
            add(pauseBtn).width(640f).height(256f)
            touchable = com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly
        }
        hudStage.addActor(hudRoot)

        // hud then overlay
        val mux = com.badlogic.gdx.InputMultiplexer(hudStage, pauseOverlay.stage)
        Gdx.input.inputProcessor = mux

        // start with hidden overlay
        pauseOverlay.stage.root.isVisible = false
        pauseOverlay.stage.root.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.disabled

        // allow overlay input
        pauseBtn.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(event: com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent?, actor: com.badlogic.gdx.scenes.scene2d.Actor?) {
                Gdx.app.log("UI", "Pause clicked")
                paused = true
                pauseOverlay.stage.root.isVisible = true
                pauseOverlay.stage.root.touchable = com.badlogic.gdx.scenes.scene2d.Touchable.enabled
            }
        })
    }

    override fun resize(width: Int, height: Int){
        game.viewport.update(width, height, true)
        hudStage.viewport.update(width, height, true)
        pauseOverlay.resize(width, height)
    }

    override fun render(delta: Float) {
        input()
        if (!paused) {
            logic()
        }
        with(stage){
            act(delta)
            draw()
        }

        // hud on top
        hudStage.act(delta)
        hudStage.draw()

        // pause overlay on top
        if (paused) {
            pauseOverlay.render(delta)
        }

    }

    fun input() {
        if (Gdx.input.isTouched) {
            // TODO: handle input
        }
    }


    override fun dispose(){
        stage.disposeSafely()
        playerTexture.disposeSafely()
        batch.disposeSafely()
        hudStage.dispose()
        pauseOverlay.dispose()
        skin.dispose()
        backgroundTexture.dispose()
        attackTexture.dispose()
        itemsTexture.dispose()
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
        game.shape.color = com.badlogic.gdx.graphics.Color.BLACK
        game.shape.rect(1f, 1f, 8f, 5f)
        game.shape.end()

        // Inner rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = com.badlogic.gdx.graphics.Color.WHITE
        game.shape.rect(1f + 0.25f, 1f + 0.25f, 8f - 0.5f, 5f - 0.5f)
        game.shape.end()

        game.batch.begin()
        game.batch.draw(attackTexture, 1.5f, 4f, 2f, 1f)
        game.batch.draw(itemsTexture, 6f, 4f, 2f, 1f)
        game.batch.end()
    }




    fun drawSprites() {




    }




    companion object{
        private val log = logger<GameScreen>()
    }
}
