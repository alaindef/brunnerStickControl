package com.alaindef.brunner

import android.graphics.Color

class PositionRel constructor (val id: String, var pos: VectorF, val color: Int) {

    init{
        println("init ======== > $id set ---------: ($pos.x  $pos.y)  $color targetcol= $targetColor stickcol=$stickColor")
    }

    fun setPos(xNew: Float, yNew: Float){
        pos.x = xNew
        pos.y = yNew
        Main.stickPad!!.invalidate()
    }
   fun setPosV(new:VectorF){
        pos = new
        Main.stickPad!!.invalidate()
    }

    fun minus(arg: PositionRel): PositionRel {
        val x1 = pos.x - arg.pos.x
        val y1 = pos.y - arg.pos.y
        return PositionRel(this.id, VectorF(x1, y1), this.color)
    }

    fun times(arg: Float): PositionRel {
        return PositionRel(this.id, VectorF(pos.x * arg, pos.y * arg), this.color)
    }

    fun divide(arg: Float): PositionRel {
        return PositionRel(this.id, VectorF(pos.x / arg, pos.y / arg), this.color)
    }

    fun toIntVector(): VectorI {
        return VectorI(pos.x.toInt(), pos.y.toInt())
    }

    companion object {
        const val targetColor = Color.BLUE
        const val stickColor = Color.MAGENTA
    }
}