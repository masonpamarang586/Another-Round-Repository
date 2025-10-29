package com.anotherround.Consumables

class ConsumablesInventory {

    //Amount of slots is set to 8
    val slots: MutableList<Consumable> = mutableListOf()

    //Add a consumable to a slot
    fun addConsumable(consumable: Consumable) {
        if (slots.size < 8) {
            slots.add(consumable);
        }
    }

    //Remove a consumable from a slot
    fun removeConsumable(consumable: Consumable) {
        slots.remove(consumable);
    }

}
