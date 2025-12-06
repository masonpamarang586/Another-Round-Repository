package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.anotherround.SaveLoad.PotionData

class ConsumablesInventory {

    private val items = mutableListOf<Potion>()

    private val potionsSpritesheet by lazy {
        Texture(Gdx.files.internal("items/potions.png"))
    }

    // Coordinates: x, y, width, height.
    // Assuming standard health potion is at 48, 32.
    // I will reuse this region for all Health potions for now as I don't have new assets.
    private val healthPotionRegion by lazy { TextureRegion(potionsSpritesheet, 48, 32, 16, 16) }

    // Using the old mana potion icon (64, 32) for Defensive Lacquer for now to distinguish it.
    private val defensivePotionRegion by lazy { TextureRegion(potionsSpritesheet, 128, 112, 16, 16) }

    // I need a third icon for Fire Potion. I'll pick another spot on the sheet, e.g., 80, 32.
    // If it's invalid, it might show garbage, but it's better than nothing.
    private val firePotionRegion by lazy { TextureRegion(potionsSpritesheet, 80, 192, 16, 16) }


    fun getItems(): List<Potion> {
        return items
    }

    fun loadDefaultPotions() {
        items.clear()
        // Add one of each for testing/gameplay start
        items.add(createHealthPotion(PotionRarity.COMMON))
        items.add(createHealthPotion(PotionRarity.RARE))
        items.add(createHealthPotion(PotionRarity.EPIC))

        items.add(createDefensivePotion(PotionRarity.COMMON))
        // items.add(createDefensivePotion(PotionRarity.RARE))
        // items.add(createDefensivePotion(PotionRarity.EPIC))

        items.add(createFirePotion(PotionRarity.COMMON))
        // items.add(createFirePotion(PotionRarity.RARE))
        // items.add(createFirePotion(PotionRarity.EPIC))
    }

    fun loadFromSaveState(data: List<PotionData>) {
        items.clear()
        data.forEach { potionData ->
            val rarity = try {
                PotionRarity.valueOf(potionData.rarity)
            } catch (e: Exception) {
                PotionRarity.COMMON
            }

            val potion = when (potionData.type) {
                "Health" -> createHealthPotion(rarity)
                "Defense" -> createDefensivePotion(rarity)
                "Fire" -> createFirePotion(rarity)
                else -> createHealthPotion(rarity) // Fallback
            }
            items.add(potion)
        }
    }

    fun toSaveData(): List<PotionData> {
        return items.map {
            val type = when (it) {
                is HealthPotion -> "Health"
                is DefensePotion -> "Defense"
                is FirePotion -> "Fire"
            }
            PotionData(type, it.rarity.name)
        }
    }

    fun useItem(potion: Potion) {
        items.remove(potion)
    }

    fun addItem(potion: Potion) {
        items.add(potion)
    }

    fun dispose() {
        potionsSpritesheet.dispose()
    }

    fun createHealthPotion(rarity: PotionRarity = PotionRarity.COMMON) = HealthPotion(
        rarity = rarity,
        textureRegion = healthPotionRegion
    )

    fun createDefensivePotion(rarity: PotionRarity = PotionRarity.COMMON) = DefensePotion(
        rarity = rarity,
        textureRegion = defensivePotionRegion
    )

    fun createFirePotion(rarity: PotionRarity = PotionRarity.COMMON) = FirePotion(
        rarity = rarity,
        textureRegion = firePotionRegion
    )
}
