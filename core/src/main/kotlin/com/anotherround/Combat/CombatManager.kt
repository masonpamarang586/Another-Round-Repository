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

        // Apply Start-of-Turn Effects for the NEW turn owner
        applyStartOfTurnEffects(if (turn == Turn.PLAYER) player else enemy)
    }

    private fun applyStartOfTurnEffects(character: Character) {
        val iterator = character.activeEffects.iterator()
        while (iterator.hasNext()) {
            val effect = iterator.next()
            if (effect.type == EffectType.DOT_FIRE) {
                character.takeDamage(effect.value)
                onLog("${character.name} takes ${effect.value} fire damage!")
                if (!character.isAlive()) {
                    onLog("${character.name} burned to death!")
                    // Handle defeat if needed, though usually handled in resolve
                    // For simplicity, we might need to trigger defeat callback here if we want robust handling
                    // But let's assume resolve checks alive status or next update loop handles it?
                    // Actually, update loop checks isOver().
                    // But we need to trigger onDefeat if they die from DOT.
                    // Let's check here.
                    val killer = if (character === player) enemy else player
                    onDefeat(character, killer)
                }
            }
            
            // Decrement duration
            effect.duration--
            if (effect.duration <= 0) {
                iterator.remove()
                onLog("${effect.name} on ${character.name} has expired.")
            }
        }
    }

    private fun resolve(action: Action) {
        when (action) {
            is Action.Attack -> {
                var dealt = action.attacker.attack(action.defender)
                
                // Check for Defense Buffs on defender
                val defenseBuff = action.defender.activeEffects.find { it.type == EffectType.DEFENSE_BUFF }
                if (defenseBuff != null) {
                    val reduction = (dealt * (defenseBuff.value / 100f)).toInt()
                    dealt = (dealt - reduction).coerceAtLeast(0)
                    onLog("Defensive Lacquer reduced damage by $reduction!")
                    
                    // Defense buff expires after being hit (as per plan/implementation choice)
                    // Or we can let it last for 'duration' turns. 
                    // Plan said "Lasts until hit (handled as 1 turn or special logic)".
                    // Let's consume it here if it's "until hit".
                    if (defenseBuff.duration > 0) {
                         // If we want it to be single-hit:
                         action.defender.activeEffects.remove(defenseBuff)
                         onLog("Defensive Lacquer wore off.")
                    }
                }

                action.defender.health = (action.defender.health - dealt).coerceAtLeast(0)
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
