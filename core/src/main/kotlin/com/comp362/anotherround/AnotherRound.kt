package com.comp362.anotherround

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import com.badlogic.gdx.assets.AssetManager
import com.comp362.anotherround.screen.GameScreen
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.utils.viewport.FitViewport

class AnotherRound : KtxGame<KtxScreen>() {
    val batch by lazy { SpriteBatch() }
    val assets = AssetManager()
    val viewport by lazy {
        FitViewport(10f, 20f) }
    val shape by lazy { ShapeRenderer() }

    override fun create() {
        Gdx.app.logLevel = Application.LOG_DEBUG
        KtxAsync.initiate()

        addScreen(GameScreen(this))
        setScreen<GameScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        shape.dispose()
        assets.dispose()
        super.dispose()
    }
}
