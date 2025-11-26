package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

data class Consumable(
    val name: String,
    val description: String,
    val healAmount: Int,
    val textureRegion: TextureRegion
)

class ConsumablesInventory {

    private val items = mutableListOf<Consumable>()

    private val potionsSpritesheet by lazy {
        Texture(Gdx.files.internal("items/potions.png"))
    }

    private val healthPotionRegion by lazy {
        TextureRegion(potionsSpritesheet, 48, 32, 16, 16)
    }
    private val manaPotionRegion by lazy {
        TextureRegion(potionsSpritesheet, 64, 32, 16, 16)
    }


    fun getItems(): List<Consumable> {
        return items
    }

    fun loadDefaultPotions() {
        items.clear()
        repeat(3) {
            items.add(createHealthPotion())
        }
        items.add(createManaPotion())
    }

    fun loadFromSaveState(potionCount: Int) {
        items.clear()
        repeat(potionCount) {
            items.add(createHealthPotion())
            // this only loads health potions.
        }
    }

    fun useItem(consumable: Consumable): Int {
        val healAmount = consumable.healAmount
        items.remove(consumable)
        return healAmount
    }

    fun addItem(consumable: Consumable) {
        items.add(consumable)
    }

    fun dispose() {
        potionsSpritesheet.dispose()
    }

    fun createHealthPotion() = Consumable(
        name = "Health Potion",
        description = "A standard potion. Heals 10 HP.",
        healAmount = 10,
        textureRegion = healthPotionRegion
    )

    private fun createManaPotion() = Consumable(
        name = "Mana Potion",
        description = "A blue potion. Restores 10 MP.",
        healAmount = 0,
        textureRegion = manaPotionRegion
    )
}
