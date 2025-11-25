package com.anotherround.CharacterClasses

class RedEnemy(
    override val name: String = "Grunt",
    override var level: Int = 1,
    override var health: Int = 20,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 10
) : Character {
    override val maxHealth: Int get() = 20
}

class Phantom(
    override val name: String = "Phantom",
    override var level: Int = 2,
    override var health: Int = 35,
    override var defenseStat: Int = 2,
    override var attackStat: Int = 13
) : Character {
    override val maxHealth: Int get() = 35
}
