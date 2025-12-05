package com.anotherround.Consumables

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion

object PotionFactory {

    private lateinit var potionsTexture: Texture
    private lateinit var regions: Array<Array<TextureRegion>>

    fun init(texture: Texture) {
        potionsTexture = texture
        regions = TextureRegion.split(texture, 16, 16)
    }

    // Texture coordinates (row, col) based on typical RPG spritesheets or the existing one
    // Assuming 16x16 grid. 
    // Health: Red potions. 
    // Defense: Green/Brown potions.
    // Attack (Liquid Fire): Orange/Red potions.
    
    // Let's define some coordinates based on the previous code which used (48, 32) for health.
    // 48/16 = 3 (col), 32/16 = 2 (row). So row 2, col 3.
    
    // We'll pick some arbitrary ones for now, assuming a standard sheet layout.
    // Row 2, Col 3 -> Health Common
    // Row 2, Col 4 -> Health Uncommon
    // Row 2, Col 5 -> Health Rare
    
    // Row 3, Col 3 -> Defense (Lacquer)
    // Row 4, Col 3 -> Attack (Liquid Fire)

    fun createHealthPotion(rarity: PotionRarity): Potion {
        val (name, heal, colOffset) = when (rarity) {
            PotionRarity.COMMON -> Triple("Small Health Potion", 10, 0)
            PotionRarity.UNCOMMON -> Triple("Medium Health Potion", 25, 1)
            PotionRarity.RARE -> Triple("Large Health Potion", 50, 2)
        }
        
        // Use row 2 (index 2) for health potions
        val region = getRegion(2, 3 + colOffset)

        return Potion(
            name = name,
            description = "Restores $heal HP.",
            type = PotionType.HEALTH,
            rarity = rarity,
            textureRegion = region,
            effectValue = heal
        )
    }

    fun createDefensiveLacquer(): Potion {
        // Use row 3 (index 3), col 3 for defense
        val region = getRegion(3, 3)
        return Potion(
            name = "Defensive Lacquer",
            description = "Reduces next incoming damage by 20%.",
            type = PotionType.DEFENSE,
            rarity = PotionRarity.UNCOMMON,
            textureRegion = region,
            effectValue = 20, // 20%
            duration = 1 // Lasts until hit (handled as 1 turn or special logic)
        )
    }

    fun createLiquidFire(): Potion {
        // Use row 4 (index 4), col 3 for fire
        val region = getRegion(4, 3)
        return Potion(
            name = "Vial of Liquid Fire",
            description = "Burns enemy for 10 damage over 2 turns.",
            type = PotionType.ATTACK,
            rarity = PotionRarity.RARE,
            textureRegion = region,
            effectValue = 10, // Damage per turn
            duration = 2
        )
    }

    private fun getRegion(row: Int, col: Int): TextureRegion {
        if (!this::regions.isInitialized) {
            // Fallback or error, but we should ensure init is called
            return TextureRegion() 
        }
        // Safety check bounds
        val r = row.coerceIn(0, regions.size - 1)
        val c = col.coerceIn(0, regions[0].size - 1)
        return regions[r][c]
    }
}
