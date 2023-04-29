package com.alaindef.state

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent

class PadView(context: Context, attrs: AttributeSet) :
    androidx.appcompat.widget.AppCompatImageView(context, attrs) {

//    private val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//    private val canvas = Canvas(bitmap)

    public val path = Path()
    private val paint = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = Color.BLUE
    }

    private fun drawPoint(x: Float, y: Float) {
        val paint = Paint()
        paint.color = Color.RED
        paint.strokeWidth = 10f

//        canvas.drawPoint(x, y, paint)
        invalidate()
    }

    data class Vector(val x: Float, val y: Float)

    fun useTargetXY(x: Float, y: Float, padWidth: Float, padHeight: Float): Vector {
        val xRel = (minOf(maxOf(((x * 100F) / padWidth), 0F), 100F)) / 100f
        val yRel = (minOf(maxOf(((y * 100F) / padHeight), 0F), 100F)) / 100f

        sendy.xTarget = xRel
        sendy.yTarget = yRel
        sendy.send(PollMaster.EV_4_target_pos)
        Main.mReport2!!.text =
            "TARGET: ${String.format("%.${2}f", xRel)}  ${String.format("%.${2}f", yRel)})"
        return Vector(xRel, yRel)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
//        canvas?.drawBitmap(bitmap, 0f, 0f, null)
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
                path.lineTo(x, y)
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
                val xView = minOf(padWidth, maxOf(0f, x))
                val yView = minOf(padHeight, maxOf(0f, y))
                path.addCircle(xView, yView, 15f, Path.Direction.CCW)
//                Main.mReport4!!.text = "xt ======"
                Main.mReport4b!!.text = "$x"

                val xci = sendy.xCurrent
                val yci = sendy.yCurrent
                val xc = sendy.xCurrent * padWidth
                val yc = sendy.yCurrent * padHeight
                Main.mReport5!!.text = "current pos"
                Main.mReport5b!!.text = "($x $y)    ($xci $yci)    ($xc $yc)"
                paint.setColor(Color.BLUE)
                path.addCircle(xc, yc, 15f, Path.Direction.CCW)
                paint.setColor(Color.RED)
            }
            else -> return false
        }

        fun markCurrentPos():Int {
            return 117
        }

        // Force a redraw of the view
        invalidate()

        return true
    }
}