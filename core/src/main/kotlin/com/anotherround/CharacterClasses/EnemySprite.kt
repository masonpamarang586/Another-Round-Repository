package com.anotherround.CharacterClasses

import com.badlogic.gdx.utils.viewport.Viewport

class EnemySprite(
    viewport: Viewport,
    kind: EnemyKind
) : BaseSprite(
    viewport,
    enemyConfig(kind)
)
