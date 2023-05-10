package com.alaindef.brunner

import android.graphics.fonts.FontVariationAxis

object Forces {
    private val horizontalPID = BasicPID(1f, 0f, 0f)

    //    private val verticalPID = MiniPID(1f, 0f, 0f)
    private val verticalPID = BasicPID(1f, 0f, 0f)

    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f

    var forceX = 0f
    var forceY = 0f

    //    private val xTable = intArrayOf( -10, 5, 20, 30, 40, 50, 60, 70, 85, 100, 130)
//    private val yTable = intArrayOf( -40, -5, 16, 28, 40, 50, 60, 70, 80, 95, 130)
    private val xTable = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
    private val yTable = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

    init {}

    fun newPIDParam(value: Float, source: String) {
        when (source) {
            "conP" -> conP = value
            "conI" -> conI = value
            "conPv" -> conPv = value
            "conIv" -> conIv = value
        }
    }

    private fun correct(pos: Vector): Vector {
//        range of pos: 0f .. 1f. scale up to 0 .. 100
        val pos100 = pos.times(100f)
//        xTable and yTable go from 0 to 100 in steps of 10, so an index for the array is:
        val index = pos100.divide(10f).toIntVector()
        val newX =
            xTable[index.x] + (xTable[index.x + 1] - xTable[index.x]) * (pos100.x - index.x * 10f) / 10f
        val newY =
            yTable[index.y] + (yTable[index.y + 1] - yTable[index.y]) * (pos100.y - index.y * 10f) / 10f
//        return values in range 0f .. 1f
        return Vector(newX, newY).divide(100f)
    }

    fun calculateForces(current: Vector, target: Vector): Vector {
        val corrected = correct(target)
        horizontalPID.setP(100f * conP)
        horizontalPID.setI(conI * 2f)
//        horizontalPID.setI(0f)
        horizontalPID.setDirection(true)
        horizontalPID.setOutputLimits(4000f)
//        horizontalPID.setSetpoint(xTarget)

        forceX = horizontalPID.getOutput(current.x, corrected.x)

        verticalPID.setP(160f * conPv)
        verticalPID.setI(conIv * 5f)
//        verticalPID.setI(0f)
        verticalPID.setDirection(true)
        verticalPID.setOutputLimits(4000f)
//        verticalPID.setSetpoint(yTarget)

        forceY = verticalPID.getOutput(current.y, corrected.y)

        return Vector(forceX, forceY)
    }
}