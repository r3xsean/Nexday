package com.nexday.app.ui.common

import android.graphics.Color
import kotlin.random.Random

/**
 * Data class representing a single confetti particle with physics properties
 */
data class ConfettiParticle(
    var x: Float,
    var y: Float,
    var velocityX: Float,
    var velocityY: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var alpha: Float,
    val color: Int,
    val emoji: String,
    val size: Float
) {
    companion object {
        private val CONFETTI_EMOJIS = arrayOf("🎉", "✨", "🌟", "🎊", "💫", "⭐", "🔥", "🎈", "🎆", "🎁", "💎")
        
        private val CONFETTI_COLORS = arrayOf(
            Color.parseColor("#FF6B35"), // Orange
            Color.parseColor("#F7931E"), // Yellow-Orange
            Color.parseColor("#FFD23F"), // Yellow
            Color.parseColor("#06FFA5"), // Green
            Color.parseColor("#118AB2"), // Blue
            Color.parseColor("#7209B7"), // Purple
            Color.parseColor("#F72585")  // Pink
        )
        
        /**
         * Create a random confetti particle at the given position
         */
        fun createRandom(screenWidth: Float, startY: Float = -50f): ConfettiParticle {
            val random = Random.Default
            
            return ConfettiParticle(
                x = random.nextFloat() * screenWidth,
                y = startY,
                velocityX = (random.nextFloat() - 0.5f) * 200f, // Random horizontal drift
                velocityY = random.nextFloat() * 100f + 200f, // Downward velocity
                rotation = random.nextFloat() * 360f,
                rotationSpeed = (random.nextFloat() - 0.5f) * 180f, // Degrees per second
                alpha = 1f,
                color = CONFETTI_COLORS.random(),
                emoji = CONFETTI_EMOJIS.random(),
                size = random.nextFloat() * 20f + 30f // Size between 30-50
            )
        }
    }
    
    /**
     * Update particle physics for one frame
     * @param deltaTime Time since last update in seconds
     * @param gravity Gravity acceleration
     */
    fun update(deltaTime: Float, gravity: Float = 500f) {
        // Apply gravity
        velocityY += gravity * deltaTime
        
        // Update position
        x += velocityX * deltaTime
        y += velocityY * deltaTime
        
        // Update rotation
        rotation += rotationSpeed * deltaTime
        
        // Fade out over time (particles live for about 5 seconds)
        alpha = maxOf(0f, alpha - (deltaTime * 0.1f))
        
        // Add some air resistance
        velocityX *= 0.999f
        velocityY *= 0.999f
    }
    
    /**
     * Check if particle is still alive and visible
     */
    fun isAlive(screenHeight: Float): Boolean {
        return alpha > 0.1f && y < screenHeight + 100f
    }
}