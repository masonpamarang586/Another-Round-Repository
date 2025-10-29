package com.anotherround.CharacterClasses

class Enemy(
    override val name: String = "BadGuy",
    override var level: Int = 1,
    override var health: Int = 100,
    override var defenseStat: Int = 1,
    override var attackStat: Int = 10
) : Character {
    override val maxHealth: Int get() = 100
}
