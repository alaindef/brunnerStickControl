package com.alaindef.brunner

/** 230417 created by alaindef */

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class CircleView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    // Store the x and y coordinates
    private var xCoord = 0f
    private var yCoord = 0f

    // Override onDraw to draw the circle
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        // Set the paint color and style
        val paint = Paint()
//        paint.color = Color.RED
        paint.style = Paint.Style.FILL

        canvas?.drawCircle(xCoord, yCoord, 20f, paint)
    }

    // Public method to set the x and y coordinates
    fun setCoordinates(x: Float, y: Float) {
        xCoord = x
        yCoord = y
        invalidate()
        invalidate()
    }
}
