package com.alaindef.brunner

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent

class PadView(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {

    private val path = Path()
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = Color.BLUE
    }
    private val paintRed = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = Color.MAGENTA
    }

    fun useTargetXY(x: Float, y: Float, padWidth: Float, padHeight: Float): Vector {
        val xRel = (minOf(maxOf(((x * 100F) / padWidth), 0F), 100F)) / 100f
        val yRel = (minOf(maxOf(((y * 100F) / padHeight), 0F), 100F)) / 100f

        sendy.xTarget = xRel
        sendy.yTarget = yRel
        sendy.send(PollMaster.EV_4_target_pos)
        Main.mReport2!!.text =
            "(${String.format("%.${2}f", xRel)}  ${String.format("%.${2}f", yRel)})"
        return Vector(xRel, yRel)
    }

    override fun onDraw(canvas: Canvas) {
        val padWidth = width.toFloat()
        val padHeight = height.toFloat()
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
        canvas?.drawCircle(sendy.xCurrent  * padWidth, sendy.yCurrent * padHeight,15f, paintRed)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        val padWidth = width.toFloat()
        val padHeight = height.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                useTargetXY(x, y, padWidth, padHeight)
                path.moveTo(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                useTargetXY(x, y, padWidth, padHeight)
                paint.color = Color.BLUE
                path.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
                val xView = minOf(padWidth, maxOf(0f, x))
                val yView = minOf(padHeight, maxOf(0f, y))
//          draw a small circle to show the target position
                paint.setColor(Color.RED)
                path.addCircle(xView, yView, 15f, Path.Direction.CCW)
            }
            else -> return false
        }
        // Force a redraw of the view
        invalidate()

        return true
    }
}