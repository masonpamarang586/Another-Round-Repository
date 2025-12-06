package com.anotherround.combat

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import kotlin.math.max
enum class Turn { PLAYER, ENEMY, OVER }
enum class SfxEvent {
    PlayerAttack, EnemyAttack,
    PlayerHurt, EnemyHurt,
    PlayerDeath, EnemyDeath
}


sealed class Action {
    data class Attack(val attacker: Character, val defender: Character) : Action()
}

class CombatManager(
    val player: Player,
    val enemy: Character,

    private val onLog: (String) -> Unit = {},

    private val onActionStart: (Action) -> Unit = {},

    private val onActionEnd: (Action) -> Unit = {},

    private val onSfx: (SfxEvent) -> Unit = {},

    private val onDefeat: (defeated: Character, by: Character) -> Unit = { _, _ -> },

    private val onDamage: (Character, Int) -> Unit = { _, _ -> },

    var resolveDelay: Float = 5.0f
) {
    var turn: Turn = Turn.PLAYER
        private set

    private var pending: Action? = null
    private var timer = 0f
    private var postDelay = 0f

    fun pauseNextTurnFor(seconds: Float) {
        postDelay = max(postDelay, seconds)
    }

    fun canOpenMenu(): Boolean {
        if (turn != Turn.PLAYER || isOver()) return false
        if (pending != null) return false
        return true
    }

    fun requestPlayerAttack(): Boolean {
        if (turn != Turn.PLAYER || isOver()) return false
        if (pending != null) return false
        pending = Action.Attack(player, enemy)
        return true
    }

    private val statusEffects = mutableMapOf<Character, MutableList<StatusEffect>>()

    fun addEffect(target: Character, effect: StatusEffect) {
        val effects = statusEffects.getOrPut(target) { mutableListOf() }
        effects.add(effect)
        onLog("${target.name} gains ${effect.name}!")
    }

    private fun processBurnEffects(character: Character) {
        val effects = statusEffects[character] ?: return
        
        val it = effects.iterator()
        var totalBurnDamage = 0

        while (it.hasNext()) {
            val effect = it.next()
            if (effect is StatusEffect.Burn) {
                totalBurnDamage += effect.damagePerRound
                effect.roundsLeft--
                if (effect.roundsLeft <= 0) {
                    it.remove()
                    onLog("${character.name}'s burn fades.")
                }
            }
        }

        if (totalBurnDamage > 0) {
            character.takeDamage(totalBurnDamage)
            onDamage(character, totalBurnDamage) // <--- ADDED
            onLog("${character.name} takes $totalBurnDamage burn damage!")
            // Check for death from burn
            if (!character.isAlive()) {
                 onLog("${character.name} burned to death!")
                 onDefeat(character, if (character === player) enemy else player)
            } else {
                 onSfx(if (character === player) SfxEvent.PlayerHurt else SfxEvent.EnemyHurt)
            }
        }
    }

    private fun getDefenseReduction(character: Character): Float {
        val effects = statusEffects[character] ?: return 0f
        val defenseBuffs = effects.filterIsInstance<StatusEffect.DefenseBuff>()
        if (defenseBuffs.isEmpty()) return 0f

        // Simplistic stacking: use the highest reduction, or sum?
        // Use highest single reduction for now to avoid >100% block issues easily
        val maxReduction = defenseBuffs.maxOf { it.reductionPercent }
        
        // Decrement stack
        val it = effects.iterator()
        while (it.hasNext()) {
            val effect = it.next()
            if (effect is StatusEffect.DefenseBuff) {
                effect.durationStack--
                if (effect.durationStack <= 0) {
                    it.remove()
                    onLog("${character.name}'s defense buff fades.")
                }
            }
        }
        return maxReduction
    }

    fun update(delta: Float) {
        if (isOver()) {
            turn = Turn.OVER
            return
        }

        if (postDelay > 0f) {
            postDelay -= delta
            return
        }

        // If it's the enemy's turn and nothing is queued, queue a simple attack.
        if (turn == Turn.ENEMY && pending == null && timer <= 0f) {
            // Process start of turn effects for enemy
            // Assuming effects process at start of turn? Or end? 
            // Let's do start of turn logic for burn just before they act
             // processBurnEffects(enemy) // Moving this to finishAndAdvance to process consistently each round/turn transition
            pending = Action.Attack(enemy, player)
        }

        val action = pending ?: return

        if (timer == 0f) {
            onActionStart(action)
            if (action is Action.Attack) {
                if (action.attacker === player) {
                    onSfx(SfxEvent.PlayerAttack)
                } else {
                    onSfx(SfxEvent.EnemyAttack)
                }
            }
            timer = max(0f, resolveDelay)
            if (timer == 0f) {
                resolve(action)
                finishAndAdvance(action)
            }
            return
        }

        timer -= delta
        if (timer <= 0f) {
            resolve(action)
            finishAndAdvance(action)
        }
    }

    private fun finishAndAdvance(action: Action) {
        onActionEnd(action)
        pending = null
        timer = 0f

        if (isOver()) { turn = Turn.OVER; return }

        // Alternate turns based on who just acted
        turn = when (action) {
            is Action.Attack ->
                if (action.attacker === player) Turn.ENEMY else Turn.PLAYER
        }
        
        // Process burn at start of new turn holder? 
        // Or process at end of round?
        // "Damage for 2 rounds". 
        // Simple approach: Apply burn to the character whose turn it IS NOT (end of their turn) or whose turn it IS (start of their turn).
        // Let's apply burn to the character who is ABOUT TO ACT.
        if (turn == Turn.PLAYER) {
             processBurnEffects(player)
        } else if (turn == Turn.ENEMY) {
             processBurnEffects(enemy)
        }
        
        // Check death again after burn
        if (isOver()) { turn = Turn.OVER; return }
    }

    private fun resolve(action: Action) {
        when (action) {
            is Action.Attack -> {
                var dealt = action.attacker.attack(action.defender)
                
                // Apply Defense Buff
                val reduction = getDefenseReduction(action.defender)
                if (reduction > 0f) {
                    val original = dealt
                    dealt = (dealt * (1f - reduction)).toInt()
                    onLog("Damage reduced by ${(reduction*100).toInt()}% ($original -> $dealt)")
                }

                action.defender.health = (action.defender.health - dealt).coerceAtLeast(0)
                onDamage(action.defender, dealt) // <--- ADDED
                onLog("${action.attacker.name} attacks ${action.defender.name} for $dealt. " +
                    "${action.defender.name} HP=${action.defender.health}")
                val defenderIsPlayer = (action.defender === player)
                if (!action.defender.isAlive()) {
                    onLog("${action.defender.name} is defeated!")
                    onSfx(if (defenderIsPlayer) SfxEvent.PlayerDeath else SfxEvent.EnemyDeath)
                    onDefeat(action.defender, action.attacker)
                } else {
                    onSfx(if (defenderIsPlayer) SfxEvent.PlayerHurt else SfxEvent.EnemyHurt)
                }
            }
        }
    }

    private fun isOver(): Boolean = !(player.isAlive() && enemy.isAlive())
}
