package com.anotherround.CharacterClasses

class RedGrunt(
    override val name: String = "Grunt",
    override var level: Int = 1,
    override var health: Int = 25,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 8
) : Character {
    override val maxHealth: Int get() = 25
}

class Phantom(
    override val name: String = "Phantom",
    override var level: Int = 2,
    override var health: Int = 35,
    override var defenseStat: Int = 5,
    override var attackStat: Int = 16
) : Character {
    override val maxHealth: Int get() = 35
}

class EvilWizard(
    override val name: String = "EvilWizard",
    override var level: Int = 2,
    override var health: Int = 35,
    override var defenseStat: Int = 2,
    override var attackStat: Int = 11
) : Character {
    override val maxHealth: Int get() = 35
}

class NightBorne(
    override val name: String = "NightBorne",
    override var level: Int = 2,
    override var health: Int = 50,
    override var defenseStat: Int = 3,
    override var attackStat: Int = 21
) : Character {
    override val maxHealth: Int get() = 50
}
