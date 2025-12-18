package com.dadabarbie.TruckTrip.views

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import com.dadabarbie.TruckTrip.Utils.Prefs
import com.dadabarbie.TruckTrip.views.CoachMarkView

object CoachMarkHelper {

    private const val PREF_COACH_MARK_SHOWN = "coach_mark_shown_main_activity"

    fun shouldShowCoachMark(activity: Activity): Boolean {
        return Prefs[PREF_COACH_MARK_SHOWN, false] == false
    }

    fun markCoachMarkAsShown(activity: Activity) {
        Prefs[PREF_COACH_MARK_SHOWN] = true
    }

    fun showCoachMarks(
        activity: Activity,
        targets: List<CoachMarkTarget>,
        onDismiss: () -> Unit = {}
    ) {
        if (targets.isEmpty()) {
            onDismiss()
            return
        }

        var currentIndex = 0

        fun showNext() {
            if (currentIndex >= targets.size) {
                markCoachMarkAsShown(activity)
                onDismiss()
                return
            }

            val target = targets[currentIndex]
            val rootView = activity.window.decorView as ViewGroup

            val coachMarkView = CoachMarkView(activity).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Wait for layout to complete before setting target
                post {
                    setTarget(target.view, target.text, target.direction)
                }

                setOnClickListener {
                    // Remove this coach mark
                    rootView.removeView(this)

                    // Show next
                    currentIndex++
                    showNext()
                }
            }

            rootView.addView(coachMarkView)
        }

        showNext()
    }

    data class CoachMarkTarget(
        val view: View,
        val text: String,
        val direction: CoachMarkView.ArrowDirection = CoachMarkView.ArrowDirection.BOTTOM
    )
}