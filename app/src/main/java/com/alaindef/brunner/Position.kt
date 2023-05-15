package com.alaindef.brunner

import android.graphics.Color

const val targetColor = Color.BLUE
const val currentColor = Color.MAGENTA

class PositionRel constructor (val id: String, var x: Float, var y: Float, val color: Int) {

    fun setPositionRel(xNew: Float, yNew: Float){
        x = xNew
        y = yNew
        Main.stickPad!!.showPos(xNew, yNew, color)
        println("$id set ---------: ($xNew  $yNew)  $color")
        Main.stickPad!!.invalidate()
    }

    companion object {
        const val targetColor = Color.BLUE
        const val currentColor = Color.MAGENTA
    }
}