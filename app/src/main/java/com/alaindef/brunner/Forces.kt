package com.alaindef.brunner

import com.alaindef.brunner.PadView
import java.lang.Float.min
import kotlin.math.max

object Forces {

    private val horizontalPID = BasicPID(1f, 0f, 0f)
    //    private val verticalPID = MiniPID(1f, 0f, 0f)
    private val verticalPID = BasicPID(1f, 0f, 0f)
    // initial position not shown

    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f

    var targetRel = VectorF(0f, 0f)
    var currentRel = VectorF(0.05f, 0.15f)

    var forces = VectorF(0f, 0f)

    val corTable = Array<VectorF>(11) { VectorF(0f, 0f) }
    val corTableProvisional = Array<VectorF>(11) { VectorF(0f, 0f) }

    fun newPIDParam(value: Float, source: String) {
        when (source) {
            "conP" -> {
                conP = value
                Main.conPReport!!.text = " ${value.toInt()}"
            }
            "conI" -> {
                conI = value
                Main.conIReport!!.text = " ${value.toInt()}"
            }
            "conPv" -> {
                conPv = value
                Main.conPvReport!!.text = " ${value.toInt()}"
            }
            "conIv" -> {
                conIv = value
                Main.conIvReport!!.text = " ${value.toInt()}"
            }
        }
    }

    private fun correctRel(posRel: VectorF): VectorF {
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

        return posRel.minus(VectorF(corX, corY))
    }

    private fun correctRel1(posRel: PositionRel): PositionRel {
//        range of pos: 0f .. 1f. scale up to 0 .. 100
        var posRelNew = posRel
        posRelNew.pos.x = max(0f, kotlin.math.min(0.99f, posRel.pos.x))    //avoid outofbounds further down
        posRelNew.pos.y = max(0f, min(posRel.pos.y, .99f))

        var index = (posRel.divide(0.1f)).toIntVector()

        posRelNew.pos.x = if (index.x < 10)
            corTable[index.x].x + (corTable[index.x + 1].x - corTable[index.x].x) * (posRelNew.pos.x - index.x / 10f) / 0.1f
        else 0f
        posRelNew.pos.y = if (index.y < 10)
            corTable[index.y].y + (corTable[index.y + 1].y - corTable[index.y].y) * (posRelNew.pos.y - index.y / 10f) / 0.1f
        else 0f

        return posRelNew
    }

    fun calculateForces(): VectorF {
        var corrected = correctRel(Main.stickPad!!.target.pos)
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
        forces = VectorF(0f, 0f)
    }
}