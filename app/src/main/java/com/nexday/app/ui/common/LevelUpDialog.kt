package com.nexday.app.ui.common

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.BounceInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.nexday.app.R

/**
 * Full-screen celebration dialog that appears when user levels up
 * Features Canvas-based confetti particle animations and congratulatory message
 */
class LevelUpDialog(
    context: Context,
    private val previousLevel: Int,
    private val newLevel: Int,
    private val onDismiss: () -> Unit = {}
) : Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen) {
    
    private lateinit var rootLayout: FrameLayout
    private lateinit var contentLayout: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var levelText: TextView
    private lateinit var messageText: TextView
    private lateinit var confettiView: ConfettiView
    
    companion object {
        private const val CELEBRATION_DURATION = 5000L // Total time before fade starts
        private const val FADE_OUT_DURATION = 1000L // Fade out duration
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Remove window decorations for full-screen effect
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        
        setupLayout()
        setCancelable(true)
        setCanceledOnTouchOutside(true)
        
        setOnDismissListener {
            confettiView.stopConfetti()
            onDismiss()
        }
    }
    
    private fun setupLayout() {
        // Create root FrameLayout container
        rootLayout = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#AA000000")) // Semi-transparent overlay
        }
        
        // Create confetti view (behind content)
        confettiView = ConfettiView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        
        // Create content layout for text
        contentLayout = LinearLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(64, 64, 64, 64)
        }
        
        // Title text
        titleText = TextView(context).apply {
            text = "🎉 LEVEL UP! 🎉"
            textSize = 32f
            setTextColor(ContextCompat.getColor(context, R.color.primary))
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            alpha = 0f
        }
        
        // Level information
        levelText = TextView(context).apply {
            text = "Level $newLevel"
            textSize = 48f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setTypeface(null, android.graphics.Typeface.BOLD)
            alpha = 0f
        }
        
        // Congratulatory message
        messageText = TextView(context).apply {
            text = getCongratulatoryMessage()
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            alpha = 0f
            setPadding(0, 32, 0, 0)
        }
        
        // Add text views to content layout
        contentLayout.addView(titleText)
        contentLayout.addView(levelText)
        contentLayout.addView(messageText)
        
        // Add confetti and content to root layout
        rootLayout.addView(confettiView) // Behind content
        rootLayout.addView(contentLayout) // In front of confetti
        
        setContentView(rootLayout)
    }
    
    private fun getCongratulatoryMessage(): String {
        return when {
            newLevel <= 5 -> "Great start! Keep completing those tasks!"
            newLevel <= 10 -> "You're on fire! Productivity level up!"
            newLevel <= 20 -> "Unstoppable! You're becoming a task master!"
            newLevel <= 50 -> "Incredible dedication! You're a productivity pro!"
            else -> "Legendary! You've achieved peak productivity!"
        }
    }
    
    override fun show() {
        super.show()
        startCelebrationAnimation()
    }
    
    private fun startCelebrationAnimation() {
        // Start confetti animation immediately
        confettiView.startConfetti()
        
        // Animate text elements
        animateTextElements()
        
        // Start fade-out after celebration duration
        rootLayout.postDelayed({
            if (isShowing) {
                startFadeOut()
            }
        }, CELEBRATION_DURATION)
    }
    
    private fun startFadeOut() {
        // Fade out the entire dialog smoothly
        val fadeOutAnimator = ObjectAnimator.ofFloat(rootLayout, "alpha", 1f, 0f).apply {
            duration = FADE_OUT_DURATION
            interpolator = AccelerateDecelerateInterpolator()
            
            addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (isShowing) {
                        dismiss()
                    }
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {
                    if (isShowing) {
                        dismiss()
                    }
                }
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })
        }
        
        fadeOutAnimator.start()
    }
    
    private fun animateTextElements() {
        // Title animation with bounce
        val titleFadeIn = ObjectAnimator.ofFloat(titleText, "alpha", 0f, 1f).apply {
            duration = 500
            interpolator = BounceInterpolator()
        }
        
        val titleScale = ObjectAnimator.ofPropertyValuesHolder(
            titleText,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.5f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.5f, 1f)
        ).apply {
            duration = 600
            interpolator = BounceInterpolator()
        }
        
        // Level text animation
        val levelFadeIn = ObjectAnimator.ofFloat(levelText, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 300
        }
        
        val levelScale = ObjectAnimator.ofPropertyValuesHolder(
            levelText,
            android.animation.PropertyValuesHolder.ofFloat("scaleX", 0.3f, 1.2f, 1f),
            android.animation.PropertyValuesHolder.ofFloat("scaleY", 0.3f, 1.2f, 1f)
        ).apply {
            duration = 800
            startDelay = 300
            interpolator = AccelerateDecelerateInterpolator()
        }
        
        // Message animation
        val messageFadeIn = ObjectAnimator.ofFloat(messageText, "alpha", 0f, 1f).apply {
            duration = 500
            startDelay = 800
        }
        
        // Start all text animations
        AnimatorSet().apply {
            playTogether(titleFadeIn, titleScale, levelFadeIn, levelScale, messageFadeIn)
            start()
        }
    }
}