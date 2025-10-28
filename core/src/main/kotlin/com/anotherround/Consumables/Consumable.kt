package com.anotherround.Consumables

data class Consumable(val consumableID: String,
                      val name: String,
                 val description: String,
                 val healingAmount: Int = 0) {

    //Contains all types of consumables
    val consumableList: MutableMap<String,Consumable> = mutableMapOf<String, Consumable>()


    fun loadConsumables() {
        // Test consumable item
        val apple = Consumable(
            consumableID = "consum_apple",
            name = "Apple",
            description = "Freshly picked apple; Heals 5 HP",
            healingAmount = 5,
        )
        consumableList[apple.consumableID] = apple
    }

    fun use() {
        // ADD HEAL TO PLAYER HP
        //Player.health
    }




}
