package com.alaindef.state

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class PolylineView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var tunWidth = 0

    // a list of 11 (0..10) correction input factors, values between 0 and 100
    private val vertices = mutableListOf(PointF(0f,0f))


    init {
        // Add a ViewTreeObserver to the view
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Get the width of the view and store it in a variable
                tunWidth = width
                for (i in 0 .. 10){
                    vertices.add(PointF(0f, ( i.toFloat()/10) * height))
                }
                vertices.removeAt(0)

                invalidate()

                // Remove the listener to avoid memory leaks
                viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }


    private val paint = Paint().apply {
        color = Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private var selectedVertexIndex = -1

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        tunWidth = width
        println("tun dan $tunWidth")

        // Draw the polyline
        val path = Path()
        path.moveTo(vertices[0].x, vertices[0].y)
        for (i in 1 until vertices.size) {
            path.lineTo(vertices[i].x, vertices[i].y)
        }
        canvas.drawPath(path, paint)

        // Draw the vertices
        vertices.forEachIndexed { index, vertex ->
            paint.color = if (index == selectedVertexIndex) Color.RED else Color.BLACK
            canvas.drawCircle(vertex.x, vertex.y, 10f, paint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Check if the touch event is on a vertex
                for (i in vertices.indices) {
                    val vertex = vertices[i]
                    if (sqrt((x - vertex.x).pow(2) + (y - vertex.y).pow(2)) < 50) {
                        selectedVertexIndex = i
//                        val yFix = vertices[i].y
                        invalidate()
                        break
                    }
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // Move the selected vertex
                if (selectedVertexIndex in vertices.indices) {
                    vertices[selectedVertexIndex].x = min(width.toFloat(), max(0f,x))

                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                // Deselect the selected vertex
                selectedVertexIndex = -1
                invalidate()
                sendy.yTable[selectedVertexIndex] = (selectedVertexIndex*10) + ((vertices[selectedVertexIndex].x*50/width)).toInt()
            }
        }

        return true
    }

    companion object
}
