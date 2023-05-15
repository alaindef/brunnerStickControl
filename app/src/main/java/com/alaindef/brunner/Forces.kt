package com.alaindef.brunner

import android.content.Context
import androidx.core.content.ContextCompat
import java.lang.Float.min
import kotlin.math.max

object Forces {

    private val horizontalPID = BasicPID(1f, 0f, 0f)

    //    private val verticalPID = MiniPID(1f, 0f, 0f)
    private val verticalPID = BasicPID(1f, 0f, 0f)

    val stickPos = PositionRel("STICK",.2f, .05f, PositionRel.currentColor)
    val targetPos = PositionRel("TARGET",.6f, 0f, PositionRel.targetColor)
    // initial position not shown

    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f

    var targetRel = Vector(0f, 0f)
    var currentRel = Vector(0.05f, 0.15f)

    var forces = Vector(0f, 0f)

    val corTable = Array<Vector>(11) { Vector(0f, 0f) }
    val corTableProvisional = Array<Vector>(11) { Vector(0f, 0f) }

    fun newPIDParam(value: Float, source: String) {
        when (source) {
            "conP" -> conP = value
            "conI" -> conI = value
            "conPv" -> conPv = value
            "conIv" -> conIv = value
        }
    }

    fun calibrate(){
        var square = Square(0,0, 0,5)

        stickPos.setPositionRel(.8f, 0f)
        sendy.send(PollMaster.EV_5_calibratePos, square.l, square.u, square)

//        for (i in square.topLeft.x .. square.bottomRight.x) {
//            for (j in square.topLeft.y .. square.bottomRight.y){
//
////                println("i=$i  j=$j")
//            }
//        }
    }

    fun calibrateAll() {
//        view.setBackgroundColor(ContextCompat.getColor(Main.mContext!!, R.color.buttonfirstcolor))
        // There will be 11 calibration points (yes, that number is hardcoded, shut up!)
        // we start at the top of the pad. sendy will schedule subsequent points
        targetRel = Vector(0.7f, 0f)
        // we cannot do the calibration of this point right now.
        // we have to wait for the stick to do its move. Sendy will do the timing
        // sendy will also trigger further calibration points
        // arg1 is the index of the first point to calibrate
        // arg2 is the direction: 1 for index from 0 to 10, -1 for index from 10 to 0
        sendy.send(PollMaster.EV_4_calibrateOne, 0, 1, null)
    }

    fun calibrateOne(index: Int, dir: Int) {
        // Index is the seq number of one of 11 points, range 0 .. 10
        // dir is +1 for going from 0 to 10, -1 for going from 10 to 0
        // a new target will put the stick on the move, which takes time
        //so, we fix the provisional correction for the previous position, which is stable now
        val delta_y = targetRel.y - currentRel.y
        corTableProvisional[index - 1].y -= delta_y
        // now we can set the new target
        targetRel = Vector(0.7f, index / 10f)
        sendy.send(PollMaster.EV_4_calibrateOne, index, dir, null)
    }

    fun calibrateEnd(index: Int) {
        val delta_y = targetRel.y - currentRel.y
        corTableProvisional[index].y -= delta_y
        for (i in 0..10) {
            corTable[i].y = corTableProvisional[i].y * 1.3f
            Main.correctionView!!.setVertex(i, Vector(0f, corTable[i].y + 0.5f))
            Main.correctionView!!.invalidate()
        }
    }

    private fun correctRel(posRel: Vector): Vector {
//        range of pos: 0f .. 1f. scale up to 0 .. 100
        posRel.x = max(0f, kotlin.math.min(0.99f, posRel.x))    //avoid outofbounds further down
        posRel.y = max(0f, min(posRel.y, .99f))

        var index = posRel.divide(0.1f).toIntVector()

        val corX = if (index.x < 10)
            corTable[index.x].x + (corTable[index.x + 1].x - corTable[index.x].x) * (posRel.x - index.x / 10f) / 0.1f
        else 0f
        val corY = if (index.y < 10)
            corTable[index.y].y + (corTable[index.y + 1].y - corTable[index.y].y) * (posRel.y - index.y / 10f) / 0.1f
        else 0f

        return posRel.min(Vector(corX, corY))
    }


    fun calculateForces(): Vector {
        var corrected = correctRel(targetRel)
        horizontalPID.setP(100f * conP)
        horizontalPID.setI(conI * 2f)
//        horizontalPID.setI(0f)
        horizontalPID.setDirection(true)
        horizontalPID.setOutputLimits(4000f)
//        horizontalPID.setSetpoint(xTarget)

        forces.x = horizontalPID.getOutput(currentRel.x, corrected.x)

        verticalPID.setP(160f * conPv)
        verticalPID.setI(conIv * 5f)
//        verticalPID.setI(0f)
        verticalPID.setDirection(true)
        verticalPID.setOutputLimits(4000f)
//        verticalPID.setSetpoint(yTarget)

        forces.y = verticalPID.getOutput(currentRel.y, corrected.y)
//        forces.y = verticalPID.getOutput(currentRel.y, corrected.y)

        return forces
    }

    fun resetForces() {
        forces = Vector(0f, 0f)
    }
}