package com.anotherround.SaveLoad

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import com.anotherround.CharacterClasses.Enemy
import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import java.time.Instant

data class CharacterSnapshot(
    val name: String,
    val level: Int,
    val health: Int,
    val defenseStat: Int,
    val attackStat: Int
) {
    companion object {
        fun from(c: Character) = CharacterSnapshot(
            name = c.name,
            level = c.level,
            health = c.health,
            defenseStat = c.defenseStat,
            attackStat = c.attackStat
        )
    }
}

data class GameState(
    val version: Int = 1,
    val savedAtEpochSec: Long = Instant.now().epochSecond,
    val player: CharacterSnapshot,
    val enemy: CharacterSnapshot
)

/**
 * Save/Load helper. By default we use:
 * - ANDROID & DESKTOP: external storage (readable outside the app)
 * - iOS: local storage (external isn't supported)
 */
object SaveGame {
    private const val DIR = "AnotherRound/saves"
    private const val FILE_PREFIX = "slot"
    private const val FILE_EXT = ".json"

    private val json: Json = Json().apply {
        setOutputType(JsonWriter.OutputType.json)
        setUsePrototypes(false)
        setSortFields(true)
        setQuoteLongValues(true)
        prettyPrint(true)
    }

    // Where to store saves depending on platform
    private fun storageRoot(): FileHandle {
        return when (Gdx.app.type) {
            Application.ApplicationType.Android,
            Application.ApplicationType.Desktop -> Gdx.files.external(DIR)
            else -> Gdx.files.local(DIR) // iOS, WebGL fallback to local
        }
    }

    private fun fileForSlot(slot: Int): FileHandle {
        val dir = storageRoot()
        if (!dir.exists()) dir.mkdirs()
        return dir.child("$FILE_PREFIX$slot$FILE_EXT")
    }

    // Save current state to the given slot (default slot 1)
    @Synchronized
    fun save(player: Player, enemy: Enemy, slot: Int = 1) {
        val fh = fileForSlot(slot)
        val state = GameState(
            player = CharacterSnapshot.from(player),
            enemy = CharacterSnapshot.from(enemy)
        )
        val text = json.prettyPrint(state)
        fh.writeString(text, false, Charsets.UTF_8.name())
        Gdx.app.log("SAVE", "Saved slot $slot -> ${fh.path()}")
    }

    // Load state from slot or return null if missing/corrupt
    @Synchronized
    fun loadOrNull(slot: Int = 1): GameState? {
        val fh = fileForSlot(slot)
        if (!fh.exists()) return null
        return try {
            json.fromJson(GameState::class.java, fh.reader(Charsets.UTF_8.name()))
        } catch (t: Throwable) {
            Gdx.app.error("SAVE", "Failed to load slot $slot", t)
            null
        }
    }

    fun exists(slot: Int = 1): Boolean = fileForSlot(slot).exists()
    fun delete(slot: Int = 1): Boolean {
        val fh = fileForSlot(slot)
        return if (fh.exists()) fh.delete() else false
    }
}
