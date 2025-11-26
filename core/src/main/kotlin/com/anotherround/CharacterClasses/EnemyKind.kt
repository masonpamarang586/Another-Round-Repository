package com.anotherround.CharacterClasses

enum class EnemyKind { RedGrunt, Phantom, EvilWizard, NightBorne }

fun enemyConfig(kind: EnemyKind): SpriteConfig = when (kind) {
    EnemyKind.RedGrunt -> SpriteConfig(
        idlePath      = "generic_char_v0.2/png/red/char_red_1_index10.png",
        damageRowPath = "generic_char_v0.2/png/red/char_red_1_damage.png",
        deathRowPath  = "generic_char_v0.2/png/red/char_red_1_death.png",
        attackRowPath = "generic_char_v0.2/png/red/char_red_1_attack.png",
        framesAreReversed = false,
        frameSize = 60,
        idleFps = 4f,
        hurtFps = 12f,
        drawWidth = 3f,
        drawHeight = 3f,
        offsetX = +1.5f
    )

    EnemyKind.Phantom -> SpriteConfig(
        idlePath      = "phantom/phantomIdleFrame1.png",
        damageRowPath = "phantom/phantomDamageFrame1.png",
        deathRowPath  = "phantom/phantomDeathFrame1.png",
        attackRowPath = "phantom/phantomAttackFrame1.png",
        framesAreReversed = true,
        frameSize = 80,
        drawWidth = 3f,
        drawHeight = 3f,
        offsetX = +1.5f
    )
    EnemyKind.EvilWizard -> SpriteConfig(
        idlePath      = "evil_wizard/evilWizardIdleFrame1.png",
        damageRowPath = "evil_wizard/evilWizardDamageFrame1.png",
        deathRowPath  = "evil_wizard/evilWizardDeathFrame1.png",
        attackRowPath = "evil_wizard/evilWizardAttackFrame1.png",
        framesAreReversed = true,
        frameSize = 80,
        drawWidth = 3f,
        drawHeight = 3f,
        offsetX = +1.5f
    )
    EnemyKind.NightBorne -> SpriteConfig(
        idlePath      = "nightborne/NightBorneIdleFrame1.png",
        damageRowPath = "nightborne/NightBorneDamageFrame1.png",
        deathRowPath  = "nightborne/NightBorneDeathFrame1.png",
        attackRowPath = "nightborne/NightBorneAttackFrame1.png",
        framesAreReversed = true,
        frameSize = 60,
        drawWidth = 3f,
        drawHeight = 3f,
        offsetX = +1.5f
    )
}
