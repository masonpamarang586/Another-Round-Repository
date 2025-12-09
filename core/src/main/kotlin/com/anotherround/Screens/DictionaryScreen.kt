package com.anotherround.Screens

import com.badlogic.gdx.Screen
import com.badlogic.gdx.Game
import com.anotherround.GameSession
import com.anotherround.Main

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import ktx.app.KtxScreen


class DictionaryScreen(val game: Main) : KtxScreen {

    private lateinit var stage: Stage
    private lateinit var skin: Skin

    override fun show() {

        stage = Stage()
        Gdx.input.inputProcessor = stage

        skin = Skin(Gdx.files.internal("skin/uiskin.json"))

        val table = Table(skin)

        table.setFillParent(true)

        stage.addActor(table)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
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
