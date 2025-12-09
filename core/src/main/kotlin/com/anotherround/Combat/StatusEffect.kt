package com.anotherround.combat

sealed class StatusEffect {
    abstract val name: String
    abstract val description: String

    data class DefenseBuff(
        val reductionPercent: Float, // e.g., 0.20f for 20%
        var durationStack: Int = 1 // Number of attacks it lasts for
    ) : StatusEffect() {
        override val name = "Defensive Lacquer"
        override val description = "Blocks ${(reductionPercent * 100).toInt()}% of the next attack."
    }

    data class Burn(
        val damagePerRound: Int,
        var roundsLeft: Int
    ) : StatusEffect() {
        override val name = "Burn"
        override val description = "Takes $damagePerRound damage for $roundsLeft more rounds."
    }
}
