package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.Json

class Consumable {


    fun loadConsumables(): MutableList<Consumable> {
        // Test consumable item
        val json = Json()

        json.addClassTag("consumable", Consumable::class.java)

        val jsonString = Gdx.files.internal("items/items.json").readString()

        val consumables = json.fromJson(MutableList::class.java, Consumable::class.java, jsonString) as MutableList<Consumable>

        return consumables
    }

    fun use() {
        // ADD HEAL TO PLAYER HP
        //Player.health
    }




}
