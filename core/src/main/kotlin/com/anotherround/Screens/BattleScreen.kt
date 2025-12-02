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
import com.anotherround.Equipment.WeaponType
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

    fun startNewGame(session: GameSession) {
        this.currentSession = session
        Gdx.app.log("BattleScreen", "Starting new game for ${session.playerName} in slot ${session.slotId}")

        val basePlayer = Player(name = session.playerName)
        player.name = basePlayer.name
        player.level = basePlayer.level
        player.health = basePlayer.health
        player.defenseStat = basePlayer.defenseStat
        player.attackStat = basePlayer.attackStat
        player.currency = basePlayer.currency

        inventory.loadDefaultPotions()
        roundNumber = 0

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

        spawnRandomEnemy()

        inventory.loadFromSaveState(state.potions)
        roundNumber = 0  // later you can load this from save too

        showToast("Loaded Game: ${state.player.name}", 1.5f)
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

    private lateinit var itemsTable: Table
    private lateinit var itemsListTable: Table
    private lateinit var nameLabelStyle: Label.LabelStyle
    private lateinit var descLabelStyle: Label.LabelStyle

    // equipment UI tables
    private lateinit var equipmentTable: Table
    private lateinit var equipmentEquippedTable: Table
    private lateinit var equipmentInventoryTable: Table

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
                descLabel = Label(consumable.description, descLabelStyle)
                descLabel.setWrap(true)

                slotGroup.addListener(object: ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y:Float) {
                        val healAmount = inventory.useItem(consumable)
                        player.heal(healAmount)
                        sfxItemHeal.play(50f)
                        showToast("Healed for $healAmount health")
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

        val inventoryLabel = Label("Equipment in Inventory", nameLabelStyle)
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
    }

    /** Top: Weapon + 3 armor slots. Bottom: 8 inventory icons (4x2), armor and weapons mixed. */
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

        // Order: Weapon on top, then armor slots
        addEquippedWeaponRow(equipmentSlots.weapon)
        addEquippedArmorRow(equipmentSlots.helmet)
        addEquippedArmorRow(equipmentSlots.chest)
        addEquippedArmorRow(equipmentSlots.boots)

        // ------------ INVENTORY SECTION (8 slots, armor + weapons mixed) ------------

        val combined = mutableListOf<Any>()
        combined.addAll(armorInventory)
        combined.addAll(weaponInventory)

        val maxSlots = 8
        val itemsPerRow = 4
        var col = 0

        for (i in 0 until maxSlots) {
            val item = combined.getOrNull(i)

            val slotBg = Image(skin.getDrawable("item-slot"))
            val group = Group()
            group.addActor(slotBg)
            slotBg.setSize(slotSize, slotSize)

            if (item is ArmorPiece) {
                val icon = Image(item.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                group.addActor(icon)

                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val text = """
                            Armor:
                            ${item.name}
                            Rarity: ${item.rarity}
                            DEF: ${item.defense}   HP: ${item.health}
                        """.trimIndent()
                        showToast(text, 1.8f)
                    }
                })
            } else if (item is Weapon) {
                val icon = Image(item.icon)
                icon.setSize(itemSize, itemSize)
                icon.setPosition(itemPadding, itemPadding)
                group.addActor(icon)

                group.addListener(object : ClickListener() {
                    override fun clicked(event: InputEvent?, x: Float, y: Float) {
                        val text = """
                            Weapon:
                            ${item.name}
                            Rarity: ${item.rarity}
                            ATK: ${item.attack}
                        """.trimIndent()
                        showToast(text, 1.8f)
                    }
                })
            }
            // else: empty slot, just the background

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

    private fun spawnRandomEnemy() {
        enemyKind = EnemyFactory.randomKind()
        enemy = EnemyFactory.create(enemyKind)

        if (this::enemySprite.isInitialized) {
            enemySprite.dispose()
        }
        enemySprite = EnemySprite(game.worldViewport, enemyKind)

        setupCombat()

        Gdx.app.log("ENEMY", "Spawned ${enemy.name} of kind $enemyKind")
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
                }
            },

            onActionEnd = { action ->
                when (action) {
                    is Action.Attack -> {
                        if (action.attacker === player) {
                            if (enemy.isAlive()) {
                                enemySprite.playHurt()
                                combat.pauseNextTurnFor(max(1.5f, enemySprite.hurtDuration()))
                                playerIcon.remove()
                                enemyIcon_NotTurn.remove()
                                uiStage.addActor(playerIcon_NotTurn)
                                uiStage.addActor(enemyIcon)
                            } else {
                                enemySprite.playDeath()
                                combat.pauseNextTurnFor(enemySprite.deathDuration())
                            }
                        } else if (action.attacker === enemy) {
                            playerSprite.playHurt()
                            combat.pauseNextTurnFor(max(1.5f, playerSprite.hurtDuration()))
                            playerIcon_NotTurn.remove()
                            enemyIcon.remove()
                            uiStage.addActor(playerIcon)
                            uiStage.addActor(enemyIcon_NotTurn)
                        }
                    }
                }
            },

            onSfx = { e ->
                when (e) {
                    SfxEvent.PlayerAttack -> sfxPlayerAttack.play(0.9f)
                    SfxEvent.EnemyAttack  -> sfxEnemyAttack.play(0.9f)
                    SfxEvent.PlayerHurt   -> sfxPlayerHurt.play(0.9f)
                    SfxEvent.EnemyHurt    -> sfxEnemyHurt.play(0.9f)
                    SfxEvent.PlayerDeath  -> { /* TODO */ }
                    SfxEvent.EnemyDeath   -> sfxEnemyDeath.play(1.0f)
                }
            },

            onDefeat = { defeated, by ->
                if (defeated === enemy && by === player) {
                    val coins = 10
                    player.currency += coins
                    roundNumber += 1  // increment round when enemy is defeated
                    Gdx.app.log("REWARD", "+$coins Gold. Total: ${player.currency} | Round $roundNumber")
                    showToast("+10 XP, +$$coins\nRound: $roundNumber", 1.5f)
                    scheduleNextEnemy(delaySeconds = 2f)
                }
                if (defeated === player) {
                    showToast("You were defeated...", 2f)
                }
            },

            resolveDelay = 0f
        )
    }

    override fun show() {
        updateFont()

        pauseUI.updateFont(font)
        GameLogic.gameState = GameLogic.GameState.BATTLE

        buildItemsTable()
        initEquipmentSprites()
        buildEquipmentTable()
        updateEquipmentTable()

        playerSprite = PlayerSprite(game.worldViewport)

        backgroundMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/battle-fighting-warrior-drums-372078.mp3"))
        backgroundMusic.isLooping = true
        backgroundMusic.volume = 1.5f
        backgroundMusic.play()

        sfxPlayerAttack = Gdx.audio.newSound(Gdx.files.internal("audio/violent-sword-slice-393839.mp3"))
        sfxEnemyAttack  = Gdx.audio.newSound(Gdx.files.internal("audio/magical-hit-45356.mp3"))
        sfxPlayerHurt   = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyHurt    = Gdx.audio.newSound(Gdx.files.internal("audio/male_hurt7-48124.mp3"))
        sfxEnemyDeath   = Gdx.audio.newSound(Gdx.files.internal("audio/sword-clattering-to-the-ground-393838.mp3"))

        spawnRandomEnemy()

        pauseUI.updateFont(font)
        pauseUI.onResize()

        pauseUI.onSaveRequested = {
            try {
                val slotToSave = currentSession?.slotId ?: 1
                SaveGame.save(player, enemy, inventory.getItems().size, slotToSave)
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

        Gdx.input.inputProcessor = uiStage
        uiStage.addActor(menuTable)
        uiStage.addActor(itemsTable)
        uiStage.addActor(equipmentTable)
        uiStage.addActor(playerHealthLabel)
        uiStage.addActor(enemyHealthLabel)
        uiStage.addActor(playerIcon)
        uiStage.addActor(enemyIcon_NotTurn)
        GameLogic.screen = this

        playerHealthLabel.setSize(250f, 200f)
        playerHealthLabel.addAction(Actions.moveBy(150f, 0f))

        enemyHealthLabel.setSize(250f, 200f)
        enemyHealthLabel.addAction(Actions.moveBy(50f, 0f))

        playerIcon.setSize(200f, 200f)
        playerIcon.addAction(Actions.moveBy(49f, 2000f))

        playerIcon_NotTurn.setSize(200f, 200f)
        playerIcon_NotTurn.addAction(Actions.moveBy(48f, 2000f))

        enemyIcon.setSize(200f, 200f)
        enemyIcon.addAction(Actions.moveBy(580f, 2000f))

        enemyIcon_NotTurn.setSize(200f, 200f)
        enemyIcon_NotTurn.addAction(Actions.moveBy(580f, 2000f))
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

        playerHealthLabel.setPosition(100f, Gdx.graphics.height - 400f)
        enemyHealthLabel.setPosition(Gdx.graphics.width - 100f - enemyHealthLabel.width, Gdx.graphics.height - 400f)
    }

    override fun render(delta: Float) {
        input(delta)
        logic(delta)
        draw(delta)
    }

    fun input(delta: Float) { }

    fun logic(delta: Float) {
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
                playerHealthLabel.setText("${player.health}")
                enemyHealthLabel.setText("${enemy.health}")

                // COMMENTED OUT the old Player/Enemy labels:
                // font.draw(game.batch,"Player", 60f, 2275f )
                // font.draw(game.batch, "Enemy", 790f, 2275f)

                // New ROUND label at the very top, centered-ish
                val roundText = "Round: $roundNumber"
                toastLayout.setText(font, roundText)
                val rx = (Gdx.graphics.width - toastLayout.width) / 2f
                val ry = Gdx.graphics.height - 50f
                font.draw(game.batch, toastLayout, rx, ry)
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

        if (!pauseUI.isPaused) {
            val showingOverlay = isShowingItems || isShowingEquipment

            menuTable.isVisible = !showingOverlay
            playerHealthLabel.isVisible = !showingOverlay
            enemyHealthLabel.isVisible = !showingOverlay
            playerIcon.isVisible = !showingOverlay
            enemyIcon.isVisible = !showingOverlay
            playerIcon_NotTurn.isVisible = !showingOverlay
            enemyIcon_NotTurn.isVisible = !showingOverlay

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
        backgroundMusic.stop()
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
        inventory.dispose()
        if (this::armorTexture.isInitialized) {
            armorTexture.dispose()
        }
        super.dispose()
    }
}
