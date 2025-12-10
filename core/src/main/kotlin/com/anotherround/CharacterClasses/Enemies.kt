package com.anotherround.CharacterClasses

class RedGrunt(
    override val name: String = "Grunt",
    override var level: Int = 1,
    override var maxHealth: Int = 25,
    override var health: Int = maxHealth,
    override var defenseStat: Int = 2,
    override var attackStat: Int = 10,
    val xpReward: Int = (1..10).random()
) : Character

class Phantom(
    override val name: String = "Phantom",
    override var level: Int = 1,
    override var maxHealth: Int = 35,
    override var health: Int = maxHealth,
    override var defenseStat: Int = 4,
    override var attackStat: Int = 12,
    val xpReward: Int = (11..19).random()
) : Character

class EvilWizard(
    override val name: String = "EvilWizard",
    override var level: Int = 1,
    override var maxHealth: Int = 35,
    override var health: Int = maxHealth,
    override var defenseStat: Int = 2,
    override var attackStat: Int = 14,
    val xpReward: Int = (11..19).random()
) : Character

class NightBorne(
    override val name: String = "NightBorne",
    override var level: Int = 1,
    override var maxHealth: Int = 45,
    override var health: Int = maxHealth,
    override var defenseStat: Int = 6,
    override var attackStat: Int = 16,
    val xpReward: Int = (11..19).random()
) : Character
