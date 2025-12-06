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
    private val armorInventory = mutableListOf<ArmorPiece>()
    private val weaponInventory = mutableListOf<Weapon>()
    private val equipmentSlots = EquipmentSlots()

    // round number (starts at 0, increments each time an enemy is defeated)
    private var roundNumber = 0

    private val player = Player(name = "Hero")

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

        val basePlayer = Player(name = session.playerName)
        player.name = basePlayer.name
        player.level = basePlayer.level
        player.health = basePlayer.health
        player.defenseStat = basePlayer.defenseStat
        player.attackStat = basePlayer.attackStat
        player.currency = basePlayer.currency
        player.activeEffects.clear()

        inventory.loadDefaultPotions()
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

        player.name = state.player.name
        player.health = state.player.health
        player.level = state.player.level
        player.defenseStat = state.player.defenseStat
        player.attackStat = state.player.attackStat
        player.currency = state.player.currency
        
        // Restore Active Effects
        player.activeEffects.clear()
        state.player.activeEffects.forEach { snap ->
            player.activeEffects.add(com.anotherround.combat.StatusEffect(snap.name, snap.type, snap.duration, snap.value))
        }

        spawnRandomEnemy()
        
        // Restore Enemy Effects
        enemy.activeEffects.clear()
        state.enemy.activeEffects.forEach { snap ->
            enemy.activeEffects.add(com.anotherround.combat.StatusEffect(snap.name, snap.type, snap.duration, snap.value))
        }

        // Restore Round Number
        roundNumber = state.roundNumber

        // Restore Inventory (Potions)
        inventory.getItems().clear()
        state.inventory.forEach { snapshot ->
            val potion = when (snapshot.type) {
                com.anotherround.Consumables.PotionType.HEALTH -> com.anotherround.Consumables.PotionFactory.createHealthPotion(snapshot.rarity)
                com.anotherround.Consumables.PotionType.DEFENSE -> com.anotherround.Consumables.PotionFactory.createDefensiveLacquer()
                com.anotherround.Consumables.PotionType.ATTACK -> com.anotherround.Consumables.PotionFactory.createLiquidFire()
            }
            inventory.addItem(potion)
        }

        // Restore Equipment Inventory
        armorInventory.clear()
        state.armorInventory.forEach { snapshot ->
            val blueprint = com.anotherround.Equipment.DEFAULT_ARMOR_BLUEPRINTS.find { it.name == snapshot.name }
            if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                armorInventory.add(ArmorPiece(
                    name = snapshot.name,
                    slot = snapshot.slot,
                    icon = region,
                    rarity = snapshot.rarity,
                    defense = snapshot.defense,
                    health = snapshot.health
                ))
            }
        }

        weaponInventory.clear()
        state.weaponInventory.forEach { snapshot ->
            val blueprint = com.anotherround.Equipment.DEFAULT_WEAPON_BLUEPRINTS.find { it.name == snapshot.name }
            if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                weaponInventory.add(Weapon(
                    name = snapshot.name,
                    type = snapshot.type,
                    icon = region,
                    rarity = snapshot.rarity,
                    attack = snapshot.attack
                ))
            }
        }

        // Restore Equipped Items
        equipmentSlots.weapon = state.equipped.weapon?.let { snap ->
             val blueprint = com.anotherround.Equipment.DEFAULT_WEAPON_BLUEPRINTS.find { it.name == snap.name }
             if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                Weapon(snap.name, snap.type, region, snap.rarity, snap.attack)
             } else null
        }

        equipmentSlots.helmet = state.equipped.helmet?.let { snap ->
             val blueprint = com.anotherround.Equipment.DEFAULT_ARMOR_BLUEPRINTS.find { it.name == snap.name }
             if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                ArmorPiece(snap.name, snap.slot, region, snap.rarity, snap.defense, snap.health)
             } else null
        }

        equipmentSlots.chest = state.equipped.chest?.let { snap ->
             val blueprint = com.anotherround.Equipment.DEFAULT_ARMOR_BLUEPRINTS.find { it.name == snap.name }
             if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                ArmorPiece(snap.name, snap.slot, region, snap.rarity, snap.defense, snap.health)
             } else null
        }

        equipmentSlots.boots = state.equipped.boots?.let { snap ->
             val blueprint = com.anotherround.Equipment.DEFAULT_ARMOR_BLUEPRINTS.find { it.name == snap.name }
             if (blueprint != null && this::armorTexture.isInitialized) {
                val cells = TextureRegion.split(armorTexture, 64, 64)
                val region = cells[blueprint.sprite.row][blueprint.sprite.col]
                ArmorPiece(snap.name, snap.slot, region, snap.rarity, snap.defense, snap.health)
             } else null
        }

        showToast("Loaded Game: ${state.player.name}", 1.5f)
    }

    private fun performAutoSave() {
        currentSession?.let { session ->
            SaveGame.save(
                player = player,
                enemy = enemy,
                roundNumber = roundNumber,
                inventory = inventory.getItems(),
                armorInventory = armorInventory,
                weaponInventory = weaponInventory,
                equipmentSlots = equipmentSlots,
                slot = session.slotId
            )
            Gdx.app.log("BattleScreen", "Auto-saved to slot ${session.slotId}")
        }
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
        label.width = 400f
        label.height = 200f
        label
    }

    private val enemyHealthLabel by lazy {
        val label = TextButton("", buttonStyle)
        label.width = 400f
        label.height = 200f
        label
    }

    lateinit var attackButton: TextButton
    lateinit var itemsButton: TextButton
    lateinit var equipmentButton: TextButton

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

    // popup for inventory stats
    private lateinit var equipmentPopupContainer: Table
    private lateinit var equipmentPopupNameLabel: Label
    private lateinit var equipmentPopupLabel: Label
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
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
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
            val potion = inventory.getItems().getOrNull(i)

            val itemSlotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(itemSlotBg)
            slotGroup.clearListeners()
            itemSlotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (potion != null) {
                val itemImage = Image(potion.textureRegion)
                itemImage.setSize(itemSize, itemSize)
                slotGroup.addActor(itemImage)
                itemImage.setPosition(itemPadding, itemPadding)

                nameLabel = Label(potion.name, nameLabelStyle)
                nameLabel.setWrap(false)
                descLabel = Label(potion.description, descLabelStyle)
                descLabel.setWrap(true)

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        // Handle different potion types
                        when (potion.type) {
                            com.anotherround.Consumables.PotionType.HEALTH -> {
                                player.heal(potion.effectValue)
                                sfxItemHeal.play(50f)
                                showToast("Healed ${potion.effectValue} HP")
                            }
                            com.anotherround.Consumables.PotionType.DEFENSE -> {
                                player.activeEffects.add(com.anotherround.combat.StatusEffect(
                                    name = "Defensive Lacquer",
                                    type = com.anotherround.combat.EffectType.DEFENSE_BUFF,
                                    duration = potion.duration,
                                    value = potion.effectValue
                                ))
                                sfxItemHeal.play(50f)
                                showToast("Applied Defense Buff")
                            }
                            com.anotherround.Consumables.PotionType.ATTACK -> {
                                enemy.activeEffects.add(com.anotherround.combat.StatusEffect(
                                    name = "Liquid Fire",
                                    type = com.anotherround.combat.EffectType.DOT_FIRE,
                                    duration = potion.duration,
                                    value = potion.effectValue
                                ))
                                sfxItemHeal.play(50f) // Maybe different sound?
                                showToast("Applied Liquid Fire to Enemy")
                            }
                        }
                        inventory.useItem(potion)
                        updateItemsTable()
                    }
                })

            } else {
                nameLabel = Label("", nameLabelStyle)
                descLabel = Label("", descLabelStyle)

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        sfxItemFail.play(50f)
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
            val potion = inventory.getItems().getOrNull(i) // Get item for this slot

            // slot bg
            val itemSlotBg = Image(skin.getDrawable("item-slot"))
            val slotGroup = Group()
            slotGroup.addActor(itemSlotBg)
            itemSlotBg.setSize(slotSize, slotSize)

            val nameLabel: Label
            val descLabel: Label

            if (potion != null) {
                // item icon
                val itemImage = Image(potion.textureRegion)
                itemImage.setSize(itemSize, itemSize)
                slotGroup.addActor(itemImage)
                itemImage.setPosition(itemPadding, itemPadding)

                // Set the text
                nameLabel = Label(potion.name, nameLabelStyle)
                descLabel = Label(potion.description, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                // Shop Logic: Sell random potions or specific ones?
                // For now, let's just sell Small Health Potions for 10g
                // Or maybe cycle through types?
                // Let's make it simple: Buy Small Health Potion
                nameLabel = Label("Small Health Potion", nameLabelStyle)
                descLabel = Label("Tap to buy for 10 gold", descLabelStyle)
                slotGroup.clearListeners()

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        if (player.currency >= 10) {
                            player.currency -= 10
                            inventory.addItem(
                                com.anotherround.Consumables.PotionFactory.createHealthPotion(com.anotherround.Consumables.PotionRarity.COMMON)
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

        val cells = TextureRegion.split(armorTexture, 64, 64)

        armorInventory.clear()
        armorInventory.addAll(buildDefaultArmor(cells))

        weaponInventory.clear()
        weaponInventory.addAll(buildDefaultWeapons(cells))

        // Default equipped items:
        equipmentSlots.weapon = weaponInventory.firstOrNull() // first weapon
        equipmentSlots.helmet = armorInventory.firstOrNull { it.slot == ArmorSlot.HELMET }
        equipmentSlots.chest  = armorInventory.firstOrNull { it.slot == ArmorSlot.CHEST }
        equipmentSlots.boots  = armorInventory.firstOrNull { it.slot == ArmorSlot.BOOTS }
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

        equipmentPopupContainer.add(popupInner)
            .expandX()
            .padTop(100f)
            .center()

        equipmentTable.addActor(equipmentPopupContainer)
    }

    private fun showEquipmentPopup(itemName: String, statsText: String, index: Int) {
        selectedEquipmentIndex = index
        equipmentPopupNameLabel.setText(itemName)
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

                val descText = "Rarity: ${piece.rarity}\nDEF: ${piece.defense}   HP: ${piece.health}"
                descLabel = Label(descText, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                nameLabel = Label("None", nameLabelStyle)
                nameLabel.setFontScale(0.75f)
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

                val descText = "Rarity: ${weapon.rarity}\nATK: ${weapon.attack}"
                descLabel = Label(descText, descLabelStyle)
                descLabel.setWrap(true)
            } else {
                nameLabel = Label("None", nameLabelStyle)
                nameLabel.setFontScale(0.75f)
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
                slotBg.color = Color.YELLOW
            } else {
                slotBg.color = Color.WHITE
            }

            if (item != null) {
                val region = when (item) {
                    is ArmorPiece -> item.icon
                    is Weapon -> item.icon
                    else -> null
                }
                if (region != null) {
                    val icon = Image(region)
                    icon.setSize(itemSize, itemSize)
                    icon.setPosition(itemPadding, itemPadding)
                    group.addActor(icon)
                }

                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        // show popup with stats and equip button
                        val name = when (item) {
                            is ArmorPiece -> item.name
                            is Weapon -> item.name
                            else -> "?"
                        }
                        val stats = when (item) {
                            is ArmorPiece -> "Slot: ${item.slot}\nDef: ${item.defense}\nHP: ${item.health}\nRarity: ${item.rarity}"
                            is Weapon -> "Type: ${item.type}\nAtk: ${item.attack}\nRarity: ${item.rarity}"
                            else -> ""
                        }
                        showEquipmentPopup(name, stats, i)
                    }
                })
            }

            equipmentInventoryTable.add(group).size(slotSize).pad(5f)
            col++
            if (col >= itemsPerRow) {
                col = 0
                equipmentInventoryTable.row()
            }
        }
    }

    override fun show() {
        // Load assets
        initEquipmentSprites()
        buildItemsTable()
        buildEquipmentTable()
        buildGameOverTable()

        // Input
        Gdx.input.inputProcessor = uiStage

        // Build UI
        uiStage.addActor(menuTable)
        menuTable.setPosition(Gdx.graphics.width * 0.25f, Gdx.graphics.height * 0.25f)

        uiStage.addActor(itemsTable)
        itemsTable.isVisible = false

        uiStage.addActor(equipmentTable)
        equipmentTable.isVisible = false

        uiStage.addActor(gameOverTable)

        // Pause UI
        pauseUI.build()
        pauseUI.onResume = {
            // nothing special
        }
        pauseUI.onSaveRequested = {
            try {
                val slotToSave = currentSession?.slotId ?: 1
                SaveGame.save(player, enemy, roundNumber, inventory.getItems(), armorInventory, weaponInventory, equipmentSlots, slotToSave)
                Gdx.app.log("SAVE", "Game saved to slot $slotToSave")
                showToast("Game Saved (Slot $slotToSave)", 1.5f)
            } catch (t: Throwable) {
                Gdx.app.error("SAVE", "Failed to save", t)
                showToast("Save Failed", 1.5f)
            }
        }
        pauseUI.onExit = {
            game.setScreen<MainMenuScreen>()
        }
        uiStage.addActor(pauseUI.rootTable)

        // Player & Enemy Labels
        uiStage.addActor(playerHealthLabel)
        playerHealthLabel.setPosition(100f, Gdx.graphics.height - 250f)

        uiStage.addActor(enemyHealthLabel)
        enemyHealthLabel.setPosition(Gdx.graphics.width - 500f, Gdx.graphics.height - 250f)

        // Round Label
        val roundStyle = Label.LabelStyle(font, Color.YELLOW)
        roundLabel = Label("Round 1", roundStyle)
        roundLabel.setFontScale(1.5f)
        roundLabel.setPosition(Gdx.graphics.width / 2f - 50f, Gdx.graphics.height - 100f)
        uiStage.addActor(roundLabel)

        // Setup Combat
        setupCombat()
    }

    private fun setupCombat() {
        combat = CombatManager(
            player = player,
            enemy = enemy,
            onLog = { msg -> Gdx.app.log("Combat", msg) },
            onActionStart = { action ->
                if (action is Action.Attack) {
                    if (action.attacker === player) {
                        playerSprite.playAttack()
                    } else {
                        enemySprite.playAttack()
                    }
                }
            },
            onActionEnd = { action ->
                // nothing special
            },
            onSfx = { event ->
                when (event) {
                    SfxEvent.PlayerAttack -> { /* played by animation? or here */ }
                    SfxEvent.EnemyAttack -> { /* ... */ }
                    SfxEvent.PlayerHurt -> playerSprite.playHurt()
                    SfxEvent.EnemyHurt -> enemySprite.playHurt()
                    SfxEvent.PlayerDeath -> { /* handled in onDefeat */ }
                    SfxEvent.EnemyDeath -> { /* handled in onDefeat */ }
                }
            },
            onDefeat = { defeated, by ->
                if (defeated === enemy && by === player) {
                    val coins = 10
                    player.currency += coins
                    roundNumber += 1  // increment round when enemy is defeated
                    Gdx.app.log("REWARD", "+$coins Gold. Total: ${player.currency} | Round $roundNumber")
                    showToast("+10 XP, +$$coins\nRound: $roundNumber", 1.5f)
                    
                    // Auto-save after victory
                    performAutoSave()

                    scheduleNextEnemy(delaySeconds = 2f)
                }
                if (defeated === player) {
                    playerSprite.playDeath()
                    val delay = playerSprite.deathDuration()
                    combat.pauseNextTurnFor(delay)
                    showToast("You were defeated...", 2f)
                    scheduleGameOver(delaySeconds = delay)
                }
            }
        )
    }

    private fun spawnRandomEnemy() {
        val kinds = EnemyKind.values()
        enemyKind = kinds.random()

        enemy = when (enemyKind) {
            EnemyKind.Grunt -> RedGrunt()
            EnemyKind.Phantom -> Phantom()
            EnemyKind.EvilWizard -> EvilWizard()
            EnemyKind.NightBorne -> NightBorne()
        }
        // scale enemy stats by round number?
        enemy.health += roundNumber * 5
        enemy.attackStat += roundNumber
        enemy.activeEffects.clear()

        Gdx.app.log("Battle", "Spawned ${enemy.name} (Round $roundNumber)")

        // Re-init sprite
        enemySprite = EnemySprite(enemyKind)
        playerSprite = PlayerSprite() // ensure player sprite is ready
    }

    private fun scheduleNextEnemy(delaySeconds: Float) {
        pendingNextEnemy = true
        nextEnemyDelay = delaySeconds
    }

    private fun scheduleGameOver(delaySeconds: Float) {
        pendingGameOver = true
        gameOverDelay = delaySeconds
    }

    override fun render(delta: Float) {
        // Logic
        if (!isGameOver) {
            combat.update(delta)
        }

        // Timers
        if (pendingNextEnemy) {
            nextEnemyDelay -= delta
            if (nextEnemyDelay <= 0f) {
                pendingNextEnemy = false
                spawnRandomEnemy()
                setupCombat() // re-link combat manager to new enemy
            }
        }

        if (pendingGameOver) {
            gameOverDelay -= delta
            if (gameOverDelay <= 0f) {
                pendingGameOver = false
                isGameOver = true
                gameOverTable.isVisible = true
                pauseUI.rootTable.isVisible = false
            }
        }

        if (toastText != null) {
            toastTimer -= delta
            if (toastTimer <= 0f) {
                toastText = null
            }
        }

        // Draw World
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1f)
        Gdx.gl.glClear(Gdx.gl.GL_COLOR_BUFFER_BIT)

        tiledMapRenderer.setView(tiledMapCamera)
        tiledMapRenderer.render()

        game.batch.use { batch ->
            // Draw characters
            // positions hardcoded for demo
            playerSprite.draw(batch, 200f, 300f, delta)
            enemySprite.draw(batch, 800f, 300f, delta)
        }

        // Draw UI
        updateUI()
        uiStage.act(delta)
        uiStage.draw()

        // Draw Toast
        if (toastText != null) {
            game.batch.use { batch ->
                font.color = Color.YELLOW
                font.data.setScale(2f)
                toastLayout.setText(font, toastText)
                font.draw(batch, toastText,
                    (Gdx.graphics.width - toastLayout.width) / 2f,
                    Gdx.graphics.height / 2f + 100f)
                font.data.setScale(1f)
                font.color = Color.WHITE
            }
        }
    }

    private fun updateUI() {
        playerHealthLabel.setText("HP: ${player.health}/${player.maxHealth}")
        enemyHealthLabel.setText("${enemy.name}: ${enemy.health}/${enemy.maxHealth}")
        roundLabel.setText("Round $roundNumber")

        // Show/Hide menus based on state
        if (isGameOver) {
            menuTable.isVisible = false
            itemsTable.isVisible = false
            equipmentTable.isVisible = false
            return
        }

        // If pause menu is open, hide combat menu
        if (pauseUI.isVisible) {
            menuTable.isVisible = false
            return
        }

        // If items or equipment open, hide main menu
        if (isShowingItems) {
            itemsTable.isVisible = true
            menuTable.isVisible = false
            equipmentTable.isVisible = false
        } else if (isShowingEquipment) {
            equipmentTable.isVisible = true
            menuTable.isVisible = false
            itemsTable.isVisible = false
        } else {
            itemsTable.isVisible = false
            equipmentTable.isVisible = false
            menuTable.isVisible = true
        }

        // Disable buttons if not player turn
        val isPlayerTurn = (combat.turn == com.anotherround.combat.Turn.PLAYER)
        attackButton.isDisabled = !isPlayerTurn
        itemsButton.isDisabled = !isPlayerTurn
        // equipmentButton.isDisabled = !isPlayerTurn // maybe allow equipment change anytime?
    }

    override fun resize(width: Int, height: Int) {
        game.worldViewport.update(width, height, true)
        game.uiViewport.update(width, height, true)
        tiledMapCamera.setToOrtho(false, width.toFloat(), height.toFloat())
        pauseUI.resize(width, height)
    }

    override fun dispose() {
        worldStage.dispose()
        uiStage.dispose()
        tiledMap.dispose()
        tiledMapRenderer.dispose()
        if (this::armorTexture.isInitialized) armorTexture.dispose()
        inventory.dispose()
        if (this::playerSprite.isInitialized) playerSprite.dispose()
        if (this::enemySprite.isInitialized) enemySprite.dispose()
    }

    private fun onePixel(color: Color): Texture {
        val pixmap = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fill()
        val t = Texture(pixmap)
        pixmap.dispose()
        return t
    }
}
