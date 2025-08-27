package com.nexday.app.ui.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.random.Random

/**
 * Custom view that renders confetti particles using Canvas
 * Provides smooth, performant particle animations for celebrations
 */
class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    
    private val particles = mutableListOf<ConfettiParticle>()
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }
    private val textBounds = Rect()
    
    private var animator: ValueAnimator? = null
    private var lastUpdateTime = 0L
    private var isAnimating = false
    
    companion object {
        private const val PARTICLE_COUNT = 25
        private const val SPAWN_DURATION_MS = 2000L // Spawn particles over 2 seconds
        private const val ANIMATION_DURATION_MS = 6000L // Total animation time (longer than dialog)
    }
    
    /**
     * Start the confetti animation
     */
    fun startConfetti() {
        if (isAnimating) return
        
        isAnimating = true
        particles.clear()
        lastUpdateTime = System.currentTimeMillis()
        
        // Create animator to drive the animation loop
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION_MS
            interpolator = LinearInterpolator()
            
            addUpdateListener { 
                updateParticles()
                invalidate() // Trigger onDraw
            }
            
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimating = false
                    particles.clear()
                    invalidate()
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    isAnimating = false
                    particles.clear()
                    invalidate()
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
            
            start()
        }
    }
    
    /**
     * Stop the confetti animation
     */
    fun stopConfetti() {
        animator?.cancel()
        animator = null
        isAnimating = false
        particles.clear()
        invalidate()
    }
    
    /**
     * Update particle physics and spawn new particles
     */
    private fun updateParticles() {
        val currentTime = System.currentTimeMillis()
        val deltaTime = (currentTime - lastUpdateTime) / 1000f
        lastUpdateTime = currentTime
        
        // Spawn new particles during the first part of the animation
        val animationProgress = (animator?.animatedValue as? Float) ?: 0f
        if (animationProgress < (SPAWN_DURATION_MS.toFloat() / ANIMATION_DURATION_MS)) {
            spawnParticles(deltaTime)
        }
        
        // Update existing particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val particle = iterator.next()
            particle.update(deltaTime)
            
            // Remove dead particles
            if (!particle.isAlive(height.toFloat())) {
                iterator.remove()
            }
        }
    }
    
    /**
     * Spawn new particles based on timing
     */
    private fun spawnParticles(deltaTime: Float) {
        // Calculate how many particles to spawn this frame
        val spawnRate = PARTICLE_COUNT.toFloat() / (SPAWN_DURATION_MS / 1000f)
        val particlesToSpawn = (spawnRate * deltaTime).toInt()
        
        // Add some randomness to spawning
        val extraParticle = if (Random.nextFloat() < (spawnRate * deltaTime) % 1f) 1 else 0
        
        repeat(particlesToSpawn + extraParticle) {
            if (particles.size < PARTICLE_COUNT * 2) { // Allow some overflow
                particles.add(ConfettiParticle.createRandom(width.toFloat()))
            }
        }
    }
    
    /**
     * Draw all particles on the canvas
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isAnimating || particles.isEmpty()) return
        
        for (particle in particles) {
            drawParticle(canvas, particle)
        }
    }
    
    /**
     * Draw a single particle (emoji) on the canvas
     */
    private fun drawParticle(canvas: Canvas, particle: ConfettiParticle) {
        if (particle.alpha <= 0f) return
        
        canvas.save()
        
        // Move to particle position
        canvas.translate(particle.x, particle.y)
        
        // Rotate around particle center
        canvas.rotate(particle.rotation)
        
        // Set paint properties
        textPaint.textSize = particle.size
        textPaint.alpha = (particle.alpha * 255).toInt()
        
        // Get text bounds for centering
        textPaint.getTextBounds(particle.emoji, 0, particle.emoji.length, textBounds)
        
        // Draw the emoji centered at the particle position
        canvas.drawText(
            particle.emoji,
            0f,
            textBounds.height() / 2f,
            textPaint
        )
        
        canvas.restore()
    }
    
    /**
     * Clean up when view is detached
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopConfetti()
    }
    
    /**
     * Check if confetti animation is currently running
     */
    fun isAnimating(): Boolean = isAnimating
}