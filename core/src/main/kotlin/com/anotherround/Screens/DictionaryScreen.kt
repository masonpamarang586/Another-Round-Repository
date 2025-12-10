package com.anotherround.Screens

import com.badlogic.gdx.Screen
import com.badlogic.gdx.Game
import com.anotherround.GameSession
import com.anotherround.Main
import com.badlogic.gdx.graphics.g2d.SpriteBatch

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import javax.swing.text.StyleConstants.Alignment


class DictionaryScreen(val game: Main) : KtxScreen {

    private lateinit var stage: Stage
    private lateinit var skin: Skin
    private val uiStage = Stage(game.uiViewport)

    private lateinit var batch: SpriteBatch

    private var font = BitmapFont()

    override fun show() {

        stage = Stage(ScreenViewport())
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("ui/uiskin.json"))

        val table = Table(skin)

        table.setFillParent(true)

        stage.addActor(table)

        batch = SpriteBatch()

        font = BitmapFont()

        val style = TextButton.TextButtonStyle().apply {
            font = BitmapFont()
            fontColor = Color.BLACK
            up   = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }

        val dictionaryLabel = Label("Dictionary", skin)
        dictionaryLabel.setPosition(500f, 2000f)
        dictionaryLabel.color = Color.BROWN
        dictionaryLabel.setFontScale(10f)
        dictionaryLabel.setAlignment(Align.center)
        stage.addActor(dictionaryLabel)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(232f, 230f, 223f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
        TODO("Not yet implemented")
    }

    override fun resume() {
        TODO("Not yet implemented")
    }

    override fun hide() {
    }

    override fun dispose() {
        stage.dispose()
        skin.dispose()
    }


}
