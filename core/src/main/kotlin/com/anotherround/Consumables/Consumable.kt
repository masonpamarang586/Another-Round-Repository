package com.anotherround.Consumables

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.Json


data class Consumable (val name: String,
                       val description: String,
                       val image: String,
                       val healAmt: Int
)
