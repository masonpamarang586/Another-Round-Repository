package com.anotherround.Consumables

import com.badlogic.gdx.graphics.g2d.TextureRegion

enum class PotionRarity(val label: String) {
    COMMON("Common"),
    RARE("Rare"),
    EPIC("Epic")
}

sealed class Potion(
    open val name: String,
    open val description: String,
    open val rarity: PotionRarity,
    open val textureRegion: TextureRegion
)

data class HealthPotion(
    override val rarity: PotionRarity,
    override val textureRegion: TextureRegion
) : Potion(
    name = "Health Potion",
    description = when (rarity) {
        PotionRarity.COMMON -> "[${rarity.label}] Heals 10 HP."
        PotionRarity.RARE -> "[${rarity.label}] Heals 25 HP."
        PotionRarity.EPIC -> "[${rarity.label}] Heals 50 HP."
    },
    rarity = rarity,
    textureRegion = textureRegion
) {
    val healAmount: Int = when (rarity) {
        PotionRarity.COMMON -> 10
        PotionRarity.RARE -> 25
        PotionRarity.EPIC -> 50
    }
}

data class DefensiveLacquer(
    override val rarity: PotionRarity,
    override val textureRegion: TextureRegion
) : Potion(
    name = "Defense Potion",
    description = when (rarity) {
        PotionRarity.COMMON -> "[${rarity.label}] Blocks 20% of the next attack."
        PotionRarity.RARE -> "[${rarity.label}] Blocks 35% of the next attack."
        PotionRarity.EPIC -> "[${rarity.label}] Blocks 50% of the next attack."
    },
    rarity = rarity,
    textureRegion = textureRegion
) {
    val blockPercent: Float = when (rarity) {
        PotionRarity.COMMON -> 0.20f
        PotionRarity.RARE -> 0.35f
        PotionRarity.EPIC -> 0.50f
    }
}

data class FirePotion(
    override val rarity: PotionRarity,
    override val textureRegion: TextureRegion
) : Potion(
    name = "Fire Potion",
    description = when (rarity) {
        PotionRarity.COMMON -> "[${rarity.label}] Deals 5 damage for 2 rounds."
        PotionRarity.RARE -> "[${rarity.label}] Deals 10 damage for 2 rounds."
        PotionRarity.EPIC -> "[${rarity.label}] Deals 15 damage for 2 rounds."
    },
    rarity = rarity,
    textureRegion = textureRegion
) {
    val damagePerRound: Int = when (rarity) {
        PotionRarity.COMMON -> 5
        PotionRarity.RARE -> 10
        PotionRarity.EPIC -> 15
    }
    val durationRounds: Int = 2
}
