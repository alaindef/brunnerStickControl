package com.alaindef.brunner

import android.graphics.Color

const val targetColor = Color.BLUE
const val currentColor = Color.MAGENTA

class PositionRel constructor (val id: String, var x: Float, var y: Float, val color: Int) {

    init{
        println("init ======== > $id set ---------: ($x  $y)  $color targetcol= $targetColor stickcol=$currentColor")
    }
    fun setPositionRel(xNew: Float, yNew: Float){
        x = xNew
        y = yNew
        Main.stickPad!!.showPos(xNew, yNew, color)
        println("$id set ---------: ($xNew  $yNew)  $color")
        Main.stickPad!!.invalidate()
    }

    fun min(arg: PositionRel): PositionRel {
        val x1 = x - arg.x
        val y1 = y - arg.y
        return PositionRel(this.id, x1, y1, this.color)
    }

    fun times(arg: Float): PositionRel {
        return PositionRel(this.id, x * arg, y * arg, this.color)
    }

    fun divide(arg: Float): PositionRel {
        return PositionRel(this.id, x / arg, y / arg, this.color)
    }

    fun toIntVector(): IntVector {
        return IntVector(x.toInt(), y.toInt())
    }

    companion object {
        const val targetColor = Color.BLUE
        const val currentColor = Color.MAGENTA
    }
}