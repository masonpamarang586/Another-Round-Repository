package com.anotherround.combat

import com.anotherround.CharacterClasses.Character
import com.anotherround.CharacterClasses.Player
import kotlin.math.max
import kotlin.random.Random
import kotlin.math.roundToInt

enum class Turn { PLAYER, ENEMY, OVER }
enum class SfxEvent {
    PlayerAttack, EnemyAttack,
    PlayerHurt, EnemyHurt,
    PlayerDeath, EnemyDeath
}


sealed class Action {
    data class Attack(val attacker: Character, val defender: Character) : Action()
    data class Defend(val target: Character, val reductionPercent: Float, val stacks: Int) : Action()
    data class ApplyBurn(
        val applier: Character,
        val target: Character,
        val damagePerRound: Int,
        val rounds: Int
    ) : Action()
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

    private fun applyDamageVariance(base: Int): Int {
        if (base <= 0) return 0

        // +/- 15% variance
        val variance = Random.nextDouble(0.85, 1.15)
        var final = (base * variance).roundToInt()

        // Crit / glancing chances
        val roll = Random.nextDouble()
        when {
            roll < 0.10 -> { // 10% crit
                final = (final * 1.5).roundToInt()
                onLog("Critical hit!")
            }
            roll < 0.20 -> { // next 10% glancing
                final = (final * 0.5).roundToInt()
                onLog("Glancing blow.")
            }
        }

        return final.coerceAtLeast(0)
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
            pending = decideEnemyAction()
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
    private fun decideEnemyAction(): Action { // added "AI-intelligence" for the players
        val enemyHpPercent = enemy.health.toFloat() / enemy.maxHealth
        val playerHpPercent = player.health.toFloat() / player.maxHealth
        val roll = Random.nextDouble()

        return when {
            // Low HP → sometimes Defend
            enemyHpPercent < 0.35f && roll < 0.40 -> {
                Action.Defend(
                    target = enemy,
                    reductionPercent = 0.40f,
                    stacks = 1
                )
            }

            // Player healthy and not heavily burning → sometimes ApplyBurn
            playerHpPercent > 0.5f && roll < 0.30 && !hasStrongBurn(player) -> {
                Action.ApplyBurn(
                    applier = enemy,
                    target = player,
                    damagePerRound = 5,
                    rounds = 2
                )
            }

            // Default: Attack
            else -> Action.Attack(enemy, player)
        }
    }

    private fun hasStrongBurn(target: Character): Boolean {
        val effects = statusEffects[target] ?: return false
        val burn = effects.filterIsInstance<StatusEffect.Burn>().sumOf { it.damagePerRound }
        return burn >= 5
    }

    private fun finishAndAdvance(action: Action) {
        onActionEnd(action)
        pending = null
        timer = 0f

        if (isOver()) { turn = Turn.OVER; return }
        val actor: Character = when (action) {
            is Action.Attack    -> action.attacker
            is Action.Defend    -> action.target
            is Action.ApplyBurn -> action.applier
        }
        // Process End of Turn Effects (Burn) for the actor who just finished
        processBurnEffects(actor)

        // Check death again after burn
        if (isOver()) { turn = Turn.OVER; return }

        // Alternate turns based on who just acted
        turn = if (actor === player) Turn.ENEMY else Turn.PLAYER
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

                dealt = applyDamageVariance(dealt)

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
            is Action.Defend -> {
                addEffect(
                    action.target,
                    StatusEffect.DefenseBuff(
                        reductionPercent = action.reductionPercent,
                        durationStack = action.stacks
                    )
                )
                onLog("${action.target.name} braces for impact!")
            }
            is Action.ApplyBurn -> {
                addEffect(
                    action.target,
                    StatusEffect.Burn(
                        damagePerRound = action.damagePerRound,
                        roundsLeft = action.rounds
                    )
                )
                onLog("${action.applier.name} ignites ${action.target.name}!")
            }
        }
    }

    private fun isOver(): Boolean = !(player.isAlive() && enemy.isAlive())
}
