package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json

class ConsumablesInventory {

    //Amount of slots is set to 8
    val slots: MutableList<Consumable> = mutableListOf()
    val tempList: MutableList<Consumable> = loadConsumables()


    fun loadConsumables(): MutableList<Consumable> {
        // Test consumable item

        val json = Json()

        val fileHandle: FileHandle = Gdx.files.internal("items.json")

        val consumList: MutableList<Consumable> = json.fromJson(
            MutableList::class.java,
            Consumable::class.java,
            fileHandle

        ) as MutableList<Consumable>

        return consumList

    }

    //Remove a consumable from a slot
    fun removeConsumable(consumable: Consumable) {
        slots.remove(consumable);
    }

    //Add a consumable to a slot
    fun addConsumable(consumable: Consumable) {


        val foundConsumable = tempList.find{ it.equals(consumable) }


        if (slots.size < 8 && foundConsumable != null) {
          slots.add(consumable);
        }
        }
    

    }



