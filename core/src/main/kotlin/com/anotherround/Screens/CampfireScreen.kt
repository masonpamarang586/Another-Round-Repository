package com.anotherround.Screens

import com.anotherround.Main
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.scenes.scene2d.Stage
import ktx.app.KtxScreen

class CampfireScreen(val game: Main) : KtxScreen {
    private val backgroundTexture = Texture(Gdx.files.internal(""))
    private val uiStage = Stage(game.uiViewport)

    override fun show() {
        Gdx.input.inputProcessor = uiStage

    }

    override fun render(delta: Float) {

    }

    override fun resize(width: Int, height: Int) {
        game.worldViewport.update(width, height, true)
    }

    override fun pause() {

    }

    override fun hide() {

    }

    override fun dispose() {

    }
}
