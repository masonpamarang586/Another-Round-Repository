package com.anotherround

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.utils.viewport.ScreenViewport
import ktx.app.KtxScreen
import ktx.graphics.use
import com.anotherround.SaveLoad.SaveGame
import com.anotherround.Screens.BattleScreen
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.anotherround.Screens.DictionaryScreen


class MainMenuScreen(private val game: Main) : KtxScreen {

    // Static background
    private lateinit var bgTexture: Texture

    // UI
    private var isUiBuilt = false
    private val stage = Stage(ScreenViewport(game.uiViewport.camera))
    private lateinit var titleTexture: Texture
    private lateinit var skin: Skin

    private lateinit var newGameBtn: TextButton
    private lateinit var loadGameBtn: TextButton
    private lateinit var settingsBtn: TextButton

    private lateinit var overlayRoot: Table
    private lateinit var titleLabel: Label
    private lateinit var slot1: TextButton
    private lateinit var slot2: TextButton
    private lateinit var slot3: TextButton
    private lateinit var nameField: TextField
    private lateinit var confirmBtn: TextButton
    private lateinit var backBtn: TextButton

    private var mode: Submenu = Submenu.NONE
    private var selectedSlot = -1

    private var font: BitmapFont = BitmapFont()
    private val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/monogram.ttf"))

    private enum class Submenu { NONE, NEW, LOAD }

    override fun show() {
        GameLogic.gameState = GameLogic.GameState.MAIN_MENU
        Gdx.input.inputProcessor = stage
        skin = Skin(Gdx.files.internal("atlas/ui.json"))
        updateFont()
        loadStaticBackground("backgrounds/mainmenu_sheet.png")

        loadTitleImage("logo.png")

        if (!isUiBuilt) {
            buildMainButtons()
            buildTitle()
            buildSlotOverlay()
            isUiBuilt = true
        } else {
            refreshButtonFonts()
        }
        updateSlotLabels()
        layoutBottomMenu()

        val dictionaryButton = TextButton("Dictionary", skin)
        dictionaryButton.addListener(object : ChangeListener(){
            override fun changed(event: ChangeEvent?, actor: Actor) {
                game.setScreen<DictionaryScreen>(game)
            }
        })
    }

    override fun render(delta: Float) {
        game.uiViewport.apply()
        game.batch.projectionMatrix = game.uiViewport.camera.combined

        // Draw static PNG full screen
        game.batch.use {
            it.draw(
                bgTexture,
                0f,
                0f,
                Gdx.graphics.width.toFloat(),
                Gdx.graphics.height.toFloat()
            )
        }

        layoutBottomMenu()
        layoutTitle()
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        game.uiViewport.update(width, height, true)
        stage.viewport.update(width, height, true)
        updateFont()
        refreshButtonFonts()
        if (this::titleLabel.isInitialized) {
            titleLabel.style = Label.LabelStyle(font, Color.WHITE)
        }
    }

    override fun hide() { }

    override fun dispose() {
        stage.dispose()
        bgTexture.dispose()
        font.dispose()
        generator.dispose()
        if(this::titleTexture.isInitialized) titleTexture.dispose()
    }

    // ---- Helpers ----
    private fun loadStaticBackground(path: String) {
        bgTexture = Texture(Gdx.files.internal(path)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        }
    }

    private fun loadTitleImage(path: String) {
        if (!this::titleTexture.isInitialized) { // Prevents reloading
            titleTexture = Texture(Gdx.files.internal(path)).apply {
                setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            }
        }
    }

    private fun updateFont() {
        val buttonHeightFraction = 0.08f
        val textToButtonHeight = 0.65f
        val param = FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            size = (Gdx.graphics.height * buttonHeightFraction * textToButtonHeight).toInt()
            minFilter = Texture.TextureFilter.Nearest
            magFilter = Texture.TextureFilter.Nearest
        }
        font.disposeSafely()
        font = generator.generateFont(param).apply { color = Color.BLACK }
    }

    private fun refreshButtonFonts() {
        val mainStyle = TextButton.TextButtonStyle().apply {
            font = this@MainMenuScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }
        if (this::newGameBtn.isInitialized) newGameBtn.style = mainStyle
        if (this::loadGameBtn.isInitialized) loadGameBtn.style = mainStyle
        if (this::settingsBtn.isInitialized) settingsBtn.style = mainStyle

        val slotStyle = TextButton.TextButtonStyle().apply {
            font = this@MainMenuScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }
        slotStyle.font.data.setScale(3.0f)

        if (this::slot1.isInitialized) slot1.style = slotStyle
        if (this::slot2.isInitialized) slot2.style = slotStyle
        if (this::slot3.isInitialized) slot3.style = slotStyle
        if (this::confirmBtn.isInitialized) confirmBtn.style = slotStyle
        if (this::backBtn.isInitialized) backBtn.style = slotStyle

        // Handle TextField
        if (this::nameField.isInitialized) {
            nameField.style.font = this@MainMenuScreen.font
            nameField.style.font.data.setScale(1.0f)
        }

        // Handle Label
        if (this::titleLabel.isInitialized) {
            titleLabel.style.font = this@MainMenuScreen.font
        }
    }

    private fun buildMainButtons() {
        val style = TextButton.TextButtonStyle().apply {
            font = this@MainMenuScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }

        newGameBtn = TextButton("New Game", style)
        loadGameBtn = TextButton("Load Game", style)
        settingsBtn = TextButton("Settings", style)

        newGameBtn.addListener(click { openSlots(Submenu.NEW) })
        loadGameBtn.addListener(click { openSlots(Submenu.LOAD) })
        settingsBtn.addListener(click { Gdx.app.log("Menu", "Settings clicked") })

        val table = Table()

        table.add(newGameBtn).width(800f).height(250f)
        table.row()
        table.add(loadGameBtn).padTop(40f).width(800f).height(250f)
        table.row()
        table.add(settingsBtn).padTop(40f).width(800f).height(250f)
        stage.addActor(table)
        table.name = "bottomMenu"
    }

    private fun buildTitle() {
        val titleImage = Image(titleTexture)

        titleImage.name = "titleImage"

        stage.addActor(titleImage)
    }

    private fun layoutTitle() {
        val titleImage = stage.root.findActor<Image>("titleImage") ?: return

        val scale = 2.25f
        titleImage.setScale(scale)

        titleImage.setPosition(
            (Gdx.graphics.width / 2f) - (titleImage.width * scale / 2f),
            Gdx.graphics.height * 0.75f - (titleImage.height * scale / 2f)
        )
    }

    private fun layoutBottomMenu() {
        val table = stage.root.findActor<Table>("bottomMenu") ?: return
        table.pack()
        table.setPosition(
            (Gdx.graphics.width - table.width) / 2f,
            Gdx.graphics.height * 0.1f
        )
    }

    private fun buildSlotOverlay() {
        overlayRoot = Table().apply {
            setFillParent(true)
            isVisible = false
        }

        val dimmer = Image(TextureRegionDrawable(onePixel(Color(0f, 0f, 0f, 0.45f))))
        dimmer.setFillParent(true)
        overlayRoot.addActor(dimmer)

        titleLabel = Label("Choose a Slot", Label.LabelStyle(font, Color.WHITE))

        val s = TextButton.TextButtonStyle().apply {
            font = this@MainMenuScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
            font.data.setScale(3.0f)
        }

        slot1 = TextButton("Game 1", s).apply { addListener(click { selectSlot(1) }) }
        slot2 = TextButton("Game 2", s).apply { addListener(click { selectSlot(2) }) }
        slot3 = TextButton("Game 3", s).apply { addListener(click { selectSlot(3) }) }

        val textFieldStyle = TextField.TextFieldStyle().apply {
            this.font = this@MainMenuScreen.font
            this.fontColor = Color.BLACK

            this.messageFontColor = Color.BLACK
            this.font.data.setScale(1.0f)

            val bg = TextureRegionDrawable(onePixel(Color(0.8f, 0.8f, 0.8f, 1.0f)))
            this.background = bg
            this.cursor = TextureRegionDrawable(onePixel(Color.BLACK))
            this.selection = TextureRegionDrawable(onePixel(Color(0.5f, 0.5f, 1f, 0.5f)))
        }

        nameField = TextField("", textFieldStyle).apply {
            messageText = "Enter name..."
        }


        confirmBtn = TextButton("Confirm", s).apply {
            addListener(click {
                if (selectedSlot == -1) {
                    Gdx.app.log("UI", "No slot selected")
                    // TODO: show a toast "Please select a slot"
                    return@click
                }

                if (mode == Submenu.NEW) {
                    var playerName = nameField.text.trim()
                    if (playerName.isBlank()) {
                        Gdx.app.log("UI", "Name is blank")
                        // TODO: show a toast "Please enter a name"
                        return@click
                    }

                    val newSession = GameSession(selectedSlot, playerName)

                    game.getScreen<BattleScreen>().startNewGame(newSession)

                    // Switch screens
                    game.setScreen<BattleScreen>()

                } else if (mode == Submenu.LOAD) {
                    val loadedState = SaveGame.loadOrNull(selectedSlot)

                    if (loadedState != null) {
                        game.getScreen<BattleScreen>().loadSavedGame(loadedState, selectedSlot)

                        // Switch screens
                        game.setScreen<BattleScreen>()
                    } else {
                        Gdx.app.log("UI", "Load failed! File for slot $selectedSlot might be corrupt or empty.")
                        // TODO: show toast "LOAD FAILED"
                    }
                }

                closeSlots()
            })
        }

        backBtn = TextButton("Back", s).apply {
            addListener(click { closeSlots() })
        }

        val inner = Table()
        inner.add(titleLabel).padBottom(24f)
        inner.row()
        inner.add(slot1).width(600f).height(200f)
        inner.row()
        inner.add(slot2).padTop(24f).width(600f).height(200f)
        inner.row()
        inner.add(slot3).padTop(36f).width(600f).height(200f)
        inner.row()
        inner.add(nameField).padTop(50f).width(800f).height(100f)
        inner.row()

        val actions = Table()
        actions.add(confirmBtn).width(600f).height(200f)
        actions.add().width(30f)
        actions.add(backBtn).width(600f).height(200f)
        inner.add(actions).padTop(24f)

        val panel = Table()
        panel.add(inner).pad(24f)

        overlayRoot.add(panel)
        stage.addActor(overlayRoot)
    }

    private fun openSlots(m: Submenu) {
        mode = m
        selectedSlot = -1
        if (m == Submenu.NEW) {
            titleLabel.setText("New Game")
        } else {
            titleLabel.setText("Load Game")
        }
        nameField.isVisible = (m == Submenu.NEW)
        updateSlotLabels()
        overlayRoot.isVisible = true

        stage.root.findActor<Table>("bottomMenu")?.isVisible = false
        stage.root.findActor<Image>("titleImage")?.isVisible = false
    }

    private fun closeSlots() {
        overlayRoot.isVisible = false
        mode = Submenu.NONE
        selectedSlot = -1
        updateSlotLabels()

        stage.root.findActor<Table>("bottomMenu")?.isVisible = true
        stage.root.findActor<Image>("titleImage")?.isVisible = true
    }

    private fun selectSlot(i: Int) {
        if (mode == Submenu.LOAD && !SaveGame.exists(i)) {
            Gdx.app.log("UI", "Slot $i is empty, cannot load.")
            // TODO: show a toast "Slot is empty"
            return
        }

        selectedSlot = i
        updateSlotLabels()
    }

    private fun updateSlotLabels() {
        fun tag(i: Int) = if (selectedSlot == i) "!" else ""

        fun getLabel(slot: Int): String {
            val playerName = SaveGame.getPlayerNameForSlot(slot)

            return if (playerName != null) {
                playerName.take(10)
            } else {
                "Game $slot"
            }
        }

        if (this::slot1.isInitialized) {
            slot1.setText(getLabel(1) + tag(1))
        }
        if (this::slot2.isInitialized) {
            slot2.setText(getLabel(2) + tag(2))
        }
        if (this::slot3.isInitialized) {
            slot3.setText(getLabel(3) + tag(3))
        }

        if (this::confirmBtn.isInitialized) {
            if (mode == Submenu.LOAD) {
                confirmBtn.isDisabled = selectedSlot == -1
            } else {
                confirmBtn.isDisabled = false
            }
        }
    }

    private fun onePixel(color: Color): com.badlogic.gdx.graphics.g2d.TextureRegion {
        val pm = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        return com.badlogic.gdx.graphics.g2d.TextureRegion(t)
    }

    private fun click(block: () -> Unit) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) = block()
    }

    private fun BitmapFont?.disposeSafely() {
        try {
            this?.dispose()
        } catch (_: Throwable) {
        }
    }
}
