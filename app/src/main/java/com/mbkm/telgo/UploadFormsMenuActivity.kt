package com.mbkm.telgo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat

class UploadFormsMenuActivity : AppCompatActivity() {

    private lateinit var btnBaSurveyMiniOlt: Button
    private lateinit var btnBaSurveyBigOlt: Button
    private lateinit var btnCAF: Button
    private lateinit var btnBack: ImageButton
    private lateinit var formCard: CardView
    private lateinit var infoCard: CardView
    private lateinit var infoContentLayout: LinearLayout
    private lateinit var btnExpandInfo: ImageButton
    private var isInfoExpanded = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_forms_menu)

        // Initialize UI components
        btnBaSurveyMiniOlt = findViewById(R.id.btnBaSurveyMiniOlt)
        btnBaSurveyBigOlt = findViewById(R.id.btnBaSurveyBigOlt)
        btnCAF = findViewById(R.id.btnCAF)
        btnBack = findViewById(R.id.btnBack)
        formCard = findViewById(R.id.formCard)
        infoCard = findViewById(R.id.infoCard)
        infoContentLayout = findViewById(R.id.infoContentLayout)
        btnExpandInfo = findViewById(R.id.btnExpandInfo)

        // Set up button click listeners
        setupButtonListeners()

        // Apply entrance animations
        animateEntrance()
    }

    private fun animateEntrance() {
        val toolbarLayout = findViewById<View>(R.id.toolbarLayout)

        // Animate toolbar from top
        toolbarLayout.translationY = -200f
        toolbarLayout.alpha = 0f
        toolbarLayout.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animate form card from left
        formCard.translationX = -1000f
        formCard.animate()
            .translationX(0f)
            .setDuration(600)
            .setStartDelay(300)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Animate info card from right
        infoCard.translationX = 1000f
        infoCard.animate()
            .translationX(0f)
            .setDuration(600)
            .setStartDelay(500)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // Apply button animations with sequence
        val buttons = listOf(btnBaSurveyMiniOlt, btnBaSurveyBigOlt, btnCAF)
        val animatorSet = AnimatorSet()
        val animators = buttons.mapIndexed { index, button ->
            button.alpha = 0f
            ObjectAnimator.ofFloat(button, "alpha", 0f, 1f).apply {
                startDelay = 600L + (index * 150L)
                duration = 300
            }
        }
        animatorSet.playTogether(animators)
        animatorSet.start()
    }

    private fun setupButtonListeners() {
        // Set elevation and shadow for buttons to make them appear more interactive
        listOf(btnBaSurveyMiniOlt, btnBaSurveyBigOlt, btnCAF).forEach { button ->
            ViewCompat.setElevation(button, 8f)
        }

        btnBaSurveyMiniOlt.setOnClickListener {
            animateButtonClick(it)
            it.postDelayed({
                val intent = Intent(this, BaSurveyMiniOltActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 200)
        }

        btnBaSurveyBigOlt.setOnClickListener {
            animateButtonClick(it)
            it.postDelayed({
                val intent = Intent(this, BASurveyBigActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 200)
        }

        btnCAF.setOnClickListener {
            animateButtonClick(it)
            it.postDelayed({
                val intent = Intent(this, CAFActivity::class.java)
                startActivity(intent)
                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
            }, 200)
        }

        btnBack.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }

        btnExpandInfo.setOnClickListener {
            toggleInfoCard()
        }

        // Add touch feedback to cards
        formCard.setOnClickListener {
            val animation = AnimationUtils.loadAnimation(this, R.anim.card_pulse)
            formCard.startAnimation(animation)
        }
    }

    private fun animateButtonClick(view: View) {
        // Scale down slightly
        view.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
            .setDuration(100)
            .withEndAction {
                // Scale back to normal
                view.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .start()
            }
            .start()

        // Also add the original button animation
        val animation = AnimationUtils.loadAnimation(this, R.anim.button_animation)
        view.startAnimation(animation)
    }

    private fun toggleInfoCard() {
        isInfoExpanded = !isInfoExpanded

        if (isInfoExpanded) {
            // Expand info content
            infoContentLayout.visibility = View.VISIBLE
            btnExpandInfo.setImageResource(R.drawable.ic_arrow_up)

            // Animate height change
            infoContentLayout.measure(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            val targetHeight = infoContentLayout.measuredHeight

            infoContentLayout.layoutParams.height = 1
            infoContentLayout.alpha = 0f

            val animator = ValueAnimator.ofInt(1, targetHeight)
            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                infoContentLayout.layoutParams.height = value
                infoContentLayout.requestLayout()
                infoContentLayout.alpha = animation.animatedFraction
            }
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.start()
        } else {
            // Collapse info content
            btnExpandInfo.setImageResource(R.drawable.ic_arrow_down)

            val initialHeight = infoContentLayout.height

            val animator = ValueAnimator.ofInt(initialHeight, 0)
            animator.addUpdateListener { animation ->
                val value = animation.animatedValue as Int
                infoContentLayout.layoutParams.height = value
                infoContentLayout.requestLayout()
                infoContentLayout.alpha = 1 - animation.animatedFraction
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    infoContentLayout.visibility = View.GONE
                }
            })
            animator.duration = 300
            animator.interpolator = DecelerateInterpolator()
            animator.start()
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}