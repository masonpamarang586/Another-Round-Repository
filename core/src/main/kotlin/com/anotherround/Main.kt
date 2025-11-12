/**
 * INFORMATION:
 *
 * Item assets: https://merchant-shade.itch.io/16x16-mixed-rpg-icons
 *
 *
 */

package com.anotherround

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import com.anotherround.Screens.BattleScreen

data class GameSession(val slotId: Int, var playerName: String)

class Main : KtxGame<KtxScreen>() {
    companion object {
        // 1/16 because tiles are 16x16
        const val UNIT_SCALE = 1f / 16f
    }

    val batch by lazy { SpriteBatch() }
    val camera by lazy { OrthographicCamera() }
    val worldViewport by lazy { FitViewport(10f, 20f, camera) }
    val uiViewport by lazy { ScreenViewport() }

    override fun create() {
        KtxAsync.initiate()

        addScreen(BattleScreen(this))
        addScreen(MainMenuScreen(this))
        setScreen<MainMenuScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}
