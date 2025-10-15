package com.comp362.anotherround.screen

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener // <-- Add this line
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.Drawable
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.utils.Align

/**
 * A self-contained pause overlay.
 * GameScreen is responsible for calling show()/hide() and render()/resize()/dispose().
 */
class PauseScreen(
    private val skin: Skin,
    private val onResume: () -> Unit,
    private val onNewGame: () -> Unit = {},
    private val onSave: () -> Unit = {},
) {
    val stage = Stage(ScreenViewport())

    init {
        // Dim background (modal scrim) that consumes clicks so they don't leak to HUD
        val dim: Image = Image(skin.newDrawable("white", Color(0f, 0f, 0f, 0.6f))).apply {
            setFillParent(true)
            addListener(object : InputListener() {
                override fun touchDown(
                    event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int
                ): Boolean = true // consume
            })
        }
        stage.addActor(dim)

        // Centered panel with buttons
        val panel = Table(skin).apply {
            pad(48f)
            defaults().pad(20f).width(640f)
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
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        onSave()
                        showToast("Game Saved")
                    }
                })
            }
            val settings = TextButton("Settings", skin).apply {
                addListener(object : ClickListener() {
                    override fun clicked(e: InputEvent?, x: Float, y: Float) {
                        Dialog("Settings", skin).apply { button("Close") }.show(stage)
                    }
                })
            }

            add(resume).height(144f).row()
            add(newGame).height(144f).row()
            add(save).height(144f).row()
            add(settings).height(144f).row()
        }

        // Root container centers the panel
        val container = Table().apply {
            setFillParent(true)
            add(panel)
        }
        stage.addActor(container)

        // Start hidden
        hide()
    }

    fun show() {
        stage.root.isVisible = true
        stage.root.touchable = Touchable.enabled
    }

    fun hide() {
        stage.root.isVisible = false
        stage.root.touchable = Touchable.disabled
    }

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

    fun showToast(message: String, seconds: Float = 2f) {
        val bg = skin.newDrawable("white", Color(0f, 0f, 0f, 0.85f))

        val label = Label(message, skin).apply {
            setAlignment(Align.center)
            color = Color.WHITE
        }

        // Wrap label so we can pad + give it a background "pill"
        val bubble = Container(label).apply {
            background = bg
            pad(16f, 24f, 16f, 24f)  // <-- padding works on Container
        }

        val toastTable = Table().apply {
            setFillParent(true)
            add(bubble).center()
            bottom().padBottom(64f)
        }

        toastTable.color.a = 0f
        toastTable.addAction(
            Actions.sequence(
                Actions.fadeIn(0.2f),
                Actions.delay(seconds),
                Actions.fadeOut(0.25f),
                Actions.run { toastTable.remove() }
            )
        )

        stage.addActor(toastTable)
    }

}
