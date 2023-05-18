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

    val stick = PositionRel("STICK", VectorF(.2f, .05f), PositionRel.stickColor)
    val target = PositionRel("TARGET", VectorF(.6f, 0f), PositionRel.targetColor)

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
    private val paintBlue = Paint().apply {
        isAntiAlias = true
        isDither = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 6f
        color = Color.BLUE
    }

    fun sendTarget0(xPix: Float, yPix: Float, padWidth: Float, padHeight: Float) {
        target.setPos(
            (minOf(maxOf((xPix / padWidth), 0F), 0.99F)),
            (minOf(maxOf((yPix / padHeight), 0F), 0.99F))
        )
        Main.mReport2!!.text =
            "(${String.format("%.${2}f", target.pos.x)}  ${
                String.format("%.${2}f", target.pos.y)
            })"
        Forces.targetRel = target.pos
    }
    fun sendTarget(pos: VectorF, size: VectorF) {
        target.setPosV(
            pos.divide(size).maxOf(VectorF(0f,0f).minOf(VectorF(0.99f,0.99f))))
        Main.mReport2!!.text =
            "(${String.format("%.${2}f", target.pos.x)}  ${
                String.format("%.${2}f", target.pos.y)
            })"
        Forces.targetRel = target.pos
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
        canvas.drawCircle(stick.pos.x * width, stick.pos.y * height, 15f, paintRed)
        canvas.drawCircle(target.pos.x * width, target.pos.y * height, 15f, paintBlue)
        // do not try to put drawCircle in separate functions, you need canvas anyway.
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // move the target position
        val xPixel = event.x
        val yPixel = event.y
        val pixelpos = VectorF(event.x, event.y)
        val size = VectorF(width.toFloat(), height.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                Main.mReport5a!!.text = "action down ($xPixel $yPixel)"
                sendTarget(pixelpos, size)
                path.moveTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_MOVE -> {
                Main.mReport5!!.text = "action move ($xPixel $yPixel)"
                sendTarget(pixelpos, size)
                paint.color = Color.BLUE
                path.lineTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_UP -> {
                Main.mReport5a!!.text = "done"
                path.reset()
            }
            else -> return false
        }
        // Force a redraw of the view
        invalidate()

        return true
    }

    fun drawTargetP(xPixel: Float, yPixel: Float) {
        val xView = minOf(width.toFloat(), maxOf(0f, xPixel))
        val yView = minOf(height.toFloat(), maxOf(0f, yPixel))
//          draw a small circle to show the target position
        paint.setColor(Color.BLUE)
//        canvas.drawCircle(xView, yView, 15f, paint)
        path.addCircle(xView, yView, 15f, Path.Direction.CCW)
//        invalidate()
    }

    fun drawTarget(xRel: Float, yRel: Float) {
        drawTargetP(xRel * width, yRel * height)
//        Forces.targetRel = Vector(xRel, yRel)
    }


}