package com.anotherround.CharacterClasses

interface Character {
    //Stats
    val name: String
    var level: Int
    var health: Int
    var defenseStat: Int
    var attackStat: Int
    // functions
    // shouldn't need a defend function, defending may just use the defenseStat attribute
    fun attack(target: Character) {
        target.takeDamage(attackStat)
        println("$name attacks ${target.name} for $attackStat damage!")
    }
    fun takeDamage(incomingDamage: Int) {
        health -= incomingDamage
        println("$name takes $incomingDamage damage. Remaining health: $health")
    }
    fun heal(amount: Int) {
        health += amount
        println("Healed $amount hp")
    }
    fun isAlive(): Boolean {
        return health > 0
    }
}
