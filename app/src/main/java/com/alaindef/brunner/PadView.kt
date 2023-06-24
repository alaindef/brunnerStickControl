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

    var multiplier = VectorF(1f, 1f)

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

    fun newTarget(posPixel: VectorF, size: VectorF) {
        // posPixel and size are in pixels. tpos is 0..1f
        val tpos = ((posPixel.divideBy(size)).maxOf(VectorF(0f, 0f)).minOf(VectorF(1f, 1f)))
        target.setPosV(tpos)
    }

    private fun sendForces(posPixel: VectorF, size: VectorF) {
        // TEST:
        // we also set the force according to target position relative to the center of the pad
        // this wil not affect operation while running/polling
        // it is used to send that force to the stick, for testing purposes
        val tpos = ((posPixel.divideBy(size)).maxOf(VectorF(0f, 0f)).minOf(VectorF(1f, 1f)))
        if (!PollMaster.running) {
            Forces.forces = (VectorF(0.5f, 0.5f) minus tpos) mul multiplier
            udpSender.sendUDP(Forces.forces.x, Forces.forces.y)
        }
        // the following us to track the stic, but does not work (because no polling going on)
//        val res = UdpRecObject.getCoordinates(0)
//        Main.stickPad!!.stick.setPosV(res)
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
                newTarget(posPixel, size)
                path.moveTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_MOVE -> {
                newTarget(posPixel, size)
                paint.color = Color.BLUE
                path.lineTo(xPixel, yPixel)
            }
            MotionEvent.ACTION_UP -> {
                path.reset()
                sendForces(posPixel, size)
            }
            else -> return false
        }
        // Force a redraw of the view
        invalidate()
        return true
    }
}

