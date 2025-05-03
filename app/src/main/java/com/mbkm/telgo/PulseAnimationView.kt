package com.mbkm.telgo

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

class PulseAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private val handler = Handler(Looper.getMainLooper())
    private var isAnimating = false

    fun startPulseAnimation() {
        if (isAnimating) return
        isAnimating = true

        val scaleX = ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.2f, 1f)
        val scaleY = ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.2f, 1f)
        val alpha = ObjectAnimator.ofFloat(this, View.ALPHA, 0.6f, 0.3f, 0.6f)

        val animatorSet = AnimatorSet().apply {
            playTogether(scaleX, scaleY, alpha)
            duration = 1500
            interpolator = AccelerateDecelerateInterpolator()
        }

        animatorSet.start()

        handler.postDelayed({
            if (isAnimating) {
                startPulseAnimation()
            }
        }, 1500)
    }

    fun stopPulseAnimation() {
        isAnimating = false
        handler.removeCallbacksAndMessages(null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startPulseAnimation()
    }

    override fun onDetachedFromWindow() {
        stopPulseAnimation()
        super.onDetachedFromWindow()
    }
}