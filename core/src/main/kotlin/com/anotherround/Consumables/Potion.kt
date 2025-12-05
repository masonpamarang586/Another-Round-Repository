package com.anotherround.Consumables

import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class PotionType {
    HEALTH,
    DEFENSE,
    ATTACK // For Liquid Fire (offensive item)
}

enum class PotionRarity {
    COMMON,
    UNCOMMON,
    RARE
}

data class Potion(
    val name: String,
    val description: String,
    val type: PotionType,
    val rarity: PotionRarity,
    val textureRegion: TextureRegion,
    val effectValue: Int, // Heal amount, damage reduction %, or damage amount
    val duration: Int = 0 // For status effects
)
