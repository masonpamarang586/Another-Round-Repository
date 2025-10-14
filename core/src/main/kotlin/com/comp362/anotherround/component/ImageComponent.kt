package com.comp362.anotherround.component

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.github.quillraven.fleks.ComponentListener
import com.github.quillraven.fleks.Entity

class ImageComponent: Comparable<ImageComponent> {
    lateinit var image: Image
    var layer = 0

    override fun compareTo(other: ImageComponent): Int {
        val yDiff = other.image.y.compareTo(image.y)
        return if(yDiff != 0){
            yDiff
        } else {
            other.image.x.compareTo(image.x)
        }
    }

    companion object {
        class ImageComponentListener(
            private val stage: Stage
        ) : ComponentListener<ImageComponent> {

            // When an entity gets an image component
            override fun onComponentAdded(
                entity: Entity,
                component: ImageComponent
            ) {
                // Adds the entity to the stage
                stage.addActor(component.image)
            }

            // When an entity loses an image component
            override fun onComponentRemoved(
                entity: Entity,
                component: ImageComponent
            ) {
                // Removes the entity from the stage
                stage.root.removeActor(component.image)
            }

        }
    }
}
