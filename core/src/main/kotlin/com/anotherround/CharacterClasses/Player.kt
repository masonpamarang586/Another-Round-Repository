package com.anotherround.CharacterClasses

class Player (
    override var name: String = "Hero",
    override var level: Int = 1,
    override var health: Int = 100,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 20,
    var currency: Int = 100
) : Character {
    override val maxHealth: Int get() = 100
}
