package com.anotherround.CharacterClasses

class RedGrunt(
    override val name: String = "Grunt",
    override var level: Int = 1,
    override var health: Int = 35,
    override var defenseStat: Int = 5,
    override var attackStat: Int = 15,
    val xpReward: Int = (1..10).random()
) : Character {
    override val maxHealth: Int get() = 35
}

class Phantom(
    override val name: String = "Phantom",
    override var level: Int = 2,
    override var health: Int = 45,
    override var defenseStat: Int = 8,
    override var attackStat: Int = 20,
    val xpReward: Int = (11..19).random()
) : Character {
    override val maxHealth: Int get() = 45
}

class EvilWizard(
    override val name: String = "EvilWizard",
    override var level: Int = 2,
    override var health: Int = 45,
    override var defenseStat: Int = 5,
    override var attackStat: Int = 25,
    val xpReward: Int = (11..19).random()
) : Character {
    override val maxHealth: Int get() = 45
}

class NightBorne(
    override val name: String = "NightBorne",
    override var level: Int = 2,
    override var health: Int = 60,
    override var defenseStat: Int = 15,
    override var attackStat: Int = 25,
    val xpReward: Int = (11..19).random()
) : Character {
    override val maxHealth: Int get() = 60
}
