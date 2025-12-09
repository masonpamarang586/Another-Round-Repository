package com.anotherround.SaveLoad

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import kotlin.text.Charsets

data class CharacterSnapshot(
    var name: String = "",
    var level: Int = 1,
    var health: Int = 0,
    var defenseStat: Int = 0,
    var attackStat: Int = 0,
    val currency: Int = 0
) {
    companion object {
        fun from(c: Character, currency: Int = 0) = CharacterSnapshot(
            name = c.name,
            level = c.level,
            health = c.health,
            defenseStat = c.defenseStat,
            attackStat = c.attackStat,
            currency = currency
        )
    }
}

data class PotionData(
    val type: String = "", // "Health", "Defense", "Fire"
    val rarity: String = "Common"
)

data class ArmorData(
    val name: String = "",
    val slot: String = "", // "HELMET", "CHEST", ...
    val rarity: String = "Common",
    val defense: Int = 0,
    val health: Int = 0
)

data class WeaponData(
    val name: String = "",
    val type: String = "", // "SWORD", "MACE", ...
    val rarity: String = "Common",
    val attack: Int = 0
)

data class GameState(
    var version: Int = 2,
    var savedAtEpochSec: Long = 0L,
    var roundNumber: Int = 0,
    var enemyKind: String = "RedGrunt", // Store the enum name
    
    var player: CharacterSnapshot = CharacterSnapshot(),
    var enemy: CharacterSnapshot = CharacterSnapshot(),
    
    // Inventory
    var potions: List<PotionData> = emptyList(),
    var inventoryArmor: List<ArmorData> = emptyList(),
    var inventoryWeapons: List<WeaponData> = emptyList(),
    
    // Equipped
    var equippedHelmet: ArmorData? = null,
    var equippedChest: ArmorData? = null,
    var equippedBoots: ArmorData? = null,
    var equippedWeapon: WeaponData? = null
)

/**
 * Save/Load helper.
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

    private fun storageRoot(): FileHandle {
        return Gdx.files.local(DIR)
    }

    private fun fileForSlot(slot: Int): FileHandle {
        val dir = storageRoot()
        if (!dir.exists()) dir.mkdirs()
        return dir.child("$FILE_PREFIX$slot$FILE_EXT")
    }

    @Synchronized
    fun save(state: GameState, slot: Int = 1) {
        val fh = fileForSlot(slot)
        state.savedAtEpochSec = System.currentTimeMillis() / 1000
        val text = json.prettyPrint(state)
        fh.writeString(text, false, Charsets.UTF_8.name())
        Gdx.app.log("SAVE", "Saved slot $slot -> ${fh.path()}")
    }

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

    fun getPlayerNameForSlot(slot: Int): String? {
        val state = loadOrNull(slot)
        return state?.player?.name
    }
}
