package com.dadabarbie.TruckTrip.Utils

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator

object ShimmerUtils {

    fun applyShimmer(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyShimmer(view.getChildAt(i))
            }
        } else {
            val existing = view.getTag()
            if (existing is ObjectAnimator) {
                existing.cancel()
            }
            val animator = ObjectAnimator.ofFloat(view, "alpha", 0.35f, 0.95f).apply {
                duration = 750
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            view.setTag(animator)
        }
    }

    fun stopShimmer(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                stopShimmer(view.getChildAt(i))
            }
        } else {
            val tag = view.getTag()
            if (tag is ObjectAnimator) {
                tag.cancel()
                view.alpha = 1.0f
                view.setTag(null)
            }
        }
    }
}
