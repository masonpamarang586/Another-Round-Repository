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

class MainMenuScreen(private val game: Main) : KtxScreen {

    // Static background
    private lateinit var bgTexture: Texture

    // UI
    private val stage = Stage(ScreenViewport(game.uiViewport.camera))
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

        buildMainButtons()
        buildSlotOverlay()

        layoutBottomMenu()
        stage.addActor(overlayRoot)
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
    }

    // ---- Helpers ----
    private fun loadStaticBackground(path: String) {
        bgTexture = Texture(Gdx.files.internal(path)).apply {
            setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
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
            font = f
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
        table.add(newGameBtn).width(600f).height(200f)
        table.row()
        table.add(loadGameBtn).padTop(40f).width(600f).height(200f)
        table.row()
        table.add(settingsBtn).padTop(40f).width(600f).height(200f)
        stage.addActor(table)
        table.name = "bottomMenu"
    }

    private fun layoutBottomMenu() {
        val table = stage.root.findActor<Table>("bottomMenu") ?: return
        table.setPosition(
            Gdx.graphics.width / 2f,
            Gdx.graphics.height * 0.10f
        )
        table.bottom()
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

            // Use your 'onePixel' helper to create drawables for the style
            val bg = TextureRegionDrawable(onePixel(Color(0.8f, 0.8f, 0.8f, 1.0f)))
            this.background = bg
            this.cursor = TextureRegionDrawable(onePixel(Color.BLACK))
            this.selection = TextureRegionDrawable(onePixel(Color(0.5f, 0.5f, 1f, 0.5f)))
        }

        // 2. Now, create the TextField by passing in the style *we just made*.
        nameField = TextField("", textFieldStyle).apply {
            messageText = "Enter name..."
        }


        confirmBtn = TextButton("Confirm", s).apply {
            addListener(click {
                when (mode) {
                    Submenu.LOAD -> {
                        if (selectedSlot != -1) {
                            Gdx.app.log("Menu", "Loading Game $selectedSlot")
                        } else {
                            Gdx.app.log("Menu", "Pick a slot")
                        }
                    }
                    Submenu.NEW -> {
                        if (selectedSlot == -1) {
                            Gdx.app.log("Menu", "Pick a slot")
                        } else if (nameField.text.isBlank()) {
                            Gdx.app.log("Menu", "Enter a name")
                        } else {
                            Gdx.app.log("Menu", "New Game $selectedSlot named ${nameField.text}")
                        }
                    }
                    else -> { }
                }
            })
        }

        backBtn = TextButton("Back", s).apply {
            addListener(click { closeSlots() })
        }

        val inner = Table()
        inner.add(titleLabel).padBottom(24f)
        inner.row()
        inner.add(slot1).width(400f).height(200f)
        inner.row()
        inner.add(slot2).padTop(24f).width(400f).height(200f)
        inner.row()
        inner.add(slot3).padTop(36f).width(400f).height(200f)
        inner.row()
        inner.add(nameField).padTop(36f).width(600f).height(100f)
        inner.row()

        val actions = Table()
        actions.add(confirmBtn).width(400f).height(160f)
        actions.add().width(30f)
        actions.add(backBtn).width(400f).height(160f)
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
        selectedSlot = i
        updateSlotLabels()
    }

    private fun updateSlotLabels() {
        fun tag(i: Int) = if (selectedSlot == i) " ^" else ""
        slot1.setText("Game 1" + tag(1))
        slot2.setText("Game 2" + tag(2))
        slot3.setText("Game 3" + tag(3))
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
