package com.anotherround.Equipment

import com.badlogic.gdx.graphics.g2d.TextureRegion

/** Where a particular armor piece is worn. */
enum class ArmorSlot {
    HELMET,
    CHEST,
    BOOTS
}

/** One armor item (name + slot + icon from the sprite sheet). */
data class ArmorPiece(
    val name: String,
    val slot: ArmorSlot,
    val icon: TextureRegion,
    val rarity: String,
    val defense: Int,
    val health: Int
)

/**
 * All equipment the character can have on at once.
 *
 * NOTE: weapon is defined in Weapons.kt but can be referenced here
 * because it's in the same package.
 */
data class EquipmentSlots(
    var weapon: Weapon? = null,
    var helmet: ArmorPiece? = null,
    var chest: ArmorPiece? = null,
    var boots: ArmorPiece? = null
)

enum class ArmorSprite(val row: Int, val col: Int) {
    BRONZE_HELMET(119, 4),
    BRONZE_CHEST(120, 4),
    BRONZE_BOOTS(121, 4),

    IRON_HELMET(119, 5),
    IRON_CHEST(120, 5),
    IRON_BOOTS(121, 5),

    GOLD_HELMET(119, 6),
    GOLD_CHEST(120, 6),
    GOLD_BOOTS(121, 6),

    // add more armor pieces here later if you want
}

data class ArmorBlueprint(
    val name: String,
    val slot: ArmorSlot,
    val sprite: ArmorSprite,
    val rarity: String,
    val defense: Int,
    val health: Int
)

/** All the default armor pieces. */
val DEFAULT_ARMOR_BLUEPRINTS = listOf(
    // Bronze set
    ArmorBlueprint("Bronze Helmet",      ArmorSlot.HELMET, ArmorSprite.BRONZE_HELMET, "Common",   2,  5),
    ArmorBlueprint("Bronze Chestplate",  ArmorSlot.CHEST,  ArmorSprite.BRONZE_CHEST,  "Common",   4, 10),
    ArmorBlueprint("Bronze Boots",       ArmorSlot.BOOTS,  ArmorSprite.BRONZE_BOOTS,  "Common",   1,  3),

    // Iron set
    ArmorBlueprint("Iron Helmet",        ArmorSlot.HELMET, ArmorSprite.IRON_HELMET,   "Uncommon", 4,  8),
    ArmorBlueprint("Iron Chestplate",    ArmorSlot.CHEST,  ArmorSprite.IRON_CHEST,    "Uncommon", 8, 15),
    ArmorBlueprint("Iron Boots",         ArmorSlot.BOOTS,  ArmorSprite.IRON_BOOTS,    "Uncommon", 2,  6),

    // Gold set
    ArmorBlueprint("Gold Helmet",        ArmorSlot.HELMET, ArmorSprite.GOLD_HELMET,   "Rare",     6, 12),
    ArmorBlueprint("Gold Chestplate",    ArmorSlot.CHEST,  ArmorSprite.GOLD_CHEST,    "Rare",    12, 20),
    ArmorBlueprint("Gold Boots",         ArmorSlot.BOOTS,  ArmorSprite.GOLD_BOOTS,    "Rare",     3,  8)
)

/**
 * Build the list of actual ArmorPiece objects, given the split sprite sheet.
 *
 * Usage (in BattleScreen):
 *   val cells = TextureRegion.split(armorTexture, 64, 64)
 *   armorInventory.addAll(buildDefaultArmor(cells))
 */
fun buildDefaultArmor(cells: Array<Array<TextureRegion>>): List<ArmorPiece> {
    return DEFAULT_ARMOR_BLUEPRINTS.map { bp ->
        val region = cells[bp.sprite.row][bp.sprite.col]
        ArmorPiece(
            name = bp.name,
            slot = bp.slot,
            icon = region,
            rarity = bp.rarity,
            defense = bp.defense,
            health = bp.health
        )
    }
}
