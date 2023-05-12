package com.alaindef.brunner

import java.lang.Float.min
import kotlin.math.max

object Forces {
    private val horizontalPID = BasicPID(1f, 0f, 0f)

    //    private val verticalPID = MiniPID(1f, 0f, 0f)
    private val verticalPID = BasicPID(1f, 0f, 0f)

    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f

    var targetRel = Vector(0f, 0f)
    var currentRel = Vector(0.05f, 0.15f)

    var forces = Vector(0f, 0f)

    val corTable = Array<Vector>(11) { Vector(0f, 0f) }
    val corTableProvisional = Array<Vector>(11) { Vector(0f, 0f) }

    init {

    }

    fun newPIDParam(value: Float, source: String) {
        when (source) {
            "conP" -> conP = value
            "conI" -> conI = value
            "conPv" -> conPv = value
            "conIv" -> conIv = value
        }
    }

    fun calibrateAll() {
        targetRel = Vector(0.7f, 0f)        //first calibration point. now wait for current to settle
        sendy.send(PollMaster.EV_4_startcalibration, 0, 0, null)

        print("cortable")
        for (i in 0..10) print(" <$i ${corTable[i].y}")
        println()
        Main.correctionView!!.invalidate()
    }

    fun calibrateOne(index: Int) {
        val delta_y = targetRel.y - currentRel.y   //when we are here, the stick has moved to previous target
        corTableProvisional[index-1].y -= delta_y
        println("Forces.calibrateOne: index=${index-1} T=$targetRel  C=$currentRel.y D=$delta_y")

        targetRel = Vector(0.7f, index / 10f)
        sendy.send(PollMaster.EV_4_startcalibration, index, 0, null)
    }

    fun calibrateEnd(index: Int){
        val delta_y = targetRel.y - currentRel.y
        corTableProvisional[index].y -= delta_y
        println()
        print("cortable AFTER:   ")
        for (i in 0..10){
            corTable[i].y = corTableProvisional[i].y * 1.3f
            print(" <$i ${corTable[i].y}>")
            Main.correctionView!!.setVertex(i, Vector(0f, corTable[i].y + 0.5f))
            Main.correctionView!!.invalidate()
        }
        println()
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