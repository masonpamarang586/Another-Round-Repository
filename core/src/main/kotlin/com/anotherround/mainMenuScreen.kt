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

    // --- ADD THIS ENTIRE FUNCTION ---
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
        fun styleFor(f: BitmapFont) = TextButton.TextButtonStyle().apply {
            font = this@MainMenuScreen.font
            fontColor = Color.BLACK
            up = skin.getDrawable("button-normal")
            down = skin.getDrawable("button-normal-pressed")
            over = skin.getDrawable("button-normal-over")
        }
        val s = styleFor(font)
        if (this::newGameBtn.isInitialized) newGameBtn.style = s
        if (this::loadGameBtn.isInitialized) loadGameBtn.style = s
        if (this::settingsBtn.isInitialized) settingsBtn.style = s
        if (this::slot1.isInitialized) slot1.style = s
        if (this::slot2.isInitialized) slot2.style = s
        if (this::slot3.isInitialized) slot3.style = s
        if (this::confirmBtn.isInitialized) confirmBtn.style = s
        if (this::backBtn.isInitialized) backBtn.style = s
        if (this::nameField.isInitialized) nameField.style.font = font
    }

    private fun buildMainButtons() {
        val titleImage = Image(titleTexture)
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
        // 1. Create the Image widget
        val titleImage = Image(titleTexture)

        // 2. Give it a name so we can find it
        titleImage.name = "titleImage"

        // 3. Add it directly to the stage
        stage.addActor(titleImage)
    }

    private fun layoutTitle() {
        val titleImage = stage.root.findActor<Image>("titleImage") ?: return

        val scale = 2.25f
        titleImage.setScale(scale)

        // 2. Position this table at the TOP-CENTER of the screen
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
            // Use the font that was loaded in the show() method
            this.font = this@MainMenuScreen.font
            this.fontColor = Color.BLACK

            this.messageFontColor = Color.BLACK
            this.font.data.setScale(1.0f)

            // Use your 'onePixel' helper to create drawables for the style
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

                    // 1. Create the session object
                    val newSession = GameSession(selectedSlot, playerName)

                    // 2. Call the new function on BattleScreen
                    game.getScreen<BattleScreen>().startNewGame(newSession)

                    // 3. Switch screens
                    game.setScreen<BattleScreen>()

                    // --- ADD THIS ENTIRE BLOCK ---
                } else if (mode == Submenu.LOAD) {
                    // 1. Try to load the game from the selected slot
                    val loadedState = SaveGame.loadOrNull(selectedSlot)

                    if (loadedState != null) {
                        // 2. Success! Call the other new function on BattleScreen
                        game.getScreen<BattleScreen>().loadSavedGame(loadedState, selectedSlot)

                        // 3. Switch screens
                        game.setScreen<BattleScreen>()
                    } else {
                        // This shouldn't happen if we disable the button,
                        // but it's good error handling.
                        Gdx.app.log("UI", "Load failed! File for slot $selectedSlot might be corrupt or empty.")
                        // TODO: show toast "LOAD FAILED"
                    }
                }
                // --- END OF BLOCK TO ADD ---

                closeSlots() // Close the overlay
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
    }

    private fun closeSlots() {
        overlayRoot.isVisible = false
        mode = Submenu.NONE
        selectedSlot = -1
        updateSlotLabels()
    }

    private fun selectSlot(i: Int) {
        if (mode == Submenu.LOAD && !SaveGame.exists(i)) {
            Gdx.app.log("UI", "Slot $i is empty, cannot load.")
            // TODO: show a toast "Slot is empty"
            return // Don't select the slot
        }

        selectedSlot = i
        updateSlotLabels()
    }

    private fun updateSlotLabels() {
        fun tag(i: Int) = if (selectedSlot == i) "!" else ""

        fun getLabel(slot: Int): String {
            val state = SaveGame.loadOrNull(slot)

            return if (state != null) {
                val playerName = state.player.name.take(10)
                "$playerName"
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
