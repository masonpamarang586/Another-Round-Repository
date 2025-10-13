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
import ktx.actors.stage
import ktx.app.KtxScreen
import ktx.assets.disposeSafely
import ktx.graphics.use
import ktx.log.logger
import java.awt.Color

class GameScreen : KtxScreen{
    private val stage : Stage = Stage(ExtendViewport(16f, 9f))
    private val playerTexture: Texture = Texture("assets/player_spritesheet.png")

    private val batch = SpriteBatch();

    private val backgroundTexture = Texture("background.jpg")
    private val attackTexture = Texture("menu/attack.png")
    private val itemsTexture = Texture("menu/items.png")

    override fun show() {
    }

    override fun resize(width: Int, height: Int){
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        input()
        logic()
        with(stage){
            act(delta)
            draw()
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
    }

    fun logic() {
    }

    fun draw() {
        // Applies viewport to the (uncentered) camera
        stage.viewport.apply()




        // Necessary to get sprites to draw correctly
        stage.batch.projectionMatrix = stage.viewport.camera.combined




        // Combines draw calls together for performance
        stage.batch.begin()




        val worldWidth = stage.viewport.worldWidth
        val worldHeight = stage.viewport.worldHeight




        stage.batch.draw(backgroundTexture, 0f, 0f, worldWidth, worldHeight)




        stage.batch.end()




        // TODO: draw sprites
        drawMenu()
        drawSprites()
    }

    fun drawMenu() {
        /*val worldWidth = stage.viewport.worldWidth
        val worldHeight = stage.viewport.worldHeight




        stage.shape.projectionMatrix = stage.viewport.camera.combined




        // Outer rectangle
        stage.shape.begin(ShapeRenderer.ShapeType.Filled)
        stage.shape.color = Color.BLACK
        stage.shape.rect(1f, 1f, 8f, 5f)
        stage.shape.end()




        // Inner rectangle
        game.shape.begin(ShapeRenderer.ShapeType.Filled)
        game.shape.color = Color.WHITE
        game.shape.rect(1f + 0.25f, 1f + 0.25f, 8f - 0.5f, 5f - 0.5f)
        game.shape.end()




        game.batch.begin()
        game.batch.draw(attackTexture, 1.5f, 4f, 2f, 1f)
        game.batch.draw(itemsTexture, 6f, 4f, 2f, 1f)
        game.batch.end()*/
    }




    fun drawSprites() {




    }




    companion object{
        private val log = logger<GameScreen>()
    }
}
