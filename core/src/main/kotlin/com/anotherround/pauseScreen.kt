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

    /** Scales the pause icon by a fraction of screen height (e.g., 0.10f = 10%). */
    var pauseButtonHeightFraction: Float = 0.05f
        set(value) {
            field = value
            updatePauseButtonBounds()
        }

    /** Margin from the screen edges in UI units. */
    var pauseButtonMargin: Float = 0f
        set(value) {
            field = value
            updatePauseButtonBounds()
        }

    // --- Assets ---
    private val pauseTexture = Texture(Gdx.files.internal("ui/tilemapPause.png")).apply {
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }
    private val pauseIconRegion = TextureRegion(pauseTexture)

    // 1×1 white pixel for dim background veil.
    private val dimmerTex: Texture = run {
        val pm = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pm.setColor(1f, 1f, 1f, 1f)
        pm.fill()
        val t = Texture(pm)
        pm.dispose()
        t
    }

    // Reuse your existing Skin/button visuals.
    private val skin = Skin(Gdx.files.internal("atlas/ui.json"))
    private val style = TextButton.TextButtonStyle().apply {
        font = BitmapFont() // replaced via updateFont()
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
        // Make this style the default so all menu buttons match.
        skin.add("default", style, TextButton.TextButtonStyle::class.java)
        onResize()
    }

    /** Keep the pause UI font in sync with your main UI font (call from FirstScreen.resize). */
    fun updateFont(newFont: BitmapFont) {
        style.font = newFont
    }

    /** Recompute positions/sizes after any viewport change (call from FirstScreen.resize). */
    fun onResize() {
        updatePauseButtonBounds()
        layoutMenu()
    }

    /** Draws the pause icon/menu and handles click input. Call from drawUI() while the batch is active. */
    fun drawAndHandleInput(batch: SpriteBatch) {
        // Draw the pause icon EXACTLY the size of its hitbox.
        batch.draw(
            pauseIconRegion,
            pauseButtonBounds.x,
            pauseButtonBounds.y,
            pauseButtonBounds.width,
            pauseButtonBounds.height
        )

        val justTouched = Gdx.input.justTouched()
        val touch = if (justTouched) uiToWorld(Gdx.input.x, Gdx.input.y) else null

        if (!isPaused) {
            if (touch != null && pauseButtonBounds.contains(touch.x, touch.y)) {
                isPaused = true
            }
            return
        }

        // Dim the background.
        batch.setColor(0f, 0f, 0f, 0.45f)
        batch.draw(dimmerTex, 0f, 0f, uiViewport.worldWidth, uiViewport.worldHeight)
        batch.setColor(Color.WHITE)

        // Draw menu buttons.
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
        pauseTexture.dispose()
        dimmerTex.dispose()
        skin.dispose()
    }

    // --- Internals ---

    private fun updatePauseButtonBounds() {
        // Preserve the source image aspect ratio so draw-size == hitbox-size.
        val srcW = pauseIconRegion.regionWidth.toFloat()
        val srcH = pauseIconRegion.regionHeight.toFloat()

        val height = (uiViewport.worldHeight * pauseButtonHeightFraction).coerceAtLeast(16f)
        val width = height * (srcW / srcH)

        val x = uiViewport.worldWidth  - width - pauseButtonMargin
        val y = uiViewport.worldHeight - height - pauseButtonMargin
        pauseButtonBounds.set(x, y, width, height)
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
    private fun onNewGameClicked() { Gdx.app.log("Pause", "New Game clicked") }
    private fun onSaveGameClicked() { Gdx.app.log("Pause", "Save Game clicked") }
    private fun onSettingsClicked() { Gdx.app.log("Pause", "Settings clicked") }
}
