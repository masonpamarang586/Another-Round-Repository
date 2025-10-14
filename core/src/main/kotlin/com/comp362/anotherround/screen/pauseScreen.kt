package com.comp362.anotherround.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport

/**
 * Usage from GameScreen:
 *   val pauseOverlay = PauseScreen(skin, onResume = { ... })
 *   pauseOverlay.takeInput() // to open
 *   pauseOverlay.render(delta) // when paused
 */
class PauseScreen(
    private val skin: Skin,
    private val onResume: () -> Unit,
    private val onNewGame: () -> Unit = {},
    private val onSave: () -> Unit = {},
) {
    val stage = Stage(ScreenViewport())

    init {
        // Dim background
        val dim = Image(skin.newDrawable("white", Color(0f, 0f, 0f, 0.6f))).apply { setFillParent(true) }
        stage.addActor(dim)

        // Centered panel with buttons
        val panel = Table(skin).apply {
            pad(48f)
            defaults().pad(20f).width(640f) // button width in pixels
            add(Label("Paused", skin)).padBottom(32f).row()

            val resume = TextButton("Resume", skin).apply {
                addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) = onResume()
                })
            }
            val newGame = TextButton("New Game", skin).apply {
                addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) = onNewGame()
                })
            }
            val save = TextButton("Save", skin).apply {
                addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) = onSave()
                })
            }
            val settings = TextButton("Settings", skin).apply {
                addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        // Simple built‑in dialog you can replace later
                        Dialog("Settings", skin).apply {
                            button("Close")
                        }.show(stage)
                    }
                })
            }

            add(resume).height(144f).row()         // taller buttons (pixels)
            add(newGame).height(144f).row()
            add(save).height(144f).row()
            add(settings).height(144f).row()
        }

        // Root table anchors the panel in the center
        val root = Table().apply {
            setFillParent(true)
            add(panel)
        }
        stage.addActor(root)
    }

    /** Route input to the pause UI (call when opening the pause menu). */
    fun takeInput() { Gdx.input.inputProcessor = stage }

    /** Draw/update the overlay each frame while paused. */
    fun render(delta: Float) {
        // Back/ESC quick close
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onResume()
        }
        stage.act(delta)
        stage.draw()
    }

    fun resize(width: Int, height: Int) = stage.viewport.update(width, height, true)
    fun dispose() = stage.dispose()
}
