package com.anotherround.Screens

import com.anotherround.CharacterClasses.*
import com.anotherround.combat.CombatManager
import com.anotherround.combat.Action
import com.anotherround.combat.SfxEvent
import com.anotherround.Consumables.ConsumablesInventory
import com.anotherround.GameLogic
import com.anotherround.GameSession
import com.anotherround.Main
import com.anotherround.MainMenuScreen
import com.anotherround.SaveLoad.GameState
import com.anotherround.SaveLoad.SaveGame
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import ktx.app.KtxScreen
import ktx.graphics.use
import kotlin.math.max

class PrebattleScreen(val game: Main) : KtxScreen {

    var currentSession: GameSession? = null

    fun encounterStart(session: GameSession){

        val player = Player(name = "Hero")

        lateinit var enemy: Character
        lateinit var enemyKind: EnemyKind

        this.currentSession = session

        val worldStage = Stage(game.worldViewport)

    }

}
