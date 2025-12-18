package com.dadabarbie.TruckTrip.views

// 1. First, create a custom CoachMarkView class

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout

class CoachMarkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var targetView: View? = null
    private var targetRect = RectF()
    private var coachText = ""
    private var arrowDirection = ArrowDirection.BOTTOM // Arrow pointing down to target

    enum class ArrowDirection {
        TOP, BOTTOM, LEFT, RIGHT
    }

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true

        // Semi-transparent overlay
        paint.color = Color.parseColor("#CC000000")
        paint.style = Paint.Style.FILL

        // Arrow paint
        arrowPaint.color = Color.WHITE
        arrowPaint.style = Paint.Style.FILL

        // Text paint
        textPaint.color = Color.WHITE
        textPaint.textSize = 48f
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setTarget(view: View, text: String, direction: ArrowDirection = ArrowDirection.BOTTOM) {
        targetView = view
        coachText = text
        arrowDirection = direction

        // Get target view position
        val location = IntArray(2)
        view.getLocationInWindow(location)

        targetRect.set(
            location[0].toFloat(),
            location[1].toFloat(),
            (location[0] + view.width).toFloat(),
            (location[1] + view.height).toFloat()
        )

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (targetView == null) return

        // Draw overlay with hole
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)

            // Create rounded rectangle hole for target
            val cornerRadius = 16f
            addRoundRect(
                targetRect.left - 20,
                targetRect.top - 20,
                targetRect.right + 20,
                targetRect.bottom + 20,
                cornerRadius,
                cornerRadius,
                Path.Direction.CCW
            )
        }

        canvas.drawPath(path, paint)

        // Draw arrow and text based on direction
        when (arrowDirection) {
            ArrowDirection.BOTTOM -> drawBottomArrowAndText(canvas)
            ArrowDirection.TOP -> drawTopArrowAndText(canvas)
            ArrowDirection.LEFT -> drawLeftArrowAndText(canvas)
            ArrowDirection.RIGHT -> drawRightArrowAndText(canvas)
        }
    }

    private fun drawBottomArrowAndText(canvas: Canvas) {
        val arrowStartY = targetRect.bottom + 60
        val arrowCenterX = targetRect.centerX()

        // Draw arrow pointing down
        val arrowPath = Path().apply {
            moveTo(arrowCenterX, targetRect.bottom + 30)
            lineTo(arrowCenterX - 20, arrowStartY)
            lineTo(arrowCenterX + 20, arrowStartY)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Draw text below arrow
        val textY = arrowStartY + 80
        drawMultilineText(canvas, coachText, arrowCenterX, textY)
    }

    private fun drawTopArrowAndText(canvas: Canvas) {
        val arrowStartY = targetRect.top - 60
        val arrowCenterX = targetRect.centerX()

        // Draw arrow pointing up
        val arrowPath = Path().apply {
            moveTo(arrowCenterX, targetRect.top - 30)
            lineTo(arrowCenterX - 20, arrowStartY)
            lineTo(arrowCenterX + 20, arrowStartY)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Draw text above arrow
        val textY = arrowStartY - 40
        drawMultilineText(canvas, coachText, arrowCenterX, textY)
    }

    private fun drawLeftArrowAndText(canvas: Canvas) {
        val arrowCenterY = targetRect.centerY()
        val arrowStartX = targetRect.left - 60

        // Draw arrow pointing left
        val arrowPath = Path().apply {
            moveTo(targetRect.left - 30, arrowCenterY)
            lineTo(arrowStartX, arrowCenterY - 20)
            lineTo(arrowStartX, arrowCenterY + 20)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Draw text
        val textX = arrowStartX - 100
        drawMultilineText(canvas, coachText, textX, arrowCenterY)
    }

    private fun drawRightArrowAndText(canvas: Canvas) {
        val arrowCenterY = targetRect.centerY()
        val arrowStartX = targetRect.right + 60

        // Draw arrow pointing right
        val arrowPath = Path().apply {
            moveTo(targetRect.right + 30, arrowCenterY)
            lineTo(arrowStartX, arrowCenterY - 20)
            lineTo(arrowStartX, arrowCenterY + 20)
            close()
        }
        canvas.drawPath(arrowPath, arrowPaint)

        // Draw text
        val textX = arrowStartX + 100
        drawMultilineText(canvas, coachText, textX, arrowCenterY)
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Float, y: Float) {
        val maxWidth = width - 100f
        val words = text.split(" ")
        var line = ""
        var lineY = y

        for (word in words) {
            val testLine = if (line.isEmpty()) word else "$line $word"
            val textWidth = textPaint.measureText(testLine)

            if (textWidth > maxWidth && line.isNotEmpty()) {
                canvas.drawText(line, x, lineY, textPaint)
                line = word
                lineY += 60
            } else {
                line = testLine
            }
        }

        if (line.isNotEmpty()) {
            canvas.drawText(line, x, lineY, textPaint)
        }
    }
}