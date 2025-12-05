package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture

class ConsumablesInventory {

    private val items = mutableListOf<Potion>()

    private val potionsSpritesheet by lazy {
        Texture(Gdx.files.internal("items/potions.png"))
    }

    init {
        // Initialize factory with the texture
        PotionFactory.init(potionsSpritesheet)
    }

    fun getItems(): List<Potion> {
        return items
    }

    fun loadDefaultPotions() {
        items.clear()
        // Add a mix of potions
        items.add(PotionFactory.createHealthPotion(PotionRarity.COMMON))
        items.add(PotionFactory.createHealthPotion(PotionRarity.UNCOMMON))
        items.add(PotionFactory.createDefensiveLacquer())
        items.add(PotionFactory.createLiquidFire())
    }

    fun loadFromSaveState(savedItems: List<Potion>) {
        items.clear()
        items.addAll(savedItems)
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
    
    // Helper for reconstruction if needed, though PotionFactory handles creation.
    // We might need methods to create specific potions by name if we save just the name/type.
    // But for now, we rely on PotionFactory.
}
