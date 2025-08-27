package com.nexday.app.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nexday.app.R
import com.nexday.app.data.database.DifficultyLevel

/**
 * Custom view that displays animated XP gain feedback when tasks are completed
 * Shows "+X XP" text with smooth fade in, scale up, and fade out animation
 */
class XPAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    private var animationTextView: TextView
    private var isAnimating = false
    
    companion object {
        private const val ANIMATION_TOTAL_DURATION = 1800L
        private const val FADE_IN_DURATION = 200L
        private const val SCALE_UP_DURATION = 300L
        private const val HOLD_DURATION = 600L
        private const val FADE_OUT_DURATION = 700L
        private const val SCALE_MAX = 1.4f
        private const val TRANSLATION_Y_DISTANCE = -100f
        private const val INITIAL_SCALE = 0.6f
    }
    
    init {
        // Create the animated text view
        animationTextView = TextView(context).apply {
            layoutParams = LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            alpha = 0f
            visibility = View.INVISIBLE
            elevation = 8f // Add elevation for better visual hierarchy
        }
        
        addView(animationTextView)
        
        // Make this view non-clickable so it doesn't interfere with underlying views
        isClickable = false
        isFocusable = false
    }
    
    /**
     * Show XP gain animation for completing a task
     */
    fun showXPGain(difficulty: DifficultyLevel) {
        if (isAnimating) return // Prevent multiple animations
        
        val xpAmount = difficulty.xpValue
        animateXPGain(xpAmount, true)
    }
    
    /**
     * Show XP loss animation for uncompleting a task
     */
    fun showXPLoss(difficulty: DifficultyLevel) {
        if (isAnimating) return // Prevent multiple animations
        
        val xpAmount = difficulty.xpValue
        animateXPGain(xpAmount, false)
    }
    
    private fun animateXPGain(xpAmount: Int, isGain: Boolean) {
        isAnimating = true
        
        // Set text and color based on gain/loss
        val text = if (isGain) "+$xpAmount XP" else "-$xpAmount XP"
        val color = if (isGain) {
            ContextCompat.getColor(context, R.color.success_green)
        } else {
            ContextCompat.getColor(context, R.color.error_red)
        }
        
        animationTextView.apply {
            this.text = text
            setTextColor(color)
            alpha = 0f
            scaleX = INITIAL_SCALE
            scaleY = INITIAL_SCALE
            translationY = 0f
            visibility = View.VISIBLE
        }
        
        // Phase 1: Fade in and scale up with bounce
        val fadeIn = ObjectAnimator.ofFloat(animationTextView, "alpha", 0f, 1f).apply {
            duration = FADE_IN_DURATION
            interpolator = DecelerateInterpolator()
        }
        
        val scaleUpX = ObjectAnimator.ofFloat(animationTextView, "scaleX", INITIAL_SCALE, SCALE_MAX).apply {
            duration = SCALE_UP_DURATION
            interpolator = OvershootInterpolator(1.2f)
        }
        
        val scaleUpY = ObjectAnimator.ofFloat(animationTextView, "scaleY", INITIAL_SCALE, SCALE_MAX).apply {
            duration = SCALE_UP_DURATION
            interpolator = OvershootInterpolator(1.2f)
        }
        
        // Phase 2: Float upward with smooth motion
        val translateUp = ObjectAnimator.ofFloat(animationTextView, "translationY", 0f, TRANSLATION_Y_DISTANCE).apply {
            duration = ANIMATION_TOTAL_DURATION
            interpolator = DecelerateInterpolator(1.5f)
        }
        
        // Phase 3: Scale down to normal size
        val scaleDownX = ObjectAnimator.ofFloat(animationTextView, "scaleX", SCALE_MAX, 1.1f).apply {
            duration = SCALE_UP_DURATION
            startDelay = SCALE_UP_DURATION
            interpolator = DecelerateInterpolator()
        }
        
        val scaleDownY = ObjectAnimator.ofFloat(animationTextView, "scaleY", SCALE_MAX, 1.1f).apply {
            duration = SCALE_UP_DURATION
            startDelay = SCALE_UP_DURATION
            interpolator = DecelerateInterpolator()
        }
        
        // Phase 4: Fade out gracefully
        val fadeOut = ObjectAnimator.ofFloat(animationTextView, "alpha", 1f, 0f).apply {
            duration = FADE_OUT_DURATION
            startDelay = ANIMATION_TOTAL_DURATION - FADE_OUT_DURATION
            interpolator = DecelerateInterpolator(2f)
        }
        
        // Create sequenced animation set
        val animatorSet = AnimatorSet().apply {
            playTogether(
                fadeIn,
                scaleUpX,
                scaleUpY,
                translateUp,
                scaleDownX,
                scaleDownY,
                fadeOut
            )
        }
        
        animatorSet.addListener(object : android.animation.Animator.AnimatorListener {
            override fun onAnimationStart(animation: android.animation.Animator) {
                // Add subtle bounce effect to the parent view
                this@XPAnimationView.animate()
                    .scaleX(1.02f)
                    .scaleY(1.02f)
                    .setDuration(150)
                    .withEndAction {
                        this@XPAnimationView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(150)
                    }
            }
            
            override fun onAnimationEnd(animation: android.animation.Animator) {
                animationTextView.visibility = View.INVISIBLE
                isAnimating = false
            }
            
            override fun onAnimationCancel(animation: android.animation.Animator) {
                animationTextView.visibility = View.INVISIBLE
                isAnimating = false
            }
            
            override fun onAnimationRepeat(animation: android.animation.Animator) {}
        })
        
        animatorSet.start()
    }
    
    /**
     * Check if animation is currently running
     */
    fun isAnimationRunning(): Boolean = isAnimating
}