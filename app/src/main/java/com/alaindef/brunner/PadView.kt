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
    val target = PositionRel("TARGET", VectorF(.2f, 0f), PositionRel.targetColor)

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

    fun sendTarget(posPixel: VectorF, size: VectorF) {
        // posPixel and size are in pixels. tpos is 0..1f
        val tpos = ((posPixel.divideBy(size)).maxOf(VectorF(0f, 0f)).minOf(VectorF(1f, 1f)))
        val tposIndex = (tpos mul VectorF(100f,100f)).toIntVector()
        target.setPosV(tpos)

        val corrp =Forces.correctInterpol(tpos)
        val tpText = "(%.2f %.2f)".format(target.pos.x, target.pos.y)
        val corrpText = "(%.2f %.2f)".format(corrp.x, corrp.y)
        "$tpText $corrpText".also { Main.mReport2!!.text = it }

        val repPos = "(%.2f %.2f)".format(tpos.x, tpos.y)
        val repCorTar = "(%.2f %.2f)".format(Forces.corrected.x, Forces.corrected.y)
        val corrections = Forces.corrections[tposIndex.x][tposIndex.y]
        val repCorrections = "(%.2f %.2f)".format(corrections.x, corrections.y)
        "$repPos  $repCorTar  $repCorrections".also { Main.mReport5!!.text = it }
        "(target pos)  (corrected target)  (corrections)".also { Main.mReport5a!!.text = it }
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
        val posPixel = VectorF(event.x, event.y)
        val size = VectorF(width.toFloat(), height.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
//                Main.mReport5a!!.text = "action down ($xPixel $yPixel)"
                sendTarget(posPixel, size)
                path.moveTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_MOVE -> {
//                Main.mReport5!!.text = "action move ($xPixel $yPixel)"
                sendTarget(posPixel, size)
                paint.color = Color.BLUE
                path.lineTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
            }
            else -> return false
        }
        // Force a redraw of the view
        invalidate()
        return true
    }
}

