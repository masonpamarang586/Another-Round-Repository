package com.anotherround

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.graphics.g2d.NinePatch
import ktx.app.KtxScreen
import ktx.graphics.use

class MainMenuScreen(private val game: Main) : KtxScreen {

    // Static background
    private lateinit var bgTexture: Texture

    // UI
    // --- FIX 1: Use the game's UI viewport and batch ---
    // The stage MUST use the *exact same* viewport and batch instances
    // from the Main class. Your original code created a *new* ScreenViewport
    // which conflicted with Main's FitViewport, causing the crash.
    private val stage = Stage(game.uiViewport, game.batch)
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

    private enum class Submenu { NONE, NEW, LOAD }

    override fun show() {
        // --- FIX 2: Add the background as an Image actor to the stage ---
        // This is much cleaner. The stage will now manage rendering the
        // background using the correct viewport and batch.
        bgTexture = Texture(Gdx.files.internal("ui/title_screen_bg.png"))
        val bgImage = Image(bgTexture)
        bgImage.setSize(game.uiViewport.worldWidth, game.uiViewport.worldHeight)
        bgImage.setPosition(0f, 0f)
        stage.addActor(bgImage)
        bgImage.toBack() // Ensure it's drawn behind UI elements

        // --- Font and Skin Setup (This is slow, but was in your original) ---
        // This is okay for now, but for smoother loading, you should
        // use an AssetManager in your Main class to load fonts/textures.
        val generator = FreeTypeFontGenerator(Gdx.files.internal("fonts/NES-FC.ttf"))
        val parameter = FreeTypeFontGenerator.FreeTypeFontParameter()
        parameter.size = 32
        parameter.color = Color.BLACK
        val font = generator.generateFont(parameter)
        generator.dispose()

        skin = Skin()
        skin.add("default-font", font)

        // --- Use a 9-patch for resizable backgrounds ---
        // This is better than a 1x1 pixel for window/panel backgrounds
        val pixel = onePixel(Color(0.1f, 0.1f, 0.1f, 0.8f))
        val patch = NinePatch(pixel, 4, 4, 4, 4)
        skin.add("panel-bg", NinePatchDrawable(patch))
        skin.add("pixel", pixel) // For button backgrounds

        val btnStyle = TextButton.TextButtonStyle()
        btnStyle.font = font
        btnStyle.fontColor = Color.WHITE
        btnStyle.overFontColor = Color.LIGHT_GRAY
        btnStyle.downFontColor = Color.GREEN
        // Use the 1x1 pixel as a drawable background
        val pixelDrawable = TextureRegionDrawable(skin.getRegion("pixel"))
        btnStyle.up = pixelDrawable.tint(Color(0.3f, 0.3f, 0.3f, 0.7f))
        btnStyle.over = pixelDrawable.tint(Color(0.5f, 0.5f, 0.5f, 0.7f))
        btnStyle.down = pixelDrawable.tint(Color(0.2f, 0.6f, 0.2f, 0.7f))
        skin.add("default", btnStyle)

        val labelStyle = Label.LabelStyle(font, Color.WHITE)
        skin.add("default", labelStyle)

        val fieldStyle = TextField.TextFieldStyle()
        fieldStyle.font = font
        fieldStyle.fontColor = Color.BLACK
        fieldStyle.background = pixelDrawable.tint(Color(0.8f, 0.8f, 0.8f, 1.0f))
        fieldStyle.cursor = pixelDrawable.tint(Color.BLUE)
        fieldStyle.selection = pixelDrawable.tint(Color(0.5f, 0.5f, 1f, 0.5f))
        skin.add("default", fieldStyle)

        // --- Main Buttons ---
        val mainButtonTable = Table()
        mainButtonTable.setFillParent(true)
        mainButtonTable.center().bottom() // Position at bottom-center

        newGameBtn = TextButton("New Game", skin)
        loadGameBtn = TextButton("Load Game", skin)
        settingsBtn = TextButton("Settings", skin)

        val buttonWidth = 400f
        val buttonHeight = 80f
        val buttonSpacing = 20f

        mainButtonTable.add(newGameBtn).width(buttonWidth).height(buttonHeight).space(buttonSpacing)
        mainButtonTable.row()
        mainButtonTable.add(loadGameBtn).width(buttonWidth).height(buttonHeight).space(buttonSpacing)
        mainButtonTable.row()
        mainButtonTable.add(settingsBtn).width(buttonWidth).height(buttonHeight).space(buttonSpacing)
        mainButtonTable.padBottom(100f) // Add padding from the bottom edge

        stage.addActor(mainButtonTable)

        // --- Overlay for New/Load ---
        overlayRoot = Table()
        overlayRoot.setFillParent(true)
        overlayRoot.setBackground(skin.getDrawable("panel-bg")) // Use the 9-patch
        overlayRoot.isVisible = false
        stage.addActor(overlayRoot)

        titleLabel = Label("Slots", skin)
        slot1 = TextButton("Game 1", skin)
        slot2 = TextButton("Game 2", skin)
        slot3 = TextButton("Game 3", skin)
        nameField = TextField("", skin)
        confirmBtn = TextButton("Start", skin)
        backBtn = TextButton("Back", skin)

        overlayRoot.add(titleLabel).colspan(2).pad(20f)
        overlayRoot.row()
        overlayRoot.add(slot1).width(300f).height(60f).pad(10f).colspan(2)
        overlayRoot.row()
        overlayRoot.add(slot2).width(300f).height(60f).pad(10f).colspan(2)
        overlayRoot.row()
        overlayRoot.add(slot3).width(300f).height(60f).pad(10f).colspan(2)
        overlayRoot.row()
        overlayRoot.add(Label("Name:", skin)).right().pad(10f)
        overlayRoot.add(nameField).width(300f).height(60f).pad(10f).left()
        overlayRoot.row()
        overlayRoot.add(backBtn).width(200f).height(60f).pad(20f)
        overlayRoot.add(confirmBtn).width(200f).height(60f).pad(20f)

        // --- Add Listeners ---
        newGameBtn.addListener(click { openSlots(Submenu.NEW) })
        loadGameBtn.addListener(click { openSlots(Submenu.LOAD) })
        settingsBtn.addListener(click {
            // TODO: Open settings
        })
        backBtn.addListener(click { closeSlots() })
        /*confirmBtn.addListener(click {
            if (selectedSlot != -1) {
                val slot = "game$selectedSlot.sav"
                val name = if (mode == Submenu.NEW) nameField.text else "Player" // Get name if new game
                game.addScreen(MainScreen(game, slot, name, mode == Submenu.NEW))
                game.setScreen<MainScreen>()
                // We dispose this screen *after* transitioning
                this.dispose()
            }
        }) */
        slot1.addListener(click { selectSlot(1) })
        slot2.addListener(click { selectSlot(2) })
        slot3.addListener(click { selectSlot(3) })

        updateSlotLabels()
        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // --- FIX 3: Update viewport and let the stage draw everything ---
        // We must update the viewport every frame in case of resize.
        // The stage will now draw the background AND the UI
        // using the correct batch and viewport.
        game.uiViewport.apply()
        stage.act(delta)
        stage.draw()
    }

    // --- FIX 4: Add a resize method ---
    // This tells the stage's viewport (game.uiViewport) how to update
    // when the window size changes.
    override fun resize(width: Int, height: Int) {
        game.uiViewport.update(width, height, true)
    }

    override fun hide() {
        Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        // Dispose assets loaded by this screen
        bgTexture.dispose()
        stage.dispose()
        skin.dispose() // This will dispose the font and pixel texture
    }

    private fun openSlots(m: Submenu) {
        mode = m
        selectedSlot = -1
        if (m == Submenu.NEW) {
            titleLabel.setText("New Game - Choose a Slot & Name")
        } else {
            titleLabel.setText("Load Game - Choose a Slot")
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
        fun tag(i: Int) = if (selectedSlot == i) " [selected]" else ""
        slot1.setText("Game 1" + tag(1))
        slot2.setText("Game 2" + tag(2))
        slot3.setText("Game 3" + tag(3))
    }

    private fun onePixel(color: Color): com.badlogic.gdx.graphics.g2d.TextureRegion {
        val pm = com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888)
        pm.setColor(color)
        pm.fill()
        // The texture will be added to the skin, which manages its disposal
        val t = Texture(pm)
        pm.dispose()
        return com.badlogic.gdx.graphics.g2d.TextureRegion(t)
    }

    private fun click(block: () -> Unit) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            block()
        }
    }
}
