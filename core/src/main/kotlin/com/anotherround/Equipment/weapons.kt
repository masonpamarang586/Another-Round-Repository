package com.anotherround.Equipment

import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class WeaponType {
    SWORD,
    MACE,
    BOW,
    STAFF
}

/** One weapon item. */
data class Weapon(
    val name: String,
    val type: WeaponType,
    val icon: TextureRegion,
    val rarity: String,
    val attack: Int       // damage
)

enum class WeaponSprite(val row: Int, val col: Int) {
    // Swords
    STONE_SWORD(94, 0),
    GOLD_SWORD(99, 0),
    DIAMOND_SWORD(104, 0),

    // Maces
    STONE_MACE(94, 1),
    GOLD_MACE(99, 1),
    DIAMOND_MACE(104, 1),

    // Bows
    STONE_BOW(94, 10),
    GOLD_BOW(99, 10),
    DIAMOND_BOW(104, 10),

    // Staves
    STONE_STAFF(93, 7),
    GOLD_STAFF(98, 7),
    DIAMOND_STAFF(103, 7)
}

data class WeaponBlueprint(
    val name: String,
    val type: WeaponType,
    val sprite: WeaponSprite,
    val rarity: String,
    val attack: Int
)

val DEFAULT_WEAPON_BLUEPRINTS = listOf(
    // Swords
    WeaponBlueprint("Stone Sword",   WeaponType.SWORD, WeaponSprite.STONE_SWORD,   "Common",   5),
    WeaponBlueprint("Gold Sword",    WeaponType.SWORD, WeaponSprite.GOLD_SWORD,    "Uncommon",    9),
    WeaponBlueprint("Diamond Sword", WeaponType.SWORD, WeaponSprite.DIAMOND_SWORD, "Rare", 13),

    // Maces
    WeaponBlueprint("Stone Mace",    WeaponType.MACE,  WeaponSprite.STONE_MACE,    "Common",   6),
    WeaponBlueprint("Gold Mace",     WeaponType.MACE,  WeaponSprite.GOLD_MACE,     "Uncommon",    10),
    WeaponBlueprint("Diamond Mace",  WeaponType.MACE,  WeaponSprite.DIAMOND_MACE,  "Rare", 14),

    // Bows
    WeaponBlueprint("Stone Bow",     WeaponType.BOW,   WeaponSprite.STONE_BOW,     "Common",   4),
    WeaponBlueprint("Gold Bow",      WeaponType.BOW,   WeaponSprite.GOLD_BOW,      "Uncommon",    8),
    WeaponBlueprint("Diamond Bow",   WeaponType.BOW,   WeaponSprite.DIAMOND_BOW,   "Rare", 12),

    // Staves
    WeaponBlueprint("Stone Staff",   WeaponType.STAFF, WeaponSprite.STONE_STAFF,   "Common",   3),
    WeaponBlueprint("Gold Staff",    WeaponType.STAFF, WeaponSprite.GOLD_STAFF,    "Uncommon",    7),
    WeaponBlueprint("Diamond Staff", WeaponType.STAFF, WeaponSprite.DIAMOND_STAFF, "Rare", 11)
)

fun buildDefaultWeapons(cells: Array<Array<TextureRegion>>): List<Weapon> {
    return DEFAULT_WEAPON_BLUEPRINTS.map { bp ->
        val region = cells[bp.sprite.row][bp.sprite.col]
        Weapon(
            name = bp.name,
            type = bp.type,
            icon = region,
            rarity = bp.rarity,
            attack = bp.attack
        )
    }
}
