package com.anotherround.CharacterClasses

enum class EnemyKind { RedGrunt, Phantom, EvilWizard, NightBorne }

fun enemyConfig(kind: EnemyKind): SpriteConfig = when (kind) {
    EnemyKind.RedGrunt -> SpriteConfig(
        idlePath      = "generic_char_v0.2/png/red/char_red_1_index10.png",
        damageRowPath = "generic_char_v0.2/png/red/char_red_1_damage.png",
        deathRowPath  = "generic_char_v0.2/png/red/char_red_1_death.png",
        attackRowPath = "generic_char_v0.2/png/red/char_red_1_attack.png",
        framesAreReversed = true,
        frameSize = 60,
        idleFps = 4f,
        hurtFps = 12f,
        drawWidth = 3f,
        drawHeight = 3f,
        offsetX = +1.5f
    )

    EnemyKind.Phantom -> SpriteConfig(
        idlePath      = "phantom/png/idle.png",
        damageRowPath = "phantom/png/idle.png",
        deathRowPath  = "phantom/png/death.png",
        attackRowPath = "phantom/png/attacking.png",
        framesAreReversed = true,
        frameSize = 60,
        idleFps = 5f,
        hurtFps = 12f,
        drawWidth = 3f,
        drawHeight = 3f,
        attackFps = 6f,
        deathFps = 18f,
        offsetX = +1.5f
    )
    EnemyKind.EvilWizard -> SpriteConfig(
        idlePath      = "evil_wizard/Sprites/Idle.png",
        damageRowPath = "evil_wizard/Sprites/Take hit.png",
        deathRowPath  = "evil_wizard/Sprites/Death.png",
        attackRowPath = "evil_wizard/Sprites/Attack2.png",
        framesAreReversed = true,
        frameSize = 60,
        idleFps = 8f,
        hurtFps = 12f,
        drawWidth = 3f,
        drawHeight = 3f,
        attackFps = 8f,
        deathFps = 7f,
        offsetX = +1.5f
    )
    EnemyKind.NightBorne -> SpriteConfig(
        idlePath      = "nightborne/NightBorneIdle.png",
        damageRowPath = "nightborne/NightBorneDamage.png",
        deathRowPath  = "nightborne/NightBorneDeath.png",
        attackRowPath = "nightborne/NightBorneAttack.png",
        framesAreReversed = true,
        frameSize = 60,
        idleFps = 9f,
        hurtFps = 12f,
        drawWidth = 3f,
        drawHeight = 3f,
        attackFps = 12f,
        deathFps = 10f,
        offsetX = +1.5f
    )
}
