package com.anotherround.CharacterClasses

import kotlin.random.Random

object EnemyFactory {

    fun randomKind(): EnemyKind =
        EnemyKind.values().random()

    fun create(kind: EnemyKind): Character = when (kind) {
        EnemyKind.RedGrunt   -> RedGrunt()
        EnemyKind.Phantom    -> Phantom()
        EnemyKind.EvilWizard -> EvilWizard()
        EnemyKind.NightBorne -> NightBorne()
    }
    fun randomEnemy(): Character = create(randomKind())
}
