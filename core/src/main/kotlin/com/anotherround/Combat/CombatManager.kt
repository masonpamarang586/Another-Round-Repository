package com.anotherround.combat

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import com.anotherround.CharacterClasses.Enemy
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
    val enemy: Enemy,

    private val onLog: (String) -> Unit = {},

    private val onActionStart: (Action) -> Unit = {},

    private val onActionEnd: (Action) -> Unit = {},

    private val onSfx: (SfxEvent) -> Unit = {},

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
    }

    private fun resolve(action: Action) {
        when (action) {
            is Action.Attack -> {
                val dealt = action.attacker.attack(action.defender)

                action.defender.health = (action.defender.health - dealt).coerceAtLeast(0)
                onLog("${action.attacker.name} attacks ${action.defender.name} for $dealt. " +
                    "${action.defender.name} HP=${action.defender.health}")
                val defenderIsPlayer = (action.defender === player)
                if (!action.defender.isAlive()) {
                    onLog("${action.defender.name} is defeated!")
                    onSfx(if (defenderIsPlayer) SfxEvent.PlayerDeath else SfxEvent.EnemyDeath)
                } else {
                    onSfx(if (defenderIsPlayer) SfxEvent.PlayerHurt else SfxEvent.EnemyHurt)
                }
            }
        }
    }

    private fun isOver(): Boolean = !(player.isAlive() && enemy.isAlive())
}
