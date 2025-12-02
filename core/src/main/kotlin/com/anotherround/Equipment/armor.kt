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
    val icon: TextureRegion
)

/** Convenience holder for what the character has equipped. */
data class EquipmentSlots(
    var helmet: ArmorPiece? = null,
    var chest: ArmorPiece? = null,
    var boots: ArmorPiece? = null
)

enum class ArmorSprite(val row: Int, val col: Int) {
    BRONZE_HELMET(0, 0),
    BRONZE_CHEST(0, 1),
    BRONZE_BOOTS(0, 2),

    IRON_HELMET(1, 0),
    IRON_CHEST(1, 1),
    IRON_BOOTS(1, 2),

    GOLD_HELMET(2, 0),
    GOLD_CHEST(2, 1),
    GOLD_BOOTS(2, 2),

    // ...later: add any others you want from the 5000
}
