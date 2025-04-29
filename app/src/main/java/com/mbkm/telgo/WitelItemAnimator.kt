package com.mbkm.telgo

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.View
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView

/**
 * Custom item animator untuk RecyclerView yang menambahkan animasi pada item
 */
class WitelItemAnimator : DefaultItemAnimator() {

    override fun animateAdd(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.alpha = 0f
        holder.itemView.translationY = holder.itemView.height.toFloat()

        val animator = ObjectAnimator.ofFloat(holder.itemView, View.TRANSLATION_Y, 0f)
        animator.duration = 300
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                holder.itemView.animate().alpha(1f).setDuration(200).start()
            }

            override fun onAnimationEnd(animation: Animator) {
                dispatchAddFinished(holder)
            }
        })

        animator.start()
        return true
    }

    override fun animateRemove(holder: RecyclerView.ViewHolder): Boolean {
        holder.itemView.animate()
            .alpha(0f)
            .translationX(holder.itemView.width.toFloat())
            .setDuration(300)
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    dispatchRemoveFinished(holder)
                }
            })
            .start()
        return true
    }
}