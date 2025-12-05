package com.anotherround.SaveLoad

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import com.anotherround.Equipment.ArmorPiece
import com.anotherround.Equipment.ArmorSlot
import com.anotherround.Equipment.EquipmentSlots
import com.anotherround.Equipment.Weapon
import com.anotherround.Equipment.WeaponType
import com.anotherround.Consumables.Potion
import com.anotherround.Consumables.PotionType
import com.anotherround.Consumables.PotionRarity
import com.anotherround.combat.StatusEffect
import com.anotherround.combat.EffectType
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.JsonWriter
import kotlin.text.Charsets

data class EffectSnapshot(
    var name: String = "",
    var type: EffectType = EffectType.DEFENSE_BUFF,
    var duration: Int = 0,
    var value: Int = 0
)

data class CharacterSnapshot(
    var name: String = "",
    var level: Int = 1,
    var health: Int = 0,
    var defenseStat: Int = 0,
    var attackStat: Int = 0,
    val currency: Int = 0,
    var activeEffects: List<EffectSnapshot> = emptyList()
) {
    companion object {
        fun from(c: Character, currency: Int = 0) = CharacterSnapshot(
            name = c.name,
            level = c.level,
            health = c.health,
            defenseStat = c.defenseStat,
            attackStat = c.attackStat,
            currency = currency,
            activeEffects = c.activeEffects.map { EffectSnapshot(it.name, it.type, it.duration, it.value) }
        )
    }
}

data class PotionSnapshot(
    var name: String = "",
    var description: String = "",
    var type: PotionType = PotionType.HEALTH,
    var rarity: PotionRarity = PotionRarity.COMMON,
    var effectValue: Int = 0,
    var duration: Int = 0
)

data class ArmorSnapshot(
    var name: String = "",
    var slot: ArmorSlot = ArmorSlot.HELMET,
    var rarity: String = "",
    var defense: Int = 0,
    var health: Int = 0
)

data class WeaponSnapshot(
    var name: String = "",
    var type: WeaponType = WeaponType.SWORD,
    var rarity: String = "",
    var attack: Int = 0
)

data class EquippedSnapshot(
    var weapon: WeaponSnapshot? = null,
    var helmet: ArmorSnapshot? = null,
    var chest: ArmorSnapshot? = null,
    var boots: ArmorSnapshot? = null
)

data class GameState(
    var version: Int = 3,
    var savedAtEpochSec: Long = 0L,
    var player: CharacterSnapshot = CharacterSnapshot(),
    var enemy: CharacterSnapshot = CharacterSnapshot(),
    var roundNumber: Int = 0,
    var inventory: List<PotionSnapshot> = emptyList(),
    var armorInventory: List<ArmorSnapshot> = emptyList(),
    var weaponInventory: List<WeaponSnapshot> = emptyList(),
    var equipped: EquippedSnapshot = EquippedSnapshot()
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
    fun save(
        player: Player,
        enemy: Character,
        roundNumber: Int,
        inventory: List<Potion>,
        armorInventory: List<ArmorPiece>,
        weaponInventory: List<Weapon>,
        equipmentSlots: EquipmentSlots,
        slot: Int = 1
    ) {
        val fh = fileForSlot(slot)

        val potionSnapshots = inventory.map {
            PotionSnapshot(it.name, it.description, it.type, it.rarity, it.effectValue, it.duration)
        }

        val armorSnapshots = armorInventory.map {
            ArmorSnapshot(it.name, it.slot, it.rarity, it.defense, it.health)
        }

        val weaponSnapshots = weaponInventory.map {
            WeaponSnapshot(it.name, it.type, it.rarity, it.attack)
        }

        val equippedSnapshot = EquippedSnapshot(
            weapon = equipmentSlots.weapon?.let { WeaponSnapshot(it.name, it.type, it.rarity, it.attack) },
            helmet = equipmentSlots.helmet?.let { ArmorSnapshot(it.name, it.slot, it.rarity, it.defense, it.health) },
            chest = equipmentSlots.chest?.let { ArmorSnapshot(it.name, it.slot, it.rarity, it.defense, it.health) },
            boots = equipmentSlots.boots?.let { ArmorSnapshot(it.name, it.slot, it.rarity, it.defense, it.health) }
        )

        val state = GameState(
            version = 3,
            savedAtEpochSec = System.currentTimeMillis() / 1000,
            player = CharacterSnapshot.from(player, player.currency),
            enemy = CharacterSnapshot.from(enemy, 0),
            roundNumber = roundNumber,
            inventory = potionSnapshots,
            armorInventory = armorSnapshots,
            weaponInventory = weaponSnapshots,
            equipped = equippedSnapshot
        )

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
