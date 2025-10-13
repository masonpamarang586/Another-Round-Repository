package com.comp362.anotherround

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.Texture.TextureFilter.Linear
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.app.clearScreen
import ktx.assets.disposeSafely
import ktx.assets.toInternalFile
import ktx.async.KtxAsync
import ktx.graphics.use
import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.comp362.anotherround.screen.GameScreen
import ktx.assets.getAsset
import ktx.assets.load


enum class TextureAtlasAssets(val path: String) {
    Game("images/game.atlas")
}

inline fun AssetManager.load(asset: TextureAtlasAssets) = load<TextureAtlas>(asset.path)
inline operator fun AssetManager.get(asset: TextureAtlasAssets) = getAsset<TextureAtlas>(asset.path)


class AnotherRound : KtxGame<KtxScreen>() {

    val batch by lazy { SpriteBatch() }
    //val font by lazy { BitmapFont() }
    val assets = AssetManager()

    @Override
    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG
        KtxAsync.initiate()

        addScreen(GameScreen())
        setScreen<GameScreen>()
        super.create()

    }

    override fun dispose() {
        batch.dispose()
        //font.dispose()
        assets.dispose()
        super.dispose()
    }
}

class FirstScreen : KtxScreen {

    private val image = Texture("logo.png".toInternalFile(), true).apply { setFilter(Linear, Linear) }
    private val batch = SpriteBatch();

    private val playerTexture = Texture("assets/player_spritesheet.png")



    override fun render(delta: Float) {

        clearScreen(red = 0.7f, green = 0.7f, blue = 0.7f)
        batch.use {
            it.draw(image, 100f, 160f)
        }
    }

    override fun show() {
    }

    override fun dispose() {
        image.disposeSafely()
        playerTexture.disposeSafely()
        batch.disposeSafely()
    }
}
