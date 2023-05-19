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
        Main.mReport2!!.text = "(%.2f %.2f)".format(target.pos.x, target.pos.y)
        Forces.targetRel = target.pos
    }
    fun sendTarget(pos: VectorF, size: VectorF) {
        target.setPosV(
            pos.divide(size).maxOf(VectorF(0f,0f).minOf(VectorF(0.99f,0.99f))))
        Main.mReport2!!.text = "(%.2f %.2f)".format(target.pos.x, target.pos.y)
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


    fun calibrateAll() {
//        view.setBackgroundColor(ContextCompat.getColor(Main.mContext!!, R.color.buttonfirstcolor))
        // There will be 11 calibration points (yes, that number is hardcoded, shut up!)
        // we start at the top of the pad. sendy will schedule subsequent points
        target.setPosV(VectorF(0.7f, 0f))
        // we cannot do the calibration of this point right now.
        // we have to wait for the stick to do its move. Sendy will do the timing
        // sendy will also trigger further calibration points
        // arg1 is the index of the first point to calibrate
        // arg2 is the direction: 1 for index from 0 to 10, -1 for index from 10 to 0
        sendy.send(PollMaster.EV_6_calibrateOne, 0, 1, null)
    }

    fun calibrateOne(index: Int, dir: Int) {
        // Index is the seq number of one of 11 points, range 0 .. 10
        // dir is +1 for going from 0 to 10, -1 for going from 10 to 0
        // a new target will put the stick on the move, which takes time
        //so, we fix the provisional correction for the previous position, which is stable now
        val deltaY = target.pos.y - stick.pos.y
        Forces.corTableProvisional[index - 1].y -= deltaY
        // now we can set the new target
        target.setPosV( VectorF(0.7f, index / 10f))
        sendy.send(PollMaster.EV_6_calibrateOne, index, dir, null)
    }

    fun calibrateEnd(index: Int) {
        val deltaY = target.pos.y - stick.pos.y
        Forces.corTableProvisional[index].y -= deltaY
        for (i in 0..10) {
            Forces.corTable[i].y = Forces.corTableProvisional[i].y * 1.3f
            Main.correctionView!!.setVertex(i, VectorF(0f, Forces.corTable[i].y + 0.5f))
            Main.correctionView!!.invalidate()
        }
    }



}