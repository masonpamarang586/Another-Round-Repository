package com.anotherround

import com.badlogic.gdx.math.Vector2

object GameLogic {
    enum class State {
        PLAYER_TURN,
        PLAYER_ATTACK,
        PLAYER_ITEM,
        ENEMY_TURN,
        ENEMY_ATTACK,
        ENEMY_ITEM
    }

    var screen: BattleScreen? = null

    var state = State.PLAYER_TURN

    const val ANIMATION_WAIT = 1.0f
    var accumulator = 0f

    fun doLogic(delta: Float, state: GameLogic.State) {
        println(accumulator)
        accumulator += delta
        if (accumulator >= ANIMATION_WAIT) {
            accumulator = 0f
        }

        when (state) {
            State.PLAYER_TURN -> {}
            State.PLAYER_ATTACK -> {
                if (this.state == State.PLAYER_TURN) {
                    // TODO: Attack
                }
            }
            State.PLAYER_ITEM -> {
                if (this.state == State.PLAYER_TURN) {
                    // TODO: Open item menu
                }
            }
            State.ENEMY_TURN -> {}
            State.ENEMY_ATTACK -> {}
            State.ENEMY_ITEM -> {}
        }
    }
}
