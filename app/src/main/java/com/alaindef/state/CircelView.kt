package com.alaindef.state

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CircleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            paint.color = Color.RED
            paint.style = Paint.Style.FILL
            val radius = 100f
            val x = width / 2f
            val y = height / 2f
            canvas.drawCircle(x, y, radius, paint)
        }
    }
}
