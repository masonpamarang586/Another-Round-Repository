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
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.ui.Image
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

        // UI elements
        val backgroundColor = Image(Texture((Gdx.files.internal("ui/dictionary_bgColor.png"))))
        val pageColor = Image(Texture((Gdx.files.internal("ui/dictionary_pageColor.png"))))
        val pageTitle = Image(Texture((Gdx.files.internal("ui/dictionary_Title.png"))))

        stage.addActor(backgroundColor)
        backgroundColor.setPosition(0f, 0f)

        stage.addActor(pageColor)
        pageColor.setSize(900f, 2000f)
        pageColor.setPosition(100f, 130f)

        stage.addActor(pageTitle)
        pageTitle.setSize(1000f, 500f)
        pageTitle.setPosition(40f, 2000f)


        // Enemy descriptions

        //Grunt
        val entry_Grunt = Image(Texture((Gdx.files.internal("generic_char_v0.2/png/red/char_red_1_index10.png"))))
        stage.addActor(entry_Grunt)
        entry_Grunt.setSize(500f, 500f)
        entry_Grunt.setPosition(50f, 1300f)

        //Phantom
        val entry_Phantom = Image(Texture((Gdx.files.internal("phantom/phantomIdleFrame1.png"))))
        stage.addActor(entry_Phantom)
        entry_Phantom.setSize(500f, 500f)
        entry_Phantom.setPosition(500f, 1350f)

        //Evil Wizard
        val entry_EvilWizard = Image(Texture((Gdx.files.internal("evil_wizard/evilWizardIdleFrame1.png"))))
        stage.addActor(entry_EvilWizard)
        entry_EvilWizard.setSize(500f, 500f)
        entry_EvilWizard.setPosition(120f, 570f)


        //Night Borne
        val entry_NightBorne = Image(Texture((Gdx.files.internal("nightborne/NightBorneIdleFrame1.png"))))
        stage.addActor(entry_NightBorne)
        entry_NightBorne.setSize(500f, 500f)
        entry_NightBorne.setPosition(490f, 450f)





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
