package com.anotherround.Consumables

data class Consumable(val consumableID: String,
                      val name: String,
                 val description: String,
                 val healingAmount: Int = 0) {

    //Contains all types of consumables
    val consumableList: MutableMap<String,Consumable> = mutableMapOf<String, Consumable>()


    fun loadConsumables() {
        // Test consumable item
        val healthPotion = Consumable(
            consumableID = "consum_healthpotion",
            name = "Health Potion",
            description = "Basic health potion",
            healingAmount = 5,
        )
        consumableList[healthPotion.consumableID] = healthPotion
    }

    fun use() {
        // ADD HEAL TO PLAYER HP
        //Player.health
    }




}
