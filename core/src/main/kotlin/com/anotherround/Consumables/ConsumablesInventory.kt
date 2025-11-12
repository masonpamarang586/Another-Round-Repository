package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.utils.Json

class ConsumablesInventory {

    //Amount of slots is set to 8
    val slots: MutableList<Consumable> = mutableListOf()
    val tempList: MutableList<Consumable> = loadConsumables()


    fun getConsumable(index: Int): Consumable {


        return slots.get(index)
    }


    fun loadConsumables(): MutableList<Consumable> {

        /*
        val json = Json()

        val fileHandle: FileHandle = Gdx.files.internal("items/items.json")
        val jsonString = fileHandle.readString()

        // FLAG: ERROR IS HAPPENING WITH THE CODE BELOW, 32-37
        val consumList = json.fromJson(
            MutableList::class.java,
            Consumable::class.java,
            fileHandle

        ) as MutableList<Consumable>
*/


        var consumList: MutableList<Consumable> = mutableListOf()

        consumList.add(Consumable("Healing Potion", "Most standard healing potion. Heals 5 HP.", "potions.png", 5))

        return consumList

    }

    //Remove a consumable from a slot
    fun removeConsumable(consumable: Consumable) {
        slots.remove(consumable);
    }

    //Add a consumable to a slot
    fun addConsumable(name: String) {


        val foundConsumable = tempList.find{ it.name == name }


        if (slots.size < 8 && foundConsumable != null) {
          slots.add(foundConsumable);
        }
        }
    }







