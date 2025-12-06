package com.anotherround.CharacterClasses

class Player (
    override var name: String = "Hero",
    override var level: Int = 1,
    override var health: Int = 40,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 20,
    var currency: Int = 100
) : Character {
    override var maxHealth: Int = 40
}
