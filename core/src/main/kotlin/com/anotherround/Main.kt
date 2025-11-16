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
import com.anotherround.Consumables.ConsumablesInventory
import com.anotherround.SaveLoad.GameState
import com.anotherround.SaveLoad.SaveGame
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.utils.viewport.FitViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.async.KtxAsync
import com.anotherround.Screens.CampfireScreen
import com.anotherround.Screens.PauseScreenUI
import com.anotherround.render.EnemySprite
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.maps.tiled.TmxMapLoader
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import ktx.graphics.use
import ktx.style.addStyle
import kotlin.math.max
import com.badlogic.gdx.scenes.scene2d.Group
// --- NEW IMPORTS ---
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextTooltip
import com.badlogic.gdx.scenes.scene2d.ui.TooltipManager
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

data class GameSession(val slotId: Int, var playerName: String)

// --- NEW DATA CLASS FOR POTIONS ---
data class Potion(
    val name: String,
    val description: String,
    val healAmount: Int,
    val textureRegion: TextureRegion
)

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

        addScreen(MainMenuScreen(this))
        addScreen(BattleScreen(this))
        addScreen(CampfireScreen(this))
        setScreen<MainMenuScreen>()

        super.create()
    }

    override fun dispose() {
        batch.dispose()
        super.dispose()
    }

    override fun render() {
        super.render()
    }
}

class BattleScreen(val game: Main) : KtxScreen {

    var currentSession: GameSession? = null

    // --- POTION INVENTORY ---
    // This is now a list that acts as our stack
    private val potionInventory = mutableListOf<Potion>()
    // ---

    fun startNewGame(session: GameSession) {
        this.currentSession = session
        Gdx.app.log("BattleScreen", "Starting new game for ${session.playerName} in slot ${session.slotId}")

        // Reset player/enemy stats to default for a new game
        player.name = session.playerName
        player.health = 100
        player.level = 1

        enemy.health = 20
        enemy.level = 1

        // --- UPDATED ---
        // Add 3 default potions to the inventory
        potionInventory.clear()
        repeat(3) {
            potionInventory.add(createHealthPotion())
        }
        // Add a mana potion for variety
        potionInventory.add(createManaPotion())
        // ---

//        showToast("New Game: ${session.playerName}!", 1.5f)
    }

    fun loadSavedGame(state: GameState, slot: Int) {
        this.currentSession = GameSession(slot, state.player.name)
        Gdx.app.log("BattleScreen", "Loading game for ${state.player.name} from slot $slot")

        // Apply saved stats to the player
        player.name = state.player.name
        player.health = state.player.health
        player.level = state.player.level
        player.defenseStat = state.player.defenseStat
        player.attackStat = state.player.attackStat

        // Apply saved stats to the enemy
        enemy.health = state.enemy.health
        enemy.level = state.enemy.level
        enemy.defenseStat = state.enemy.defenseStat
        enemy.attackStat = state.enemy.attackStat

        // --- UPDATED ---
        // Re-create the potion inventory from the saved count
        potionInventory.clear()
        repeat(state.potions) {
            potionInventory.add(createHealthPotion())
            // Note: This only saves/loads Health Potions.
            // A more complex system would save the *type* of each potion.
        }
        // ---

//        showToast("Loaded Game: ${state.player.name}", 1.5f)
    }

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

    private lateinit var sfxItemHeal: Sound
    private lateinit var sfxItemFail: Sound

    private val worldStage = Stage(game.worldViewport)
    // TODO: Use this.
    private val uiStage = Stage(game.uiViewport)

    //ui
    private val pauseUI by lazy { PauseScreenUI(game.uiViewport) }

    var font = BitmapFont()
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    // --- CLEANUP: Shared Skin and Style ---
    private val skin by lazy {
        Skin(Gdx.files.internal("atlas/ui.json"))
    }

    private val buttonStyle by lazy {
        TextButton.TextButtonStyle().apply {
            font = this@BattleScreen.font // Uses the already-sized font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }
    }
    // ---

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

    private val playerHealthLabel by lazy {
        // Use the shared style
        val label = TextButton("${player.health}", buttonStyle)
        label.width = 400f
        label.height = 200f
        label
    }

    private val enemyHealthLabel by lazy {
        // Use the shared style
        val label = TextButton("${enemy.health}", buttonStyle)
        label.width = 400f
        label.height = 200f
        label
    }

    lateinit var attackButton: TextButton
    lateinit var itemsButton: TextButton

    private val menuTable by lazy {
        val table = Table()

        // Use the shared style
        val attackButton = TextButton("Attack", buttonStyle)
        attackButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                val accepted = combat.requestPlayerAttack()
                Gdx.app.log("UI", if (accepted) "Player queued Attack" else "Attack ignored (not your turn?)")
            }
        })
        this.attackButton = attackButton
        table.add(attackButton).width(400f).height(200f)
        table.row()

        // Use the shared style
        val itemsButton = TextButton("Items", buttonStyle)
        this.itemsButton = itemsButton
        itemsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (combat.canOpenMenu() && !isShowingItems) {
                    isShowingItems = true
                    // --- NEW: Update the table when it's opened ---
                    updateItemsTable()
                }
            }
        })
        table.add(itemsButton).pad(100f).width(400f).height(200f)

        table
    }

    private var isShowingItems = false

    // --- NEW: Load the entire spritesheet ---
    private val potionsSpritesheet by lazy {
        Texture(Gdx.files.internal("items/potions.png"))
    }

    // --- NEW: Define Potion types ---
    private val healthPotionRegion by lazy {
        TextureRegion(potionsSpritesheet, 48, 32, 16, 16)
    }
    private val manaPotionRegion by lazy {
        TextureRegion(potionsSpritesheet, 64, 32, 16, 16)
    }

    // --- NEW: Potion creation functions ---
    private fun createHealthPotion() = Potion(
        name = "Health Potion",
        description = "Heals 10 HP.",
        healAmount = 10,
        textureRegion = healthPotionRegion
    )

    private fun createManaPotion() = Potion(
        name = "Mana Potion",
        description = "Restores 10 MP.",
        healAmount = 0, // Or add mana logic
        textureRegion = manaPotionRegion
    )

    // --- REMOVED: Old `potions` and `useButton` variables ---

    // --- REBUILT: itemsTable is now a placeholder, built by functions ---
    private lateinit var itemsTable: Table
    private lateinit var itemsGrid: Table // This will hold the 8 slots
    private lateinit var tooltipLabelStyle: Label.LabelStyle
    private lateinit var itemTooltipStyle: TextTooltip.TextTooltipStyle

    // --- NEW: Function to build the items table structure ---
    private fun buildItemsTable() {
        // Setup Tooltip Manager
        val tooltipManager = TooltipManager.getInstance()
        tooltipManager.initialTime = 0f // Hover for 0.5s to show
        tooltipManager.resetTime = 0f
        tooltipManager.hideAll()

        // Style for the tooltip label
        if (!this::tooltipLabelStyle.isInitialized) {
            tooltipLabelStyle = Label.LabelStyle(font, Color.WHITE)
        }
        val tooltipBackground = TextureRegionDrawable(onePixel(Color(0.2f, 0.2f, 0.2f, 0.8f)))
        // This is now our reusable "template style"
        itemTooltipStyle = TextTooltip.TextTooltipStyle(tooltipLabelStyle, tooltipBackground)
        // Initializes sound effects
        sfxItemHeal  = Gdx.audio.newSound(Gdx.files.internal("audio/item-use.mp3"))
        sfxItemFail  = Gdx.audio.newSound(Gdx.files.internal("audio/item-fail.mp3"))

        // Create the main table
        itemsTable = Table()
        itemsTable.setFillParent(true) // This table will dim the background
        itemsTable.background(TextureRegionDrawable(onePixel(Color(0f, 0f, 0f, 0.7f))))
        itemsTable.center()

        // Create an inner table to hold the grid and back button
        val innerTable = Table()
        itemsTable.add(innerTable)

        // This table will hold the 8 item slots
        itemsGrid = Table()
        innerTable.add(itemsGrid).pad(750f)
        innerTable.row()

        // Return button, goes back when you click it
        val backButton = TextButton("Return", buttonStyle)
        backButton.addListener(object: ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (isShowingItems) {
                    isShowingItems = false
                }
            }
        })

        // Add the back button at the bottom
        innerTable.add(backButton).width(400f).height(200f).padTop(50f)
    }

    // --- NEW: Function to update the 8 item slots ---
    private fun updateItemsTable() {
        itemsGrid.clear() // Clear all old slots
        TooltipManager.getInstance().hideAll() // Hide any active tooltip

        val slotSize = 200f
        val itemSize = 160f
        val itemPadding = (slotSize - itemSize) / 2f

        // Create 8 slots (2 rows of 4)
        for (i in 0 until 8) {
            val potion = potionInventory.getOrNull(i) // Get potion for this slot

            // Create the item slot background
            val itemSlotBg = Image(skin.getDrawable("item-slot"))

            val slotGroup = Group()
            slotGroup.addActor(itemSlotBg)
            itemSlotBg.setSize(slotSize, slotSize)

            if (potion != null) {
                // If a potion exists, add its image
                val potionImage = Image(potion.textureRegion)
                potionImage.setSize(itemSize, itemSize)
                slotGroup.addActor(potionImage)
                potionImage.setPosition(itemPadding, itemPadding)

                // Add click listener to use the item
                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        // Use the item
                        val usedPotion = potionInventory.removeAt(i) // Remove from stack
                        player.health += usedPotion.healAmount
                        sfxItemHeal.play(50f)
                        showToast("Healed for ${usedPotion.healAmount} health")

                        // Refresh the UI
                        updateItemsTable()
                    }
                })

                // Add tooltip
                val tooltip = TextTooltip("${potion.name}\n${potion.description}", itemTooltipStyle)
                slotGroup.addListener(tooltip)

            } else {
                // If no potion, add a "fail" click listener
                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        sfxItemFail.play(50f)
                        showToast("Slot is empty")
                    }
                })
            }

            // Add the slot to the grid
            itemsGrid.add(slotGroup).size(slotSize).pad(10f)

            if ((i + 1) % 4 == 0) {
                // New row every 4 slots
                itemsGrid.row()
            }
        }
    }

    private var accumulator = 0f

    override fun show() {
        updateFont()

        pauseUI.updateFont(font)
        GameLogic.gameState = GameLogic.GameState.BATTLE

        // --- NEW ---
        // Build the items table structure once
        buildItemsTable()
        // ---

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
                            combat.resolveDelay = enemySprite.attackDuration()
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
            onDefeat = { defeated, by ->
                if (defeated === enemy && by === player) {
                    val coins = 10
                    player.currency += coins
                    Gdx.app.log("REWARD", "+$coins Gold. Total: ${player.currency}")
                    // showVictoryPopup(xp = 10, money = coins)
                }
            },
            resolveDelay = 0f
        )

        pauseUI.updateFont(font)
        pauseUI.onResize()

        pauseUI.onSaveRequested = {
            try {
                val slotToSave = currentSession?.slotId ?: 1 // Default to 1 if session is somehow null
                // --- UPDATED ---
                // Save the *size* of the inventory
                SaveGame.save(player, enemy, potionInventory.size, slotToSave)
                Gdx.app.log("SAVE", "Game saved to slot $slotToSave")
                showToast("Game Saved (Slot $slotToSave)", 1.5f)
            } catch (t: Throwable) {
                Gdx.app.error("SAVE", "Failed to save", t)
                showToast("Save Failed", 1.5f)
            }
        }

        pauseUI.onMainMenuRequested = {
            game.setScreen<MainMenuScreen>()
        }

        // Enable input for UI
        Gdx.input.inputProcessor = uiStage
        uiStage.addActor(menuTable)
        uiStage.addActor(itemsTable) // Add the (initially empty) items table

        uiStage.addActor(playerHealthLabel)
        uiStage.addActor(enemyHealthLabel)
        GameLogic.screen = this
    }

    // TODO: Fix font turning into squares when reopening the application
    override fun resume() {
        updateFont() // Simplified
    }

    override fun resize(width: Int, height: Int) {
        updateFont() // Simplified

        game.worldViewport.update(width, height, true)
        game.worldViewport.camera.update()
        game.uiViewport.update(width, height, true)
        game.uiViewport.camera.update()

        //ui
        pauseUI.updateFont(this.font)
        pauseUI.onResize()
    }

    override fun render(delta: Float) {
        input(delta)
        logic(delta)
        draw(delta)
    }

    /**
     * TODO: Handles the user's input.
     */
    fun input(delta: Float) {

    }

    /**
     * TODO: Handles the game logic.
     */
    fun logic(delta: Float) {
        combat.update(Gdx.graphics.deltaTime)
        playerSprite.update(Gdx.graphics.deltaTime)
        enemySprite.update(Gdx.graphics.deltaTime)
        if (toastTimer > 0f) {
            toastTimer -= Gdx.graphics.deltaTime
            if (toastTimer <= 0f) toastText = null
        }
        if (enemy.health == 0 || player.health == 0) {
            accumulator += delta
            if (accumulator >= 2f && enemy.health == 0) {
                accumulator = 0f
                enemy.health = 20
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
                                    combat.resolveDelay = enemySprite.attackDuration()
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
                    onDefeat = { defeated, by ->
                        if (defeated === enemy && by === player) {
                            val coins = 10
                            player.currency += coins
                            Gdx.app.log("REWARD", "+$coins Gold. Total: ${player.currency}")
                            // showVictoryPopup(xp = 10, money = coins)
                        }
                    },
                    resolveDelay = 0f
                )
            }
        }
    }

    /**
     * Draws everything.
     */
    fun draw(delta: Float) {
        drawGame(delta)
        drawUI(delta)
    }

    /**
     * Draws the game.
     */
    fun drawGame(delta: Float) {
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
        }
    }

    /**
     * Draws the UI.
     */
    fun drawUI(delta: Float) {
        uiStage.act(Gdx.graphics.deltaTime)
        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        game.batch.use {
            // TODO: Use the font from the BattleScreen class
            pauseUI.drawAndHandleInput(game.batch)

            if (!pauseUI.isPaused) {
                menuTable.isVisible = !isShowingItems
                playerHealthLabel.isVisible = !isShowingItems
                enemyHealthLabel.isVisible = !isShowingItems

                // --- UPDATED: Only draw the table, don't position it ---
                itemsTable.isVisible = isShowingItems
                if (isShowingItems) {
                    // The table is already added to the stage and
                    // set to fill parent, so we just need to draw the stage.
                    // The `itemsTable.draw` call is handled by `stage.draw()`
                } else {
                    menuTable.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f * 0.1f)
                    menuTable.bottom()
                    menuTable.draw(game.batch, 1f)
                    playerHealthLabel.setText("${player.health}")
                    playerHealthLabel.setPosition(100f, Gdx.graphics.height - 400f)
                    playerHealthLabel.draw(game.batch, 1f)
                    enemyHealthLabel.setText("${enemy.health}")
                    enemyHealthLabel.setPosition(Gdx.graphics.width - 100f - enemyHealthLabel.width, Gdx.graphics.height - 400f)
                    enemyHealthLabel.draw(game.batch, 1f)
                }
            }

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

        // --- NEW: Draw the stage AFTER the batch ---
        // This makes the UI (tooltips, tables) draw correctly.
        if (!pauseUI.isPaused && isShowingItems) {
            uiStage.draw()
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

    // --- NEW: Helper function to create a 1x1 pixmap texture ---
    private fun onePixel(color: Color): TextureRegion {
        val pm = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        return TextureRegion(t)
    }

    // --- NEW: Reusable updateFont function ---
    private fun updateFont() {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f

        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            if (size <= 0) size = 15 // Fix for size being 0
            println(size)
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        // Dispose the old font to prevent memory leaks
        if (font.data.fontFile != null) font.dispose()

        font = generator.generateFont(parameter)
        font.color = Color.BLACK

        if (this::tooltipLabelStyle.isInitialized) {
            tooltipLabelStyle.font = font
        } else {
            // If this is the first time, create the style
            tooltipLabelStyle = Label.LabelStyle(font, Color.WHITE)
        }
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

        // --- NEW: Dispose new sounds and texture ---
        sfxItemHeal.dispose()
        sfxItemFail.dispose()
        potionsSpritesheet.dispose()
    }
}
