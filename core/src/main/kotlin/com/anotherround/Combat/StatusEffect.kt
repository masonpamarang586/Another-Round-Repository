package com.anotherround.combat

enum class EffectType {
    DEFENSE_BUFF, // Reduces incoming damage
    DOT_FIRE      // Damage over time
}

data class StatusEffect(
    val name: String,
    val type: EffectType,
    var duration: Int, // Number of turns remaining
    val value: Int     // Magnitude (e.g. % reduction or damage amount)
)
