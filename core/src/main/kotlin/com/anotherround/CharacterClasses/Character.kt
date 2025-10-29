package com.anotherround.CharacterClasses

enum class CharacterState { Idle, Attacking, Hurt, Dead }
interface Character {
    //Stats
    val name: String
    var level: Int
    var health: Int
    var defenseStat: Int
    var attackStat: Int
    val maxHealth: Int get() = 100

    var state: CharacterState
        get() = CharacterState.Idle
        set(_) {}
    // functions
    // shouldn't need a defend function, defending may just use the defenseStat attribute
    fun attack(target: Character): Int {
        return (attackStat - target.defenseStat).coerceAtLeast(0)
    }

    fun takeDamage(incomingDamage: Int) {
        val dmg = (incomingDamage - defenseStat).coerceAtLeast(0)
        val before = health
        health = (health-dmg).coerceAtLeast(0)
        println("$name takes $dmg damage. HP: $before -> $health")
    }
    fun heal(amount: Int) {
        health = (health+amount).coerceAtMost(maxHealth)
        println("Healed $amount hp")
    }
    fun isAlive(): Boolean {
        return health > 0
    }
}
