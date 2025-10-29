/**
 * INFORMATION:
 *
 * Item assets: https://merchant-shade.itch.io/16x16-mixed-rpg-icons
 *
 *
 */

package com.anotherround

import com.anotherround.CharacterClasses.Enemy
import com.anotherround.CharacterClasses.Player
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.style.addStyle
import com.anotherround.PauseScreenUI
import com.anotherround.render.EnemySprite
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import kotlin.math.max
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound


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

        addScreen(FirstScreen(this))
        setScreen<FirstScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }
}

class FirstScreen(val game: Main) : KtxScreen {
    // TODO: using this for save game
    private var toastText: String? = null
    private var toastTimer = 0f
    private val toastLayout by lazy { com.badlogic.gdx.graphics.g2d.GlyphLayout()}
    private fun showToast(text: String, seconds: Float = 1.5f) {
        toastText = text
        toastTimer = seconds
    }
    // TODO: Use this.
    private lateinit var playerSprite: com.anotherround.render.PlayerSprite
    private lateinit var enemySprite: EnemySprite
    // TODO: for background music
    private lateinit var backgroundMusic: Music
    private lateinit var attackSound: Sound
    private lateinit var sfxPlayerAttack: Sound
    private lateinit var sfxEnemyAttack: Sound
    private lateinit var sfxPlayerHurt: Sound
    private lateinit var sfxEnemyHurt: Sound
    private lateinit var sfxEnemyDeath: Sound

    private val worldStage = Stage(game.worldViewport)
    // TODO: Use this.
    private val uiStage = Stage(game.uiViewport)

    //ui
    private val pauseUI by lazy { PauseScreenUI(game.uiViewport) }

    var font = BitmapFont()
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    private val tiledMap by lazy {
        val mapLoader = TmxMapLoader()
        mapLoader.load("world/test.tmx")
    }
    private val tiledMapCamera = OrthographicCamera()
    private val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap, Main.UNIT_SCALE)

    // fields
    private val player = Player(name = "Hero")
    private val enemy  = Enemy(name = "Meany")
    private lateinit var combat: com.anotherround.combat.CombatManager

    // TODO:
    //  Add event/onClick listeners for the buttons.
    //  Add textures for button being hovered and being pressed.
    private val table by lazy {
        val table = Table()

        val skin = Skin(Gdx.files.internal("atlas/ui.json"))

        val style = TextButton.TextButtonStyle()
        style.font = font
        style.fontColor = Color.BLACK
        style.up = skin.getDrawable("button-normal")
        style.down = skin.getDrawable("button-normal-pressed")
        style.over = skin.getDrawable("button-normal-over")
        skin.addStyle("default", style)

        val attackButton = TextButton("Attack", skin)
        attackButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val accepted = combat.requestPlayerAttack()
                Gdx.app.log("UI", if (accepted) "Player queued Attack" else "Attack ignored (not your turn?)")
            }
        })
        table.add(attackButton).width(400f).height(200f)
        table.row()


        val itemsButton = TextButton("Items", skin)
        table.add(itemsButton).pad(100f).width(400f).height(200f)

        table
    }


    override fun show() {
        playerSprite = com.anotherround.render.PlayerSprite(
            game.worldViewport,
            idlePath = "generic_char_v0.2/png/blue/char_blue_1_index00.png",
            attackRowPath = "generic_char_v0.2/png/blue/blue_attack1.png"
        )
        enemySprite = com.anotherround.render.EnemySprite(game.worldViewport)
        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/battle-fighting-warrior-drums-372078.mp3"))
        backgroundMusic.isLooping = true
        backgroundMusic.volume = 1.5f
        backgroundMusic.play()
        sfxPlayerAttack = Gdx.audio.newSound(Gdx.files.internal("audio/violent-sword-slice-393839.mp3"))
        sfxEnemyAttack  = Gdx.audio.newSound(Gdx.files.internal("audio/magical-hit-45356.mp3"))
        sfxPlayerHurt   = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyHurt    = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyDeath   = Gdx.audio.newSound(Gdx.files.internal("audio/sword-clattering-to-the-ground-393838.mp3"))
        combat = com.anotherround.combat.CombatManager(
            player, enemy,
            onLog = { msg -> Gdx.app.log("COMBAT", msg) },
            onActionStart = { action ->
                when (action) {
                    is com.anotherround.combat.Action.Attack -> {
                        if (action.attacker === player) {
                            playerSprite.playAttack()
                            combat.resolveDelay = playerSprite.attackDuration()
                        } else if (action.attacker === enemy) {
                            enemySprite.playAttack()
                            combat.resolveDelay = enemySprite.attackDuration()   // <- NEW
                        }
                    }
                }
            },
            onActionEnd   = { action ->
                when (action) {
                    is com.anotherround.combat.Action.Attack -> {
                        if (action.attacker === player) {
                            if (enemy.isAlive()) {
                                enemySprite.playHurt()
                                combat.pauseNextTurnFor(max(1.5f,enemySprite.hurtDuration())) // little hit-pause
                            } else {
                                enemySprite.playDeath()
                                combat.pauseNextTurnFor(enemySprite.deathDuration())
                            }
                        } else if (action.attacker === enemy) {
                            playerSprite.playHurt()
                            combat.pauseNextTurnFor(max(1.5f, playerSprite.hurtDuration()))
                        }
                        }
                }
            },
            onSfx = { e ->
                when (e) {
                    com.anotherround.combat.SfxEvent.PlayerAttack -> sfxPlayerAttack.play(0.9f)
                    com.anotherround.combat.SfxEvent.EnemyAttack  -> sfxEnemyAttack.play(0.9f)
                    com.anotherround.combat.SfxEvent.PlayerHurt   -> sfxPlayerHurt.play(0.9f)
                    com.anotherround.combat.SfxEvent.EnemyHurt    -> sfxEnemyHurt.play(0.9f)
                    com.anotherround.combat.SfxEvent.PlayerDeath  -> { /* add later if you have it */ }
                    com.anotherround.combat.SfxEvent.EnemyDeath   -> sfxEnemyDeath.play(1.0f)
                }
            },
            resolveDelay = 0f
        )

        pauseUI.updateFont(font)
        pauseUI.onResize()

        pauseUI.onSaveRequested = {
            try {
                com.anotherround.SaveLoad.SaveGame.save(player, enemy)
                Gdx.app.log("SAVE", "Game saved")
                showToast("Game Saved", 1.5f)   // <-- top-center
            } catch (t: Throwable) {
                Gdx.app.error("SAVE", "Failed to save", t)
                showToast("Save Failed", 1.5f)
            }
        }

        // Enable input for UI
        Gdx.input.inputProcessor = uiStage
        uiStage.addActor(table)
    }

    override fun resize(width: Int, height: Int) {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        font.dispose()
        val font = generator.generateFont(parameter)
        font.color = Color.BLACK
        this.font = font

        game.worldViewport.update(width, height, true)
        game.worldViewport.camera.update()
        game.uiViewport.update(width, height, true)
        game.uiViewport.camera.update()

        //ui
        pauseUI.updateFont(this.font)
        //pauseUI.pauseButtonHeightFraction = 0.10f
        pauseUI.onResize()
    }

    override fun render(delta: Float) {
        input()
        logic()
        draw()
    }

    /**
     * TODO: Handles the user's input.
     */
    fun input() {

    }

    /**
     * TODO: Handles the game logic.
     */
    fun logic() {
        combat.update(Gdx.graphics.deltaTime)
        playerSprite.update(Gdx.graphics.deltaTime)
        enemySprite.update(Gdx.graphics.deltaTime)
        if (toastTimer > 0f) {
            toastTimer -= Gdx.graphics.deltaTime
            if (toastTimer <= 0f) toastText = null
        }
    }

    /**
     * Draws everything.
     */
    fun draw() {
        drawGame()
        drawUI()
    }

    /**
     * Draws the game.
     */
    fun drawGame() {
        game.worldViewport.apply()
        game.batch.projectionMatrix = game.worldViewport.camera.combined

        game.batch.use {
            // Draw the world
            tiledMapCamera.setToOrtho(false, 10f, 20f)
            tiledMapCamera.update()
            tiledMapRenderer.setView(tiledMapCamera)
            tiledMapRenderer.render()

            // TODO: Draw the sprites
            playerSprite.draw(it)
            enemySprite.draw(it)
            //TODO: Draw health bar
            /*health = new NinePatch(gradient, 0, 0, 0, 0)
            width = currentHealth / totalHealth * totalBarWidth;
            container = new NinePatch(containerRegion, 5, 5, 2, 2);
            container.draw(batch, 5, 8, totalBarWidth + 10, 8);
            health.draw(batch, 10, 10, width, 4)
            */
        }
    }

    /**
     * Draws the UI.
     */
    fun drawUI() {
        uiStage.act(Gdx.graphics.deltaTime)
        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        game.batch.use {
//            // Why this has to be done: https://gamedev.stackexchange.com/questions/73688/why-is-my-text-is-too-large-even-when-scaled-to-05f-in-libgdx
//            val originalMatrix = it.projectionMatrix.cpy()
//            it.projectionMatrix = originalMatrix.cpy().scale(getWidthInPixels(), getHeightInPixels(), 1f)
//            game.font.draw(it, "Hello World", 0f * getWidthInPixels(), game.worldViewport.screenHeight / 2f)
//            it.projectionMatrix = originalMatrix

            // TODO: Draw the pause button.
            pauseUI.drawAndHandleInput(game.batch)

            // Draw the action menu.
            table.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f * 0.1f)
            table.center().bottom()
            table.draw(game.batch, 1f)

            toastText?.let { msg ->
                // subtle fade-out during the last 0.3s
                val alpha = if (toastTimer < 0.3f) toastTimer / 0.3f else 1f
                val oldColor = game.batch.color.cpy()
                game.batch.setColor(1f, 1f, 1f, alpha)

                toastLayout.setText(font, msg)
                val x = (Gdx.graphics.width  - toastLayout.width)  / 2f
                val y = (Gdx.graphics.height - 144f)
                font.draw(game.batch, toastLayout, x, y)

                game.batch.color = oldColor
            }
        }
    }

    /**
     * Gets the pixel ratio width.
     */
    fun getWidthInPixels(): Float {
        return game.worldViewport.worldWidth / game.worldViewport.screenWidth
    }

    /**
     * Gets the pixel ratio height.
     */
    fun getHeightInPixels(): Float {
        return game.worldViewport.worldHeight / game.worldViewport.screenHeight
    }

    override fun hide() {
        backgroundMusic.stop()
    }

    override fun dispose() {
        font.dispose()
        generator.dispose()
        worldStage.dispose()
        uiStage.dispose()
        tiledMap.dispose()
        tiledMapRenderer.dispose()
        pauseUI.dispose()
        playerSprite.dispose()
        enemySprite.dispose()
        backgroundMusic.dispose()
        sfxPlayerAttack.dispose()
        sfxEnemyAttack.dispose()
        sfxPlayerHurt.dispose()
        sfxEnemyHurt.dispose()
        sfxEnemyDeath.dispose()
        super.dispose()
    }
}
