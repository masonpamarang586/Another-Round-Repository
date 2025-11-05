package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json

class ConsumablesInventory {

    //Amount of slots is set to 8
    val slots: MutableList<Consumable> = mutableListOf()


    //Add a consumable to a slot
    fun addConsumable(consumable: Consumable) {

        val tempList: MutableList<Consumable> = loadConsumables()

        tempList.find{ it == consumable }

        if (slots.size < 8) {
            slots.add(consumable);
        }
    }

    //Remove a consumable from a slot
    fun removeConsumable(consumable: Consumable) {
        slots.remove(consumable);
    }


    fun loadConsumables(): MutableList<Consumable> {
        // Test consumable item
        val json = Json()

        json.addClassTag("consumable", Consumable::class.java)

        val jsonString = Gdx.files.internal("items/items.json").readString()

        val consumables = json.fromJson(MutableList::class.java, Consumable::class.java, jsonString) as MutableList<Consumable>

        return consumables
    }

}
