package com.anotherround

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.utils.viewport.Viewport

class PauseScreenUI(private val uiViewport: Viewport) {

    /** Make the pause icon scale across devices. 0.08f = 8% of screen height. */
    var pauseButtonHeightFraction: Float = 0.08f
        set(value) {
            field = value
            updatePauseButtonBounds()
        }

    /** Margin from the screen edges in UI units. */
    var pauseButtonMargin: Float = 12f
        set(value) {
            field = value
            updatePauseButtonBounds()
        }

    // --- Assets ---
    private val tileTexture = Texture(Gdx.files.internal("ui/tilemap.png")).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    // Left half for panel, right half for the pause icon (adjust these if needed).
    private val panelRegion: TextureRegion
    private val pauseIconRegion: TextureRegion

    // A 1×1 white pixel we tint to draw the dim background veil (instead of reusing tilemap.png).
    private val dimmerTex: Texture = run {
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pm.setColor(1f, 1f, 1f, 1f)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        t
    }

    // Reuse your existing Skin style so it matches Attack/Items.
    private val skin = Skin(Gdx.files.internal("atlas/ui.json"))
    private val style = TextButton.TextButtonStyle().apply {
        font = BitmapFont() // will be replaced by updateFont()
        fontColor = Color.BLACK
        up   = skin.getDrawable("button-normal")
        down = skin.getDrawable("button-normal-pressed")
        over = skin.getDrawable("button-normal-over")
    }

    private val resumeButton   = TextButton("Resume", style)
    private val newGameButton  = TextButton("New Game", style)
    private val saveGameButton = TextButton("Save Game", style)
    private val settingsButton = TextButton("Settings", style)

    private val pauseButtonBounds = Rectangle()
    private val panelBounds = Rectangle()
    private val tmpVec = Vector3()
    private val tmpRect = Rectangle()

    var isPaused: Boolean = false
        private set

    init {
        // Split the source image once.
        val w = tileTexture.width
        val h = tileTexture.height
        panelRegion = TextureRegion(tileTexture, 0, 0, w / 2, h)               // left half
        pauseIconRegion = TextureRegion(tileTexture, 2 * w / 5, 0, w, h)   // right half

        // Make our style the default in the skin (optional but handy).
        skin.add("default", style, TextButton.TextButtonStyle::class.java)

        onResize()
    }

    /** Keep the pause UI font in sync with your main UI font (call in FirstScreen.resize). */
    fun updateFont(newFont: BitmapFont) {
        style.font = newFont
    }

    /** Recompute positions/sizes after any viewport change (call in FirstScreen.resize). */
    fun onResize() {
        updatePauseButtonBounds()
        layoutMenu()
    }

    /** Draws the pause icon/menu and handles click input. Call from drawUI() while the batch is active. */
    fun drawAndHandleInput(batch: SpriteBatch) {
        // Always draw the pause icon in the top-right
        batch.draw(pauseIconRegion, pauseButtonBounds.x, pauseButtonBounds.y, pauseButtonBounds.width, pauseButtonBounds.height)

        val justTouched = Gdx.input.justTouched()
        val touch = if (justTouched) uiToWorld(Gdx.input.x, Gdx.input.y) else null

        if (!isPaused) {
            if (touch != null && pauseButtonBounds.contains(touch.x, touch.y)) {
                isPaused = true
            }
            return
        }

        // Dim the background with a translucent black veil (not tilemap.png).
        batch.setColor(0f, 0f, 0f, 0.45f)
        batch.draw(dimmerTex, 0f, 0f, uiViewport.worldWidth, uiViewport.worldHeight)
        batch.setColor(Color.WHITE)

        // Panel background uses the left half of tilemap.png
        // batch.draw(panelRegion, panelBounds.x, panelBounds.y, panelBounds.width, panelBounds.height)

        resumeButton.draw(batch, 1f)
        newGameButton.draw(batch, 1f)
        saveGameButton.draw(batch, 1f)
        settingsButton.draw(batch, 1f)

        if (touch != null) {
            when {
                boundsOf(resumeButton).contains(touch.x, touch.y)   -> onResumeClicked()
                boundsOf(newGameButton).contains(touch.x, touch.y)  -> onNewGameClicked()
                boundsOf(saveGameButton).contains(touch.x, touch.y) -> onSaveGameClicked()
                boundsOf(settingsButton).contains(touch.x, touch.y) -> onSettingsClicked()
            }
        }
    }

    fun dispose() {
        tileTexture.dispose()
        dimmerTex.dispose()
        skin.dispose()
    }

    // --- Internals ---

    private fun updatePauseButtonBounds() {
        // Scale icon by a fraction of the current screen height so it looks right on phones & desktop.
        val size = (uiViewport.worldHeight * pauseButtonHeightFraction).coerceAtLeast(24f)
        val x = uiViewport.worldWidth  - size - pauseButtonMargin
        val y = uiViewport.worldHeight - size - pauseButtonMargin
        pauseButtonBounds.set(x, y, size, size)
    }

    private fun layoutMenu() {
        val menuWidth = uiViewport.worldWidth.coerceAtMost(600f)
        val padding = 24f
        val buttonHeight = uiViewport.worldHeight * 0.08f
        val spacing = uiViewport.worldHeight * 0.02f

        val totalHeight = padding * 2 + buttonHeight * 4 + spacing * 3
        val x = (uiViewport.worldWidth  - menuWidth)  / 2f
        val y = (uiViewport.worldHeight - totalHeight) / 2f
        panelBounds.set(x, y, menuWidth, totalHeight)

        val buttonWidth = menuWidth - padding * 2
        var by = y + totalHeight - padding - buttonHeight

        fun place(b: TextButton) {
            b.setBounds(x + padding, by, buttonWidth, buttonHeight)
            by -= (buttonHeight + spacing)
        }

        place(resumeButton)
        place(newGameButton)
        place(saveGameButton)
        place(settingsButton)
    }

    private fun uiToWorld(screenX: Int, screenY: Int): Vector3 {
        tmpVec.set(screenX.toFloat(), screenY.toFloat(), 0f)
        uiViewport.unproject(tmpVec)
        return tmpVec
    }

    private fun boundsOf(b: TextButton): Rectangle = tmpRect.set(b.x, b.y, b.width, b.height)

    private fun onResumeClicked() { isPaused = false }
    private fun onNewGameClicked() { Gdx.app.log("Pause", "New Game clicked") /* TODO wire game reset */ }
    private fun onSaveGameClicked() { Gdx.app.log("Pause", "Save Game clicked") /* TODO wire save */ }
    private fun onSettingsClicked() { Gdx.app.log("Pause", "Settings clicked") /* TODO open settings */ }
}
