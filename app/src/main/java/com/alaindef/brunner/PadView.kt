package com.alaindef.brunner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent

class PadView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) :
    com.google.android.material.imageview.ShapeableImageView(context, attrs, defStyleAttr) {

    val stickPos = PositionRel("STICK", .2f, .05f, PositionRel.currentColor)
    val targetPos = PositionRel("TARGET", .6f, 0f, PositionRel.targetColor)

    private val path = Path()
    private val canvas = Canvas()
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
    private val paintBlue = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = Color.BLUE
    }

    fun sendTarget(xPix: Float, yPix: Float, padWidth: Float, padHeight: Float): Vector {
        val xRel = (minOf(maxOf((xPix / padWidth), 0F), 0.99F))
        val yRel = (minOf(maxOf((yPix / padHeight), 0F), 0.99F))
        Main.mReport2!!.text =
            "(${String.format("%.${2}f", xRel)}  ${String.format("%.${2}f", yRel)})"

        Forces.targetRel = Vector(xRel, yRel)
        invalidate()
        return Vector(xRel, yRel)
    }

    override fun onDraw(canvas: Canvas) {
        val padWidth = width.toFloat()
        val padHeight = height.toFloat()
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
        //old
        //old
        showPos(targetPos.x, targetPos.y, targetPos.color)
        showPos(stickPos.x, stickPos.y, stickPos.color)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val xPixel = event.x
        val yPixel = event.y
        val padWidth = width.toFloat()
        val padHeight = height.toFloat()

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Main.mReport5a!!.text = "action down ($xPixel $yPixel)"
                sendTarget(xPixel, yPixel, padWidth, padHeight)
                path.moveTo(xPixel, yPixel)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                sendTarget(xPixel, yPixel, padWidth, padHeight)
                paint.color = Color.BLUE
                path.lineTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_UP -> {
                drawTargetP(xPixel, yPixel)
                Main.mReport5a!!.text = "done"
//                path.reset()
//                val xView = minOf(padWidth, maxOf(0f, x))
//                val yView = minOf(padHeight, maxOf(0f, y))
//          //draw a small circle to show the target position
//                paint.setColor(Color.RED)
//                path.addCircle(xView, yView, 15f, Path.Direction.CCW)
            }
            else -> return false
        }
        // Force a redraw of the view
        invalidate()

        return true
    }

    fun drawTargetP(xPixel: Float, yPixel: Float) {
        path.reset()
        val xView = minOf(width.toFloat(), maxOf(0f, xPixel))
        val yView = minOf(height.toFloat(), maxOf(0f, yPixel))
//          draw a small circle to show the target position
        paint.setColor(Color.BLUE)
//        canvas.drawCircle(xView, yView, 15f, paint)
        path.addCircle(xView, yView, 15f, Path.Direction.CCW)
        invalidate()
    }

    fun drawTarget(xRel: Float, yRel: Float) {
        drawTargetP(xRel * width, yRel * height)
//        Forces.targetRel = Vector(xRel, yRel)
    }

    fun showPosP(xPixel: Float, yPixel: Float, color: Int) {
        path.reset()
        val xView = minOf(width.toFloat(), maxOf(0f, xPixel))
        val yView = minOf(height.toFloat(), maxOf(0f, yPixel))
//          draw a small circle to show the target position
        paint.setColor(color)
//        canvas.drawCircle(xView, yView, 15f, paint)
        path.addCircle(xView, yView, 15f, Path.Direction.CCW)
//        invalidate()
    }

    fun showPos(xRel: Float, yRel: Float, color: Int) {
        showPosP(xRel * width, yRel * height, color)
//        Forces.targetRel = Vector(xRel, yRel)
    }
}