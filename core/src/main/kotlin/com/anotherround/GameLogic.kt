package com.anotherround

import com.anotherround.Screens.BattleScreen

object GameLogic {
    enum class GameState {
        MAIN_MENU,
        BATTLE,
        SHOP
    }

    enum class BattleState {
        PLAYER_TURN,
        PLAYER_ATTACK,
        PLAYER_ITEM,
        ENEMY_TURN,
        ENEMY_ATTACK,
        ENEMY_ITEM
    }

    var screen: BattleScreen? = null

    var gameState = GameState.MAIN_MENU
    var battles = 0

    var battleState = BattleState.PLAYER_TURN

    const val ANIMATION_WAIT_TIME = 1.0f
    var accumulator = 0f

    fun doGameLogic(newState: GameLogic.BattleState) {
        /*
         *
         *
         *
         */
    }

    fun doBattleLogic(delta: Float, newState: GameLogic.BattleState) {
        accumulator += delta
        if (accumulator >= ANIMATION_WAIT_TIME) {
            accumulator = 0f
        }

        when (newState) {
            BattleState.PLAYER_TURN -> {}
            BattleState.PLAYER_ATTACK -> {
                if (this.battleState == BattleState.PLAYER_TURN) {
                    // TODO: Attack
                }
            }
            BattleState.PLAYER_ITEM -> {
                if (this.battleState == BattleState.PLAYER_TURN) {
                    // TODO: Open item menu
                }
            }
            BattleState.ENEMY_TURN -> {}
            BattleState.ENEMY_ATTACK -> {}
            BattleState.ENEMY_ITEM -> {}
        }
    }
}
