package com.anotherround.Screens

import com.anotherround.CharacterClasses.*
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.math.Interpolation
import com.anotherround.combat.CombatManager
import com.anotherround.combat.Action
import com.anotherround.combat.SfxEvent
import com.anotherround.Consumables.ConsumablesInventory
import com.anotherround.Consumables.Potion
import com.anotherround.Consumables.PotionRarity
import com.anotherround.Consumables.HealthPotion
import com.anotherround.Consumables.DefensePotion
import com.anotherround.Consumables.FirePotion
import com.anotherround.combat.StatusEffect
import com.anotherround.GameLogic
import com.anotherround.GameSession
import com.anotherround.Main
import com.anotherround.MainMenuScreen
import com.anotherround.SaveLoad.GameState
import com.anotherround.SaveLoad.SaveGame
import com.anotherround.SaveLoad.PotionData
import com.anotherround.SaveLoad.ArmorData
import com.anotherround.SaveLoad.WeaponData
import com.anotherround.SaveLoad.CharacterSnapshot
import com.anotherround.Equipment.DEFAULT_ARMOR_BLUEPRINTS
import com.anotherround.Equipment.DEFAULT_WEAPON_BLUEPRINTS
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
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.anotherround.Equipment.ArmorPiece
import com.anotherround.Equipment.ArmorSlot
import com.anotherround.Equipment.EquipmentSlots
import com.anotherround.Equipment.buildDefaultArmor
import com.anotherround.Equipment.Weapon
import com.anotherround.Equipment.buildDefaultWeapons

class BattleScreen(val game: Main) : KtxScreen {

    var currentSession: GameSession? = null

    private val inventory = ConsumablesInventory()

    // equipment data
    private lateinit var armorTexture: Texture
    private lateinit var itemIcons: Array<Array<TextureRegion>>
    private val armorInventory = mutableListOf<ArmorPiece>()
    private val weaponInventory = mutableListOf<Weapon>()
    private val equipmentSlots = EquipmentSlots()

    private fun getColorForRarity(rarity: String): Color {
        return when (rarity) {
            "Common" -> Color.WHITE
            "Uncommon" -> Color.GREEN
            "Rare" -> Color.CYAN
            "Epic" -> Color.PURPLE
            "Legendary" -> Color.GOLD
            else -> Color.WHITE
        }
    }

    // round number (starts at 0, increments each time an enemy is defeated)
    private var roundNumber = 0

    // player info
    private val player = Player(name = "Hero")
    private lateinit var playerLevelLabel: Label
    private lateinit var enemyLevelLabel: Label
    // current enemy & its type
    private lateinit var enemy: Character
    private lateinit var enemyKind: EnemyKind

    // sprites
    private lateinit var playerSprite: PlayerSprite
    private lateinit var enemySprite: EnemySprite

    // combat manager
    private lateinit var combat: CombatManager

    // when true, we wait a bit then spawn a new random enemy
    private var pendingNextEnemy = false
    private var nextEnemyDelay = 0f

    // ui for the "Game Over"
    private lateinit var gameOverTable: Table
    private var pendingGameOver = false
    private var gameOverDelay = 0f
    private var isGameOver = false

    fun startNewGame(session: GameSession) {
        this.currentSession = session
        Gdx.app.log("BattleScreen", "Starting new game for ${session.playerName} in slot ${session.slotId}")

        isGameOver = false
        pendingGameOver = false
        gameOverDelay = 0f
        pendingNextEnemy = false
        nextEnemyDelay = 0f
        isShowingItems = false
        toastText = null

        if (this::gameOverTable.isInitialized) {
            gameOverTable.isVisible = false
        }
        val basePlayer = Player(name = session.playerName)
        player.name = basePlayer.name
        player.level = basePlayer.level
        player.health = basePlayer.health
        player.defenseStat = basePlayer.defenseStat
        player.attackStat = basePlayer.attackStat
        player.currency = basePlayer.currency

        inventory.loadDefaultPotions()

        // Clear equipment for new game
        armorInventory.clear()
        weaponInventory.clear()
        equipmentSlots.helmet = null
        equipmentSlots.chest = null
        equipmentSlots.boots = null
        equipmentSlots.weapon = null
        recalculateStats()

        roundNumber = 0

        if (this::playerSprite.isInitialized) {
            playerSprite.revive()
        }
        if (this::enemySprite.isInitialized) {
            enemySprite.dispose()
        }
        spawnRandomEnemy()
        showToast("New Game: ${session.playerName}!", 1.5f)
    }

    fun loadSavedGame(state: GameState, slot: Int) {
        this.currentSession = GameSession(slot, state.player.name)
        Gdx.app.log("BattleScreen", "Loading game for ${state.player.name} from slot $slot")

        isGameOver = false
        pendingGameOver = false
        gameOverDelay = 0f
        if (this::gameOverTable.isInitialized) {
            gameOverTable.isVisible = false
        }

        player.name = state.player.name
        player.health = state.player.health
        player.maxHealth = 40 // Default, will be recalculated
        player.level = state.player.level
        player.defenseStat = state.player.defenseStat
        player.attackStat = state.player.attackStat
        player.currency = state.player.currency

        player.currentXp = state.player.currentXp
        player.xpToNextLevel = if (state.player.xpToNextLevel > 0) {
            state.player.xpToNextLevel
        } else {
            player.xpRequiredFor(player.level)
        }
        // Ensure sprites are loaded
        initEquipmentSprites()

        // Potions
        inventory.loadFromSaveState(state.potions)

        // Round
        roundNumber = state.roundNumber

        // Restore Inventory
        armorInventory.clear()
        state.inventoryArmor.forEach { restoreArmor(it)?.let { item -> armorInventory.add(item) } }

        weaponInventory.clear()
        state.inventoryWeapons.forEach { restoreWeapon(it)?.let { item -> weaponInventory.add(item) } }

        // Restore Equipped
        equipmentSlots.helmet = state.equippedHelmet?.let { restoreArmor(it) }
        equipmentSlots.chest = state.equippedChest?.let { restoreArmor(it) }
        equipmentSlots.boots = state.equippedBoots?.let { restoreArmor(it) }
        equipmentSlots.weapon = state.equippedWeapon?.let { restoreWeapon(it) }

        recalculateStats()

        // Restore Enemy
        try {
            val kindName = state.enemyKind
            Gdx.app.log("LOAD", "Attempting to restore enemy: '$kindName' with Health: ${state.enemy.health}")

            enemyKind = EnemyKind.valueOf(kindName)
            enemy = EnemyFactory.create(enemyKind)

            // Apply saved stats
            if (state.enemy.health > 0) {
                enemy.health = state.enemy.health
            } else {
                 // If saved enemy is dead, we probably want to spawn a new one or handle it.
                 // For now, let's just respect the save, but ensure it doesn't break logic.
                 Gdx.app.log("LOAD", "Saved enemy was dead (0 HP). Spawning new random enemy.")
                 spawnRandomEnemy()
                 return
            }

            // Re-spawn sprite
            if (this::enemySprite.isInitialized) enemySprite.dispose()
            enemySprite = EnemySprite(game.worldViewport, enemyKind)

            Gdx.app.log("LOAD", "Enemy restored successfully: ${enemy.name} (${enemy.health} HP)")

        } catch (e: Exception) {
            Gdx.app.error("LOAD", "Failed to restore enemy kind '${state.enemyKind}', spawning random", e)
            spawnRandomEnemy()
        }

        setupCombat()

        if (this::playerLevelLabel.isInitialized) {
            playerLevelLabel.setText("Lvl ${player.level}")
        }

        if (this::enemyLevelLabel.isInitialized) {
             enemyLevelLabel.setText("Lvl ${enemy.level}")

             if (this::enemySprite.isInitialized) {
                 val topY = enemySprite.y + enemySprite.cfg.drawHeight
                 val screenPos = game.worldViewport.project(com.badlogic.gdx.math.Vector3(enemySprite.x + 0.5f, topY + 0.5f, 0f))
                 enemyLevelLabel.setPosition(screenPos.x - enemyLevelLabel.prefWidth / 2, screenPos.y)
             }
        }

        showToast("Loaded Game: round $roundNumber")
    }



    private var toastText: String? = null
    private var toastTimer = 0f
    private val toastLayout by lazy { com.badlogic.gdx.graphics.g2d.GlyphLayout() }
    private fun showToast(text: String, seconds: Float = 1.5f) {
        toastText = text
        toastTimer = seconds
    }

    private lateinit var backgroundMusic: Music
    private lateinit var attackSound: Sound
    private lateinit var sfxPlayerAttack: Sound
    private lateinit var sfxEnemyAttack: Sound
    private lateinit var sfxPlayerHurt: Sound
    private lateinit var sfxEnemyHurt: Sound
    private lateinit var sfxEnemyDeath: Sound
    private lateinit var sfxItemHeal: Sound
    private lateinit var sfxItemFail: Sound
    private lateinit var sfxLevelUp: Sound

    private val worldStage = Stage(game.worldViewport)
    private val uiStage = Stage(game.uiViewport)
    private val pauseUI by lazy { PauseScreenUI(game.uiViewport) }

    var font = BitmapFont()
    private var smallFont = BitmapFont()
    val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    private val skin by lazy {
        Skin(Gdx.files.internal("atlas/ui.json"))
    }

    private val buttonStyle by lazy {
        TextButton.TextButtonStyle().apply {
            font = this@BattleScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }
    }

    private val tiledMap by lazy {
        val mapLoader = TmxMapLoader()
        mapLoader.load("world/test.tmx")
    }
    private val tiledMapCamera = OrthographicCamera()
    private val tiledMapRenderer = OrthogonalTiledMapRenderer(tiledMap, Main.UNIT_SCALE)

    private val playerIcon = Image(Texture(Gdx.files.internal("ui/playerIcon.png")))
    private val playerIcon_NotTurn = Image(Texture(Gdx.files.internal("ui/playerIcon_noTurn.png")))

    private val enemyIcon = Image(Texture(Gdx.files.internal("ui/enemyIcon.png")))
    private val enemyIcon_NotTurn = Image(Texture(Gdx.files.internal("ui/enemyIcon_noTurn.png")))

    private val playerHealthLabel by lazy {
        val label = TextButton("${player.health}", buttonStyle)
        label.width = 700f
        label.height = 200f
        label
    }

    private val enemyHealthLabel by lazy {
        val label = TextButton("", buttonStyle)
        label.width =700f
        label.height = 200f
        label
    }

    lateinit var attackButton: TextButton
    lateinit var itemsButton: TextButton
    lateinit var equipmentButton: TextButton

    // Level Labels


    private val menuTable by lazy {
        val table = Table()
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

        val itemsButton = TextButton("Items", buttonStyle)
        this.itemsButton = itemsButton
        itemsButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (combat.canOpenMenu() && !isShowingItems && !isShowingEquipment) {
                    isShowingItems = true
                    isShowingEquipment = false
                    updateItemsTable()
                }
            }
        })
        table.add(itemsButton).pad(10f).width(400f).height(200f)
        table.row()

        val equipmentButton = TextButton("Equipment", buttonStyle)
        this.equipmentButton = equipmentButton

        equipmentButton.label.setFontScale(0.8f)

        equipmentButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (combat.canOpenMenu() && !isShowingEquipment && !isShowingItems) {
                    isShowingEquipment = true
                    isShowingItems = false
                    updateEquipmentTable()
                }
            }
        })
        table.add(equipmentButton).padTop(0f).width(400f).height(200f)

        table
    }

    private var isShowingItems = false
    private var isShowingEquipment = false

    private lateinit var coinsLabel: TextButton

    private lateinit var itemsTable: Table
    private lateinit var itemsListTable: Table
    private lateinit var nameLabelStyle: Label.LabelStyle
    private lateinit var descLabelStyle: Label.LabelStyle

    // equipment UI tables
    private lateinit var equipmentTable: Table
    private lateinit var equipmentEquippedTable: Table
    private lateinit var equipmentInventoryTable: Table

    // popup overlay for item stats
    private lateinit var equipmentPopupContainer: Table
    private lateinit var equipmentPopupNameLabel: Label
    private lateinit var equipmentPopupLabel: Label
    private lateinit var equipmentEquipButton: TextButton
    private var selectedEquipmentIndex: Int? = null

    // round label
    private lateinit var roundLabel: Label

    private fun buildItemsTable() {
        nameLabelStyle = Label.LabelStyle(font, Color.WHITE)
        descLabelStyle = Label.LabelStyle(smallFont, Color.LIGHT_GRAY)

        sfxItemHeal  = Gdx.audio.newSound(Gdx.files.internal("audio/item-use.mp3"))
        sfxItemFail  = Gdx.audio.newSound(Gdx.files.internal("audio/item-fail.mp3"))

        itemsTable = Table()
        itemsTable.setFillParent(true)
        itemsTable.background(TextureRegionDrawable(onePixel(Color(0f, 0f, 0f, 0.7f))))
        itemsTable.center()

        val innerTable = Table()
        itemsTable.add(innerTable)

        itemsListTable = Table()
        innerTable.add(itemsListTable).pad(20f)
        innerTable.row()

        val backButton = TextButton("Return", buttonStyle)
        backButton.addListener(object: ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y:Float) {
                if (isShowingItems) {
                    isShowingItems = false
                }
            }
        })

        innerTable.add(backButton).width(400f).height(200f).padTop(50f)

        val coinsLabel = TextButton("$${player.currency}", buttonStyle)
        this.coinsLabel = coinsLabel

        innerTable.add(coinsLabel).width(200f).height(200f).padTop(50f)
    }

    private fun updateItemsTable() {
        itemsListTable.clear()

        val slotSize = 200f
        val itemSize = 160f
        val itemPadding = (slotSize - itemSize) / 2f

        for (i in 0 until 8) {
            val consumable = inventory.getItems().getOrNull(i)

            val itemSlotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(itemSlotBg)
            slotGroup.clearListeners()
            itemSlotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (consumable != null) {
                val itemImage = Image(consumable.textureRegion)
                itemImage.setSize(itemSize, itemSize)
                slotGroup.addActor(itemImage)
                itemImage.setPosition(itemPadding, itemPadding)

                nameLabel = Label(consumable.name, nameLabelStyle)
                nameLabel.setWrap(false)
                when (consumable.rarity) {
                    PotionRarity.COMMON -> nameLabel.color = Color.WHITE
                    PotionRarity.RARE -> nameLabel.color = Color.CYAN
                    PotionRarity.EPIC -> nameLabel.color = Color.PURPLE
                }
                descLabel = Label(consumable.description, descLabelStyle)
                descLabel.setWrap(true)

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        when (consumable) {
                            is HealthPotion -> {
                                player.heal(consumable.healAmount)
                                showHealPopup(player, consumable.healAmount)
                                playerHealthLabel.setText("${player.health}")
                                showToast("Healed for ${consumable.healAmount} HP")
                            }
                            is DefensePotion -> {
                                combat.addEffect(player, StatusEffect.DefenseBuff(consumable.blockPercent))
                                showToast("Defense Applied!")
                            }
                            is FirePotion -> {
                                combat.addEffect(enemy, StatusEffect.Burn(consumable.damagePerRound, consumable.durationRounds))
                                enemy.takeTrueDamage(consumable.damagePerRound) // Instant damage (True Damage)
                                showDamagePopup(enemy, consumable.damagePerRound)
                                enemyHealthLabel.setText("${enemy.health}")
                                showToast("Fire! ${consumable.damagePerRound} damage!")
                            }
                        }
                        inventory.useItem(consumable)
                        sfxItemHeal.play(0.9f)
                        updateItemsTable()
                    }
                })

            } else {
                nameLabel = Label("", nameLabelStyle)
                descLabel = Label("", descLabelStyle)

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        sfxItemFail.play(0.9f)
                        showToast("Slot is empty")
                    }
                })
            }

            val textTable = Table()
            textTable.add(nameLabel).left().padBottom(10f)
            textTable.row()
            textTable.add(descLabel).left().expandX().fillX()

            itemsListTable.add(slotGroup).size(slotSize).pad(10f)
            itemsListTable.add(textTable).width(Gdx.graphics.width * 0.5f).padLeft(50f)
            itemsListTable.row()
        }

        coinsLabel.setText("$${player.currency}")
    }

    private fun updateShopTable() {
        itemsListTable.clear()

        val slotSize = 200f
        val itemSize = 160f
        val itemPadding = (slotSize - itemSize) / 2f

        // make 8 slots
        for (i in 0 until 8) {
            val consumable = inventory.getItems().getOrNull(i) // Get item for this slot

            // slot bg
            val itemSlotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(itemSlotBg)
            itemSlotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (consumable != null) {
                // item icon
                val itemImage = Image(consumable.textureRegion)
                itemImage.setSize(itemSize, itemSize)
                slotGroup.addActor(itemImage)
                itemImage.setPosition(itemPadding, itemPadding)

                // Set the text
                nameLabel = Label(consumable.name, nameLabelStyle)
                when (consumable.rarity) {
                    PotionRarity.COMMON -> nameLabel.color = Color.WHITE
                    PotionRarity.RARE -> nameLabel.color = Color.CYAN
                    PotionRarity.EPIC -> nameLabel.color = Color.PURPLE
                }
                descLabel = Label(consumable.description, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                nameLabel = Label("Health Potion", nameLabelStyle)
                descLabel = Label("Tap to buy for 10 gold", descLabelStyle)
                slotGroup.clearListeners()

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        if (player.currency >= 10) {
                            player.currency -= 10
                            inventory.addItem(
                                inventory.createHealthPotion()
                            )
                            updateShopTable()
                        } else {
                            showToast("Not enough coins")
                        }
                    }
                })
            }

            //  table for text and title
            val textTable = Table()
            textTable.add(nameLabel).left().padBottom(10f)
            textTable.row()
            textTable.add(descLabel).left().expandX().fillX()

            itemsListTable.add(slotGroup).size(slotSize).pad(10f)
            itemsListTable.add(textTable).width(Gdx.graphics.width * 0.5f).padLeft(50f) // Give text 50% of screen width
            itemsListTable.row()
        }

        coinsLabel.setText("$${player.currency}")
    }
    private fun buildGameOverTable() {
        gameOverTable = Table().apply {
            setFillParent(true)
            background(TextureRegionDrawable(onePixel(Color(0f, 0f, 0f, 0.7f))))
            isVisible = false
        }

        val label = Label("GAME OVER", nameLabelStyle)
        label.setFontScale(2f)
        gameOverTable.add(label).padBottom(50f).row()

        val menuButton = TextButton("Main Menu", buttonStyle)
        menuButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.setScreen<MainMenuScreen>()
            }
        })
        gameOverTable.add(menuButton).width(700f).height(300f)
    }

    /** Load armor and weapon icons + stats from the 64x64 sprite sheet. */
    private fun initEquipmentSprites() {
        if (this::armorTexture.isInitialized) return

        armorTexture = Texture(Gdx.files.internal("items/64x64.png"))
        armorTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

        itemIcons = TextureRegion.split(armorTexture, 64, 64)
    }

    private fun recalculateStats() {
        var newMaxHp = 40
        var newDef = 0
        var newAtk = 20

        val lvl = player.level.coerceAtLeast(1)
        newMaxHp += (lvl - 1) * 5
        newAtk += (lvl - 1) * 2
        newDef += (lvl - 1) * 1

        equipmentSlots.helmet?.let {
            newMaxHp += it.health
            newDef += it.defense
        }
        equipmentSlots.chest?.let {
            newMaxHp += it.health
            newDef += it.defense
        }
        equipmentSlots.boots?.let {
            newMaxHp += it.health
            newDef += it.defense
        }
        equipmentSlots.weapon?.let {
            newAtk += it.attack
        }

        player.maxHealth = newMaxHp
        player.defenseStat = newDef
        player.attackStat = newAtk

        if (player.health > player.maxHealth) {
            player.health = player.maxHealth
        }
    }

    /** Builds the equipment overlay window. */
    private fun buildEquipmentTable() {
        if (!this::nameLabelStyle.isInitialized) {
            nameLabelStyle = Label.LabelStyle(font, Color.WHITE)
        }
        if (!this::descLabelStyle.isInitialized) {
            descLabelStyle = Label.LabelStyle(smallFont, Color.LIGHT_GRAY)
        }

        equipmentTable = Table()
        equipmentTable.setFillParent(true)
        equipmentTable.background(TextureRegionDrawable(onePixel(Color(0f, 0f, 0f, 0.7f))))
        equipmentTable.center()

        val innerTable = Table()
        innerTable.defaults().pad(10f)
        equipmentTable.add(innerTable).expand().center()

        val titleLabel = Label("Equipment", nameLabelStyle)
        innerTable.add(titleLabel).padBottom(20f)
        innerTable.row()

        equipmentEquippedTable = Table()
        equipmentEquippedTable.defaults().pad(8f)
        innerTable.add(equipmentEquippedTable).padBottom(25f)
        innerTable.row()

        val inventoryLabel = Label("Inventory", nameLabelStyle)
        innerTable.add(inventoryLabel).padBottom(10f)
        innerTable.row()

        equipmentInventoryTable = Table()
        equipmentInventoryTable.defaults().pad(10f)
        innerTable.add(equipmentInventoryTable).padBottom(20f)
        innerTable.row()

        val backButton = TextButton("Return", buttonStyle)
        backButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                if (isShowingEquipment) {
                    isShowingEquipment = false
                }
            }
        })
        innerTable.add(backButton).width(400f).height(200f).padTop(5f)

        // popup overlay for item stats
        equipmentPopupContainer = Table()
        equipmentPopupContainer.setFillParent(true)
        equipmentPopupContainer.isVisible = false

        equipmentPopupContainer.top()

        val popupInner = Table()
        popupInner.background = skin.getDrawable("button-normal")
        popupInner.pad(40f)

        val popupNameStyle = Label.LabelStyle(nameLabelStyle.font, Color.BLACK)
        val popupStatsStyle = Label.LabelStyle(descLabelStyle.font, Color.BLACK)

        equipmentPopupNameLabel = Label("", popupNameStyle).apply {
            setFontScale(0.9f)
        }

        // description
        equipmentPopupLabel = Label("", popupStatsStyle).apply {
            setWrap(true)
            setFontScale(0.9f)
        }


        // Equip button
        equipmentEquipButton = TextButton("Equip", buttonStyle)
        equipmentEquipButton.label.setFontScale(0.8f) // slightly smaller text
        equipmentEquipButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                selectedEquipmentIndex?.let { index ->
                    onEquipItem(index)
                }
            }
        })

        // Discard button
        val discardButton = TextButton("Discard", buttonStyle)
        discardButton.label.setFontScale(0.8f)
        discardButton.label.color = Color.RED
        discardButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                selectedEquipmentIndex?.let { index ->
                    onDiscardItem(index)
                }
            }
        })

        // item name on top and then stats under it
        popupInner.add(equipmentPopupNameLabel)
            .padBottom(8f)
            .padLeft(25f)
            .left()
            .row()

        popupInner.add(equipmentPopupLabel)
            .width(Gdx.graphics.width * 0.6f)
            .padLeft(25f)
            .left()
            .row()

        // Buttons row
        val buttonTable = Table()
        buttonTable.add(equipmentEquipButton).width(250f).height(100f).padRight(20f)
        buttonTable.add(discardButton).width(250f).height(100f)

        popupInner.add(buttonTable)
            .padTop(20f)
            .center()

        equipmentPopupContainer.add(popupInner)
            .expandX()
            .padTop(100f)
            .center()

        equipmentTable.addActor(equipmentPopupContainer)
    }

    /**
     * Replaces the logic for equipping item from existing inventory list.
     */
    private fun onEquipItem(index: Int) {
        val combined = mutableListOf<Any>()
        combined.addAll(armorInventory)
        combined.addAll(weaponInventory)

        val itemToEquip = combined.getOrNull(index) ?: return

        // Logic:
        // 1. Remove from inventory
        // 2. Unequip old item (move to inventory)
        // 3. Equip new item

        if (itemToEquip is ArmorPiece) {
            val oldItem = when (itemToEquip.slot) {
                ArmorSlot.HELMET -> equipmentSlots.helmet
                ArmorSlot.CHEST -> equipmentSlots.chest
                ArmorSlot.BOOTS -> equipmentSlots.boots
            }

            // Remove from inventory
            armorInventory.remove(itemToEquip)

            // Add old item back
            oldItem?.let { armorInventory.add(it) }

            // Set new item
            when (itemToEquip.slot) {
                ArmorSlot.HELMET -> equipmentSlots.helmet = itemToEquip
                ArmorSlot.CHEST -> equipmentSlots.chest = itemToEquip
                ArmorSlot.BOOTS -> equipmentSlots.boots = itemToEquip
            }

        } else if (itemToEquip is Weapon) {
            val oldItem = equipmentSlots.weapon

            // Remove from inventory
            weaponInventory.remove(itemToEquip)

            // Add old
            oldItem?.let { weaponInventory.add(it) }

            // Set new
            equipmentSlots.weapon = itemToEquip
        }

        recalculateStats()
        hideEquipmentPopup()
        updateEquipmentTable()
        playerHealthLabel.setText("${player.health}/${player.maxHealth}")
        showToast("Equipped: ${if (itemToEquip is ArmorPiece) itemToEquip.name else (itemToEquip as Weapon).name}")
    }

    private fun onDiscardItem(index: Int) {
        val combined = mutableListOf<Any>()
        combined.addAll(armorInventory)
        combined.addAll(weaponInventory)

        val item = combined.getOrNull(index) ?: return

        if (item is ArmorPiece) {
            armorInventory.remove(item)
            showToast("Discarded ${item.name}")
        } else if (item is Weapon) {
            weaponInventory.remove(item)
            showToast("Discarded ${item.name}")
        }

        hideEquipmentPopup()
        updateEquipmentTable()
    }

    private fun showEquipmentPopup(itemName: String, statsText: String, index: Int, nameColor: Color = Color.BLACK) {
        selectedEquipmentIndex = index
        equipmentPopupNameLabel.setText(itemName)
        equipmentPopupNameLabel.color = nameColor
        equipmentPopupLabel.setText(statsText)
        equipmentPopupContainer.isVisible = true
    }

    private fun hideEquipmentPopup() {
        selectedEquipmentIndex = null
        if (this::equipmentPopupContainer.isInitialized) {
            equipmentPopupContainer.isVisible = false
        }
    }

    /** Top: Weapon + 3 armor slots. Bottom: 12 inventory icons (4x3), armor and weapons mixed. */
    private fun updateEquipmentTable() {
        equipmentEquippedTable.clearChildren()
        equipmentInventoryTable.clearChildren()

        val slotSize = 200f
        val itemSize = 160f
        val itemPadding = (slotSize - itemSize) / 2f

        // ------------ EQUIPPED SECTION ------------

        fun addEquippedArmorRow(piece: ArmorPiece?) {
            val slotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(slotBg)
            slotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (piece != null) {
                val icon = Image(piece.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                slotGroup.addActor(icon)

                nameLabel = Label(piece.name, nameLabelStyle)
                nameLabel.setFontScale(0.75f)
                nameLabel.color = getColorForRarity(piece.rarity)

                val descText = "Rarity: ${piece.rarity}\nDEF: ${piece.defense}   HP: ${piece.health}"
                descLabel = Label(descText, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                nameLabel = Label("None", nameLabelStyle)
                nameLabel.setFontScale(0.75f)
                nameLabel.color = Color.WHITE
                descLabel = Label("", descLabelStyle)
            }

            val textTable = Table()
            textTable.add(nameLabel).left().padBottom(4f).width(Gdx.graphics.width * 0.5f)
            textTable.row()
            textTable.add(descLabel).left().width(Gdx.graphics.width * 0.5f)

            equipmentEquippedTable.add(slotGroup).size(slotSize).pad(5f)
            equipmentEquippedTable.add(textTable).padLeft(20f)
            equipmentEquippedTable.row()
        }

        fun addEquippedWeaponRow(weapon: Weapon?) {
            val slotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(slotBg)
            slotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (weapon != null) {
                val icon = Image(weapon.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                slotGroup.addActor(icon)

                nameLabel = Label(weapon.name, nameLabelStyle)
                nameLabel.setFontScale(0.75f)
                nameLabel.color = getColorForRarity(weapon.rarity)

                val descText = "Rarity: ${weapon.rarity}\nATK: ${weapon.attack}"
                descLabel = Label(descText, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                nameLabel = Label("None", nameLabelStyle)
                nameLabel.setFontScale(0.75f)
                nameLabel.color = Color.WHITE
                descLabel = Label("", descLabelStyle)
            }

            val textTable = Table()
            textTable.add(nameLabel).left().padBottom(4f).width(Gdx.graphics.width * 0.5f)
            textTable.row()
            textTable.add(descLabel).left().width(Gdx.graphics.width * 0.5f)

            equipmentEquippedTable.add(slotGroup).size(slotSize).pad(5f)
            equipmentEquippedTable.add(textTable).padLeft(20f)
            equipmentEquippedTable.row()
        }

        addEquippedWeaponRow(equipmentSlots.weapon)
        addEquippedArmorRow(equipmentSlots.helmet)
        addEquippedArmorRow(equipmentSlots.chest)
        addEquippedArmorRow(equipmentSlots.boots)

        // ------------ INVENTORY SECTION (12 slots, armor + weapons mixed) ------------

        val combined = mutableListOf<Any>()
        combined.addAll(armorInventory)
        combined.addAll(weaponInventory)

        val maxSlots = 12
        val itemsPerRow = 4
        var col = 0

        for (i in 0 until maxSlots) {
            val item = combined.getOrNull(i)

            val slotBg = Image(skin.getDrawable("item-slot"))
            val group = Group()
            group.addActor(slotBg)
            slotBg.setSize(slotSize, slotSize)

            // highlight currently selected item
            if (selectedEquipmentIndex == i) {
                slotBg.color = Color(1f, 1f, 0.7f, 1f) // soft yellow tint
            }

            if (item is ArmorPiece) {
                val icon = Image(item.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                group.addActor(icon)

                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val name = item.name
                        val stats = "Rarity: ${item.rarity}\nDEF: ${item.defense}   HP: ${item.health}"
                        val color = getColorForRarity(item.rarity)

                        if (selectedEquipmentIndex == i && equipmentPopupContainer.isVisible) {
                            hideEquipmentPopup()
                        } else {
                            showEquipmentPopup(name, stats, i, color)
                        }
                        // refresh highlight
                        updateEquipmentTable()
                    }
                })
            } else if (item is Weapon) {
                val icon = Image(item.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                group.addActor(icon)

                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val name = item.name
                        val stats = "Rarity: ${item.rarity}\nATK: ${item.attack}"
                        val color = getColorForRarity(item.rarity)

                        if (selectedEquipmentIndex == i && equipmentPopupContainer.isVisible) {
                            hideEquipmentPopup()
                        } else {
                            showEquipmentPopup(name, stats, i, color)
                        }
                        // refresh highlight
                        updateEquipmentTable()
                    }
                })
            } else {
                // empty slot
                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        hideEquipmentPopup()
                        updateEquipmentTable()
                    }
                })
            }

            equipmentInventoryTable.add(group).size(slotSize).pad(5f)

            col++
            if (col >= itemsPerRow) {
                equipmentInventoryTable.row()
                col = 0
            }
        }
    }

    private var accumulator = 0f

    private fun updateFont() {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f

        var parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            if (size <= 0) size = 15
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        if (font.data.fontFile != null) font.dispose()

        font = generator.generateFont(parameter)
        font.color = Color.BLACK

        parameter = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * 0.04f * textToButtonHeight).toInt()
            if (size <= 0) size = 12
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }

        if (smallFont.data.fontFile != null) smallFont.dispose()
        smallFont = generator.generateFont(parameter)
        smallFont.color = Color.LIGHT_GRAY

        if (this::nameLabelStyle.isInitialized) {
            nameLabelStyle.font = font
        } else {
            nameLabelStyle = Label.LabelStyle(font, Color.WHITE)
        }

        if (this::descLabelStyle.isInitialized) {
            descLabelStyle.font = smallFont
        } else {
            descLabelStyle = Label.LabelStyle(smallFont, Color.LIGHT_GRAY)
        }
    }

    private fun scheduleNextEnemy(delaySeconds: Float = 2f) {
        pendingNextEnemy = true
        nextEnemyDelay = delaySeconds
    }

    private fun scheduleGameOver(delaySeconds: Float = 2f) {
        pendingGameOver = true
        gameOverDelay = delaySeconds
    }

    private fun showGameOverPopup() {
        isGameOver = true
        gameOverTable.isVisible = true

        // Delete save file if session is active
        currentSession?.let {
            Gdx.app.log("GAME_OVER", "Deleting save for slot ${it.slotId}")
            SaveGame.delete(it.slotId)
        }
    }


    private fun spawnRandomEnemy() {
        enemyKind = EnemyFactory.randomKind()
        enemy = EnemyFactory.create(enemyKind)

        // --- BALANCED SCALING ALGORITHM ---
        // Scale enemy stats to match Player growth formula:
        // HP: 10 + (Lvl * 2)
        // ATK: 2 + (Lvl / 2)
        // DEF: 1 + (Lvl / 3)
        // Enemies start at Lvl 1 equivalent (Round 1).

        // Simulating level growth based on rounds.
        // Round 1 = 0 extra levels. Round 2 = 1 extra level.
        // CAP: Enemy level should never exceed Player level.
        // Since Enemy starts at Level 1, max added levels = Player.Level - 1.

        val potentialLevels = roundNumber.coerceAtLeast(1) - 1
        val maxLevels = (player.level - 1).coerceAtLeast(0)

        val levelsGained = potentialLevels.coerceAtMost(maxLevels)

        var addedHp = 0
        var addedAtk = 0
        var addedDef = 0

        for (i in 1..levelsGained) {
             // Use "current level" i for scaling calc if needed, or just linear cumulative
             // Player gain is constant per level up:
             // HP: +10 + (level*2) <- This is cumulative? No, Player.kt applyLevelUpStatGains uses 'level'
             // Player.kt: hpGain = 10 + level * 2.
             // Variable growth! Higher levels give MORE stats per level.
             // We need to simulate the level up loop.

             // Current simulated level for the enemy
             val simLevel = enemy.level + i

             // Heavily scaled down to make enemies easier
             addedHp += (6 + (simLevel * 1.2)).toInt()
             addedAtk += 1 + (simLevel / 4)
             addedDef += 1 + (simLevel / 5)
        }

        enemy.maxHealth += addedHp
        enemy.health = enemy.maxHealth // Heal to full
        enemy.attackStat += addedAtk
        enemy.defenseStat += addedDef

        // Update visual level
        enemy.level += levelsGained

        if (this::enemySprite.isInitialized) {
            enemySprite.dispose()
        }
        enemySprite = EnemySprite(game.worldViewport, enemyKind)

        setupCombat()

        Gdx.app.log("ENEMY", "Spawned ${enemy.name} (Lvl ${enemy.level}) - HP:${enemy.maxHealth} ATK:${enemy.attackStat} DEF:${enemy.defenseStat}")
        saveGame()
    }

    private fun setupCombat() {
        combat = CombatManager(
            player = player,
            enemy  = enemy,
            onLog  = { msg -> Gdx.app.log("COMBAT", msg) },

            onActionStart = { action ->
                when (action) {
                    is Action.Attack -> {
                        if (action.attacker === player) {
                            playerSprite.playAttack()
                            combat.resolveDelay = playerSprite.attackDuration()
                        } else if (action.attacker === enemy) {
                            enemySprite.playAttack()
                            combat.resolveDelay = enemySprite.attackDuration()
                        }
                    }
                    is Action.Defend -> {
                         combat.resolveDelay = 1.5f
                         showToast("${action.target.name} Braces!", 1.5f)
                    }
                    is Action.ApplyBurn -> {
                         combat.resolveDelay = 1.5f
                         showToast("${action.applier.name} casts Burn!", 1.5f)
                    }
                    else -> { /***/ }
                }
            },

            onActionEnd = { action ->

                // Determine who just acted
                val attacker = when(action) {
                    is Action.Attack -> action.attacker
                    is Action.ApplyBurn -> action.applier
                    is Action.Defend -> action.target // Self-target
                }

                // 1. Play hurt animations/Death logic ONLY on Attack
                if (action is Action.Attack) {
                    if (action.attacker === player) {
                        if (enemy.isAlive()) {
                            enemySprite.playHurt()
                            combat.pauseNextTurnFor(max(1.5f, enemySprite.hurtDuration()))
                        } else {
                            enemySprite.playDeath()
                            combat.pauseNextTurnFor(enemySprite.deathDuration())
                        }
                    } else if (action.attacker === enemy) {
                        playerSprite.playHurt()
                        combat.pauseNextTurnFor(max(1.5f, playerSprite.hurtDuration()))
                    }
                }

                // 2. SWAP Turn Icons (Using attacker to determine who finished turn)
                if (attacker === player) {
                     // Player finished acting -> Enemy Turn
                     playerIcon.remove()
                     enemyIcon_NotTurn.remove()
                     uiStage.addActor(playerIcon_NotTurn)
                     uiStage.addActor(enemyIcon)
                } else {
                     // Enemy finished acting -> Player Turn
                     playerIcon_NotTurn.remove()
                     enemyIcon.remove()
                     uiStage.addActor(playerIcon)
                     uiStage.addActor(enemyIcon_NotTurn)
                }
            },

            onSfx = { e ->
                when (e) {
                    SfxEvent.PlayerAttack -> sfxPlayerAttack.play(0.9f)
                    SfxEvent.EnemyAttack  -> sfxEnemyAttack.play(0.9f)
                    SfxEvent.PlayerHurt   -> sfxPlayerHurt.play(0.9f)
                    SfxEvent.EnemyHurt    -> sfxEnemyHurt.play(0.9f)
                    SfxEvent.PlayerDeath  -> sfxEnemyDeath.play(0.9f)
                    SfxEvent.EnemyDeath   -> sfxEnemyDeath.play(0.9f)
                    SfxEvent.LevelUp      -> sfxLevelUp.play(0.9f)
                }
            },

            onDefeat = { defeated, by ->
                if (defeated === enemy && by === player) {
                    val coins = 10
                    player.currency += coins
                    roundNumber += 1  // increment round when enemy is defeated
                    Gdx.app.log("REWARD", "+$coins Gold. Total: ${player.currency} | Round $roundNumber")
                    showToast("+10 XP, +$$coins\nRound: $roundNumber", 1.5f)

                    // Attempt Drop
                    rollForDrop(roundNumber)

                    scheduleNextEnemy(delaySeconds = 2f)
                    saveGame() // Auto-save after round
                }
                if (defeated === player) {
                    playerSprite.playDeath()
                    val delay = playerSprite.deathDuration()
                    combat.pauseNextTurnFor(delay)
                    showToast("You were defeated...", 2f)
                    scheduleGameOver(delaySeconds = delay)
                }
            },
            onDamage = { character, amount ->
                showDamagePopup(character, amount)
            },
            onLevelUp = { newLevel, PlayerRef ->
                if (this::playerLevelLabel.isInitialized) {
                    playerLevelLabel.setText("Lvl $newLevel")
                }
                playerHealthLabel.setText("${PlayerRef.health}/${PlayerRef.maxHealth}")

                showToast("Level up! You are now Lvl $newLevel", 2.0f)
                sfxLevelUp.play(0.9f)
            },
            resolveDelay = 0f
        )
    }

    private fun rollForDrop(round: Int) {
        // --- 1. POTION DROP (Indepedent) ---
        // 70% Chance
        if (kotlin.random.Random.nextFloat() <= 0.70f) {
             // Simple weighting for potions
             val roll = kotlin.random.Random.nextFloat()
             val potion = when {
                 roll < 0.60f -> inventory.createHealthPotion()
                 roll < 0.80f -> inventory.createDefensivePotion()
                 else -> inventory.createFirePotion()
             }
             inventory.addItem(potion)
             showToast("Found ${potion.name}!", 2f)
        }

        // --- 2. EQUIPMENT DROP (Indepedent) ---
        // 100% Chance (Guaranteed)
        if (kotlin.random.Random.nextFloat() <= 0.80f) {

            // Dynamic Rarity Weights based on Round
            // User Request: Start very small (e.g. 1%) and grow as rounds progress.
            // Formula: Weight = (Round - StartThreshold) * Multiplier
            
            val wCommon = 100
            
            // Start Round 4. At Round 5, weight is (5-3)=2. 2/102 ≈ 2%. 
            val wUncommon = (round - 3).coerceAtLeast(0) * 1 
            
            // Start Round 10.
            val wRare = (round - 9).coerceAtLeast(0) * 1
            
            // Start Round 20.
            val wEpic = (round - 19).coerceAtLeast(0) * 1
            
            // Start Round 30.
            val wLegendary = (round - 29).coerceAtLeast(0) * 1
            
            val totalWeight = wCommon + wUncommon + wRare + wEpic + wLegendary
            val roll = kotlin.random.Random.nextInt(totalWeight)

            var current = 0
            var selectedRarity = "Common"

            if (roll < (current + wCommon)) { selectedRarity = "Common" }
            else {
                current += wCommon
                if (roll < (current + wUncommon)) { selectedRarity = "Uncommon" }
                else {
                    current += wUncommon
                    if (roll < (current + wRare)) { selectedRarity = "Rare" }
                    else {
                        current += wRare
                        if (roll < (current + wEpic)) { selectedRarity = "Epic" }
                        else { selectedRarity = "Legendary" }
                    }
                }
            }

            // Pick Item Type (50/50 Armor/Weapon)
            if (kotlin.random.Random.nextBoolean()) {
                // ARMOR
                val validArmor = DEFAULT_ARMOR_BLUEPRINTS.filter { it.rarity == selectedRarity }
                if (validArmor.isNotEmpty()) {
                    val blueprint = validArmor.random()
                    // Manually build the item since buildDefaultArmor builds ALL items
                    val region = itemIcons[blueprint.sprite.row][blueprint.sprite.col]
                    val piece = ArmorPiece(
                        name = blueprint.name,
                        slot = blueprint.slot,
                        icon = region,
                        rarity = blueprint.rarity,
                        defense = blueprint.defense,
                        health = blueprint.health
                    )
                    armorInventory.add(piece)
                    showToast("Found ${piece.name}!", 2f)
                }
            } else {
                // WEAPON
                val validWeapons = DEFAULT_WEAPON_BLUEPRINTS.filter { it.rarity == selectedRarity }
                if (validWeapons.isNotEmpty()) {
                    val blueprint = validWeapons.random()
                    val region = itemIcons[blueprint.sprite.row][blueprint.sprite.col]
                    val weapon = Weapon(
                        name = blueprint.name,
                        type = blueprint.type,
                        icon = region,
                        rarity = blueprint.rarity,
                        attack = blueprint.attack
                    )
                    weaponInventory.add(weapon)
                    showToast("Found ${weapon.name}!", 2f)
                }
            }
        }
    }

    override fun show() {
        updateFont()
        uiStage.clear() // Clear previous actors (labels, buttons) to prevent overlaps/duplicates

        pauseUI.updateFont(font)
        GameLogic.gameState = GameLogic.GameState.BATTLE

        buildItemsTable()
        initEquipmentSprites()
        buildEquipmentTable()
        buildGameOverTable()
        updateEquipmentTable()

        playerSprite = PlayerSprite(game.worldViewport)

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/battle-fighting-warrior-drums-372078.mp3"))
        backgroundMusic.isLooping = true
        backgroundMusic.volume = 0.3f
        backgroundMusic.play()

        sfxPlayerAttack = Gdx.audio.newSound(Gdx.files.internal("audio/violent-sword-slice-393839.mp3"))
        sfxEnemyAttack  = Gdx.audio.newSound(Gdx.files.internal("audio/magical-hit-45356.mp3"))
        sfxPlayerHurt   = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyHurt    = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyDeath   = Gdx.audio.newSound(Gdx.files.internal("audio/sword-clattering-to-the-ground-393838.mp3"))
        sfxLevelUp      = Gdx.audio.newSound(Gdx.files.internal("audio/level-up-06-370051.mp3"))
        if (!this::enemyKind.isInitialized) {
            spawnRandomEnemy()
        }

        if (!this::enemy.isInitialized) {
            spawnRandomEnemy()
        }

        pauseUI.updateFont(font)
        pauseUI.onResize()

        pauseUI.onSaveRequested = {
            saveGame()
        }

        pauseUI.onMainMenuRequested = {
            game.setScreen<MainMenuScreen>()
        }

        Gdx.input.inputProcessor = uiStage
        uiStage.addActor(menuTable)
        uiStage.addActor(itemsTable)
        uiStage.addActor(equipmentTable)
        uiStage.addActor(gameOverTable)
        uiStage.addActor(playerHealthLabel)
        uiStage.addActor(enemyHealthLabel)
        uiStage.addActor(playerIcon)
        uiStage.addActor(enemyIcon_NotTurn)
        GameLogic.screen = this

        // Labels for Levels (Dynamic Position)
        playerLevelLabel = Label("Lvl ${player.level}", Label.LabelStyle(font, Color.WHITE))
        uiStage.addActor(playerLevelLabel)

        enemyLevelLabel = Label("Lvl ${enemy.level}", Label.LabelStyle(font, Color.WHITE))
        uiStage.addActor(enemyLevelLabel)

        playerHealthLabel.setSize(300f, 200f)

        enemyHealthLabel.setSize(300f, 200f)

        playerIcon.setSize(200f, 200f)

        playerIcon_NotTurn.setSize(200f, 200f)

        enemyIcon.setSize(200f, 200f)

        enemyIcon_NotTurn.setSize(200f, 200f)

        // Round label at top of screen (UI actor)
        roundLabel = Label("Round: $roundNumber", Label.LabelStyle(font, Color.WHITE))
        uiStage.addActor(roundLabel)
        positionRoundLabel()
    }

    private fun positionRoundLabel() {
        if (!this::roundLabel.isInitialized) return
        roundLabel.setPosition(
            (Gdx.graphics.width - roundLabel.prefWidth) / 2f,
            Gdx.graphics.height - roundLabel.prefHeight - 20f
        )
    }

    private fun showDamagePopup(character: Character, amount: Int) {
        showFloatingText(character, "-$amount", Color.RED)
    }

    private fun showHealPopup(character: Character, amount: Int) {
        showFloatingText(character, "+$amount", Color.GREEN)
    }

    private fun showFloatingText(character: Character, text: String, color: Color) {
        val labelStyle = Label.LabelStyle(font, color)
        val popup = Label(text, labelStyle)

        // Position roughly above the sprite
        // We need to project world coordinates to UI stage coordinates or just estimate
        // The sprites are drawn in world coordinates (projectionMatrix = worldViewport.camera.combined)
        // The UI is drawn in UI coordinates.
        // Simple map:
        val x = if (character === player) Gdx.graphics.width * 0.25f else Gdx.graphics.width * 0.75f
        val y = Gdx.graphics.height * 0.6f

        popup.setPosition(x, y)
        popup.setFontScale(1.5f)

        popup.addAction(Actions.sequence(
            Actions.parallel(
                Actions.moveBy(0f, 100f, 1.5f, Interpolation.pow2Out),
                Actions.fadeOut(1.5f)
            ),
            Actions.removeActor()
        ))

        uiStage.addActor(popup)
    }

    override fun resume() {
        updateFont()
    }

    override fun resize(width: Int, height: Int) {
        updateFont()

        game.worldViewport.update(width, height, true)
        game.worldViewport.camera.update()
        game.uiViewport.update(width, height, true)
        game.uiViewport.camera.update()

        pauseUI.updateFont(this.font)
        pauseUI.onResize()

        menuTable.setPosition(Gdx.graphics.width / 2f, Gdx.graphics.height / 2f * 0.1f)
        menuTable.bottom()

        val iconY = Gdx.graphics.height - 400f

        // Player: Icon (50) -> Label (250)
        playerIcon.setPosition(25f, iconY)
        playerIcon_NotTurn.setPosition(25f, iconY)
        playerHealthLabel.setPosition(225f, iconY)

        // Enemy: Icon (Width-500) -> Label (Width-300)
        enemyIcon.setPosition(Gdx.graphics.width - 525f, iconY)
        enemyIcon_NotTurn.setPosition(Gdx.graphics.width - 525f, iconY)
        enemyHealthLabel.setPosition(Gdx.graphics.width - 325f, iconY)

        positionRoundLabel()
    }

    override fun render(delta: Float) {
        input(delta)
        logic(delta)
        draw(delta)
    }

    fun input(delta: Float) { }

    fun logic(delta: Float) {
        if (isGameOver) {
            return
        }
        combat.update(delta)
        playerSprite.update(delta)
        enemySprite.update(delta)

        if (toastTimer > 0f) {
            toastTimer -= delta
            if (toastTimer <= 0f) toastText = null
        }

        if (pendingNextEnemy) {
            nextEnemyDelay -= delta
            if (nextEnemyDelay <= 0f) {
                pendingNextEnemy = false
                spawnRandomEnemy()
            }
        }

        if (pendingGameOver) {
            gameOverDelay -= delta
            if (gameOverDelay <= 0f) {
                pendingGameOver = false
                showGameOverPopup()
            }
        }
    }

    fun draw(delta: Float) {
        drawGame(delta)
        drawUI(delta)
    }

    fun drawGame(delta: Float) {
        game.worldViewport.apply()
        game.batch.projectionMatrix = game.worldViewport.camera.combined

        game.batch.use {
            tiledMapCamera.setToOrtho(false, 10f, 20f)
            tiledMapCamera.update()
            tiledMapRenderer.setView(tiledMapCamera)
            tiledMapRenderer.render()

            playerSprite.draw(it)
            enemySprite.draw(it)
        }
    }

    fun drawUI(delta: Float) {
        uiStage.act(delta)

        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        game.batch.use {
            pauseUI.drawAndHandleInput(game.batch)

            if (!pauseUI.isPaused) {
                playerHealthLabel.setText("${player.health}/${player.maxHealth}")
                enemyHealthLabel.setText("${enemy.health}/${enemy.maxHealth}")

                if (this::roundLabel.isInitialized) {
                    roundLabel.setText("Round: $roundNumber")
                }
            }

            toastText?.let { msg ->
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
        if (isGameOver) {
            menuTable.isVisible = false
            playerHealthLabel.isVisible = false
            enemyHealthLabel.isVisible = false
            itemsTable.isVisible = false
            playerIcon.isVisible = false
            enemyIcon.isVisible = false
            playerIcon_NotTurn.isVisible = false
            enemyIcon_NotTurn.isVisible = false

            // Game Over state - only show game over table
             if (this::playerLevelLabel.isInitialized) {
                playerLevelLabel.isVisible = false
             }
             if (this::enemyLevelLabel.isInitialized) {
                enemyLevelLabel.isVisible = false
             }

            uiStage.draw()
            return
        }
        if (!pauseUI.isPaused) {
            val showingOverlay = isShowingItems || isShowingEquipment

            menuTable.isVisible = !showingOverlay
            playerHealthLabel.isVisible = !showingOverlay
            enemyHealthLabel.isVisible = !showingOverlay
            playerIcon.isVisible = !showingOverlay
            enemyIcon.isVisible = !showingOverlay
            playerIcon_NotTurn.isVisible = !showingOverlay
            enemyIcon_NotTurn.isVisible = !showingOverlay

        if (this::playerLevelLabel.isInitialized) {
             playerLevelLabel.isVisible = !showingOverlay
             // Position above player sprite
             if (this::playerSprite.isInitialized) {
                 val topY = playerSprite.y + playerSprite.cfg.drawHeight
                 val screenPos = game.worldViewport.project(com.badlogic.gdx.math.Vector3(playerSprite.x + 0.5f, topY + 0.5f, 0f))
                 playerLevelLabel.setPosition(screenPos.x - playerLevelLabel.prefWidth / 2, screenPos.y)
             }
        }

        if (this::enemyLevelLabel.isInitialized) {
             enemyLevelLabel.isVisible = !showingOverlay
             enemyLevelLabel.setText("Lvl ${enemy.level}") // Update text just in case
             // Position above enemy sprite
             if (this::enemySprite.isInitialized) {
                 val topY = enemySprite.y + enemySprite.cfg.drawHeight
                 val screenPos = game.worldViewport.project(com.badlogic.gdx.math.Vector3(enemySprite.x + 0.5f, topY + 0.5f, 0f))
                 enemyLevelLabel.setPosition(screenPos.x - enemyLevelLabel.prefWidth / 2 + 180f, screenPos.y)
             }
        }

            itemsTable.isVisible = isShowingItems
            equipmentTable.isVisible = isShowingEquipment

            uiStage.draw()
        }
    }

    fun getWidthInPixels(): Float {
        return game.worldViewport.worldWidth / game.worldViewport.screenWidth
    }

    fun getHeightInPixels(): Float {
        return game.worldViewport.worldHeight / game.worldViewport.screenHeight
    }

    override fun hide() {
        if (this::backgroundMusic.isInitialized) {
            backgroundMusic.stop()
        }
    }

    private fun onePixel(color: Color): TextureRegionDrawable {
        val pm = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        return TextureRegionDrawable(t)
    }

    override fun dispose() {
        font.dispose()
        smallFont.dispose()
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
        sfxItemHeal.dispose()
        sfxItemFail.dispose()
        sfxLevelUp.dispose()
        inventory.dispose()
        if (this::armorTexture.isInitialized) {
            armorTexture.dispose()
        }
        super.dispose()
    }
    private fun saveGame() {
        if (currentSession == null) return
        val state = GameState(
            roundNumber = roundNumber,
            enemyKind = if (this::enemyKind.isInitialized) enemyKind.name else "RedGrunt",
            player = CharacterSnapshot.from(player, player.currency),
            enemy = CharacterSnapshot.from(enemy, 0),

            potions = inventory.toSaveData(),
            inventoryArmor = armorInventory.map { toArmorData(it) },
            inventoryWeapons = weaponInventory.map { toWeaponData(it) },

            equippedHelmet = equipmentSlots.helmet?.let { toArmorData(it) },
            equippedChest = equipmentSlots.chest?.let { toArmorData(it) },
            equippedBoots = equipmentSlots.boots?.let { toArmorData(it) },
            equippedWeapon = equipmentSlots.weapon?.let { toWeaponData(it) }
        )
        SaveGame.save(state, currentSession!!.slotId)
        showToast("Saved!", 1f)
    }

    private fun toArmorData(piece: ArmorPiece) = ArmorData(
        name = piece.name, slot = piece.slot.name, rarity = piece.rarity,
        defense = piece.defense, health = piece.health
    )

    private fun restoreArmor(data: ArmorData): ArmorPiece? {
        val bp = DEFAULT_ARMOR_BLUEPRINTS.find { it.name == data.name } ?: return null
        return ArmorPiece(
             name = bp.name, slot = bp.slot,
             icon = itemIcons[bp.sprite.row][bp.sprite.col],
             rarity = data.rarity, defense = data.defense, health = data.health
        )
    }

    private fun toWeaponData(piece: Weapon) = WeaponData(
        name = piece.name, type = piece.type.name, rarity = piece.rarity,
        attack = piece.attack
    )

    private fun restoreWeapon(data: WeaponData): Weapon? {
        val bp = DEFAULT_WEAPON_BLUEPRINTS.find { it.name == data.name } ?: return null
        return Weapon(
             name = bp.name, type = bp.type,
             icon = itemIcons[bp.sprite.row][bp.sprite.col],
             rarity = data.rarity, attack = data.attack
        )
    }
}
