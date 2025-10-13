package com.comp362.anotherround.screen

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger

class GameScreen : KtxScreen{
    private val stage : Stage = Stage(ExtendViewport(16f, 9f))
    private val texture: Texture = Texture("assets/player_spritesheet.png")

    override fun show() {
    }

    override fun resize(width: Int, height: Int){
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        with(stage){
            act(delta)
            draw()
        }
    }

    override fun dispose(){
        stage.disposeSafely()
        texture.disposeSafely()
    }

    companion object{
        private val log = logger<GameScreen>()
    }
}
