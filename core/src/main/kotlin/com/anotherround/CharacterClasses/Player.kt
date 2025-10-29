package com.anotherround.CharacterClasses

class Player (
    override val name: String = "Hero",
    override var level: Int = 1,
    override var health: Int = 100,
    override var defenseStat: Int = 1,
    override var attackStat: Int = 10,
) : Character {
    val gear = arrayListOf("Sword")
    val usables = arrayListOf("HealthPotion")
    override val maxHealth: Int get() = 100
}
