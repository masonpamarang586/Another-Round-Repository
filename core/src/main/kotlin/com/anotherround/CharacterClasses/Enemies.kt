package com.anotherround.CharacterClasses

class RedGrunt(
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

class EvilWizard(
    override val name: String = "EvilWizard",
    override var level: Int = 2,
    override var health: Int = 35,
    override var defenseStat: Int = 10,
    override var attackStat: Int = 7
) : Character {
    override val maxHealth: Int get() = 35
}

class NightBorne(
    override val name: String = "NightBorne",
    override var level: Int = 2,
    override var health: Int = 50,
    override var defenseStat: Int = 8,
    override var attackStat: Int = 17
) : Character {
    override val maxHealth: Int get() = 50
}
