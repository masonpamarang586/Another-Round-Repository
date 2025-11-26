package com.anotherround.CharacterClasses

import com.badlogic.gdx.utils.viewport.Viewport

class PlayerSprite(viewport: Viewport) : BaseSprite(
    viewport,
    SpriteConfig(
        idlePath      = "generic_char_v0.2/png/blue/char_blue_1_index00.png",
        damageRowPath = "generic_char_v0.2/png/blue/char_blue_1_damage.png",
        deathRowPath  = "generic_char_v0.2/png/blue/char_blue_1_death.png",
        attackRowPath = "generic_char_v0.2/png/blue/blue_attack1.png",
        framesAreReversed = false,
        offsetX = -1.5f
    )
)
