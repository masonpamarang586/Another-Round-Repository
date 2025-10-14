package com.comp362.anotherround.system

import com.badlogic.gdx.scenes.scene2d.Stage
import com.github.quillraven.fleks.*
import com.comp362.anotherround.component.ImageComponent
import com.github.quillraven.fleks.collection.compareEntity

@AllOf(components = [ImageComponent::class])
class RenderSystem(
    private val stage: Stage,
    private val imageCmps:ComponentMapper<ImageComponent>


) : IteratingSystem(
    // Compares two entities by their image position
    comparator = compareEntity{e1, e2 -> imageCmps[e1].compareTo(imageCmps[e2]) }
) {

    override fun onTick() {
        super.onTick()

        with(stage){
            viewport.apply()
            act(deltaTime)
            draw()
        }
    }

    override fun onTickEntity(entity: Entity) {
        imageCmps[entity].image.toFront()
    }

}
