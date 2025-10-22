package com.comp362.anotherround.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.comp362.anotherround.AnotherRound
import com.comp362.anotherround.component.ImageComponent
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.log.logger
import com.github.quillraven.fleks.World
import com.comp362.anotherround.system.RenderSystem
import com.github.quillraven.fleks.world
import com.comp362.anotherround.component.ImageComponent.Companion.ImageComponentListener

class GameScreen(val game: AnotherRound) : KtxScreen {
    private val stage: Stage = Stage(ExtendViewport(16f, 9f))

    private val playerTexture: Texture = Texture("player_spritesheet.png")
    private val enemyTexture: Texture = Texture("enemy_slime_spritesheet.png")

    @com.github.quillraven.fleks.WorldCfgMarker
    private val world = world {
        entityCapacity = 64
        injectables { add(stage) }
        systems { add<RenderSystem>() }
        components { add<ImageComponentListener>() }
    }

    private val batch = SpriteBatch()

    private val backgroundTexture = Texture("background.jpg")
    private val attackTexture = Texture("menu/attack.png")
    private val itemsTexture = Texture("menu/items.png")

    // UI
    private val skin by lazy { com.badlogic.gdx.scenes.scene2d.ui.Skin(Gdx.files.internal("skin/uiskin.json")) }
    private val hudStage by lazy { com.badlogic.gdx.scenes.scene2d.Stage(com.badlogic.gdx.utils.viewport.ScreenViewport()) }
    private var paused = false

    // Keep this multiplexer for the entire lifetime of the screen
    private lateinit var inputMux: InputMultiplexer

    // Pause overlay
    private val pauseOverlay: PauseScreen by lazy {
        PauseScreen(
            skin = skin,
            onResume = {
                paused = false
                pauseOverlay.hide()      // hide overlay here (no inputProcessor swapping!)
            },
            onNewGame = { /* TODO */ },
            onSave = { /* TODO */ }
        )
    }

    override fun show() {
        log.debug { "GameScreen gets shown" }

        // Sample entities
        world.entity {
            add<ImageComponent> {
                image = Image(TextureRegion(playerTexture, 16, 16)).apply {
                    setSize(4f, 4f)
                    setPosition(1.2f, 11.8f)
                }
            }
        }
        world.entity {
            add<ImageComponent> {
                image = Image(TextureRegion(enemyTexture, 16, 16)).apply {
                    setSize(4f, 4f)
                    setPosition(10.5f, 11.8f)
                }
            }
        }

        hudStage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)
        pauseOverlay.stage.viewport.update(Gdx.graphics.width, Gdx.graphics.height, true)

        // Pause button
        val pauseBtn = com.badlogic.gdx.scenes.scene2d.ui.TextButton("Pause", skin)
        val hudRoot = com.badlogic.gdx.scenes.scene2d.ui.Table().apply {
            setFillParent(true)
            top().right().pad(12f)
            add(pauseBtn).width(640f).height(256f)
            touchable = com.badlogic.gdx.scenes.scene2d.Touchable.childrenOnly
        }
        hudStage.addActor(hudRoot)

        // Always route input through a single multiplexer (overlay FIRST so it has priority)
        inputMux = InputMultiplexer(pauseOverlay.stage, hudStage)
        Gdx.input.inputProcessor = inputMux

        // Start with overlay hidden
        pauseOverlay.hide()

        pauseBtn.addListener(object : com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
            override fun changed(
                event: com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent?,
                actor: com.badlogic.gdx.scenes.scene2d.Actor?
            ) {
                Gdx.app.log("UI", "Pause clicked")
                paused = true
                pauseOverlay.show()
            }
        })
    }

    override fun resize(width: Int, height: Int) {
        game.viewport.update(width, height, true)
        hudStage.viewport.update(width, height, true)
        pauseOverlay.resize(width, height)
    }

    override fun render(delta: Float) {
        input()

        // Freeze time for ECS while paused (still renders, doesn't advance)
        val ecsDelta = if (paused) 0f else delta

        // Always draw the world/background so the screen doesn't go black
        draw()

        // Let ECS render (and only animate when not paused)
        world.update(ecsDelta)

        // HUD on top
        hudStage.act(delta)
        hudStage.draw()

        // Pause overlay on top when active
        if (paused) {
            pauseOverlay.render(delta)
        }
    }

    fun input() {
        if (Gdx.input.isTouched) {
            // TODO: gameplay input
        }
    }

    override fun dispose() {
        stage.disposeSafely()
        playerTexture.disposeSafely()
        enemyTexture.disposeSafely()
        batch.disposeSafely()
        hudStage.dispose()
        pauseOverlay.dispose()
        skin.dispose()
        backgroundTexture.dispose()
        attackTexture.dispose()
        itemsTexture.dispose()
        world.dispose()
    }

    fun logic() { /* gameplay update */ }

    fun draw() {
        // Background
        game.viewport.apply()
        game.batch.projectionMatrix = game.viewport.camera.combined
        game.batch.begin()
        game.batch.draw(backgroundTexture, 0f, 0f, game.viewport.worldWidth, game.viewport.worldHeight)
        game.batch.end()

        // Menu frame + icons (optional while paused; fine to keep)
        drawMenu()
    }

    fun drawMenu() {
        game.shape.projectionMatrix = game.viewport.camera.combined

        // Outer rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = com.badlogic.gdx.graphics.Color.BLACK
        game.shape.rect(1f, 1f, 8f, 5f)
        game.shape.end()

        // Inner rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = com.badlogic.gdx.graphics.Color.WHITE
        game.shape.rect(1.25f, 1.25f, 7.5f, 4.5f)
        game.shape.end()

        game.batch.begin()
        game.batch.draw(attackTexture, 1.5f, 4f, 2f, 1f)
        game.batch.draw(itemsTexture, 6f, 4f, 2f, 1f)
        game.batch.end()
    }

    companion object {
        private val log = logger<GameScreen>()
    }
}
