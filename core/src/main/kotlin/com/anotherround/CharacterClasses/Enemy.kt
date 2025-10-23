package com.anotherround.CharacterClasses

class Enemy(
    override val name: String,
    override var level: Int = 1,
    override var health: Int = 100,
    override var defenseStat: Int = 1,
    override var attackStat: Int = 10
) : Character {}
