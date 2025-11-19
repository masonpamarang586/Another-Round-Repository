package com.anotherround.CharacterClasses

class Enemies(
    override val name: String = "BadGuy",
    override var level: Int = 1,
    override var health: Int = 20,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 10
) : Character {
    override val maxHealth: Int get() = 20
}
