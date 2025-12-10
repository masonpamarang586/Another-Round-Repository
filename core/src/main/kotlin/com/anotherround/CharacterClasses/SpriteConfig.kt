package com.anotherround.CharacterClasses

data class SpriteConfig(
    val idlePath: String,
    val damageRowPath: String,
    val deathRowPath: String,
    val attackRowPath: String,

    val frameSize: Int = 60,
    val framesAreReversed: Boolean = false,

    val idleFps: Float = 4f,
    val hurtFps: Float = 4f,
    val deathFps: Float = 12f,
    val attackFps: Float = 8f,

    val drawWidth: Float = 3f,
    val drawHeight: Float = 3f,

    val offsetX: Float = 0f,
    val offsetY: Float = 0f,

    val endHoldAttack: Float = 0.5f,
    val endHoldHurt: Float = 0.3f,
    val endHoldDeath: Float = 1.0f
)
