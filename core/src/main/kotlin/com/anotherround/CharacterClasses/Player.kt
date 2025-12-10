package com.anotherround.CharacterClasses

class Player (
    override var name: String = "Hero",
    override var level: Int = 1,
    override var health: Int = 40,
    override var defenseStat: Int = 0,
    override var attackStat: Int = 20,
    var currency: Int = 100,
    var currentXp: Int = 0,
    var xpToNextLevel: Int = 10
) : Character {
    override var maxHealth: Int = health + (level - 1) * 10

    fun gainXp(amount: Int, onLevelUp: (newLevel: Int) -> Unit = {}) {
        if (amount <= 0) return

        currentXp += amount

        while (currentXp >= xpToNextLevel) {
            currentXp -= xpToNextLevel
            level++
            xpToNextLevel = xpRequiredFor(level)
            applyLevelUpStatGains()
            onLevelUp(level)
        }
    }

    fun xpRequiredFor(level: Int): Int { // random math I found to make it non-linear
        return 10 + (level - 1) * (level - 1) * 5
    }

    private fun applyLevelUpStatGains() {
        val hpGain = 10 + level * 2
        val atkGain = 2 + (level / 2)
        val defGain = 1 + (level / 3)

        val oldMax = maxHealth - hpGain
        val newMax = maxHealth
        val missing = oldMax - health
        health = (health + missing + hpGain).coerceAtMost(newMax)

        attackStat += atkGain
        defenseStat += defGain
    }
}
