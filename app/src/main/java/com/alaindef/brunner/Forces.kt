package com.alaindef.brunner
//230500 created ADF

import java.lang.Float.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

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

    var corrected = VectorF(0f, 0f)
    var correctedXY = VectorF(0f, 0f)

    var dim2 = false


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

        var index = posRel.divideBy(0.1f).toIntVector()

        val corX = if (index.x < 10)
            corTable[index.x].x + (corTable[index.x + 1].x - corTable[index.x].x) * (posRel.x - index.x / 10f) / 0.1f
        else 0f
        val corY = if (index.y < 10)
            corTable[index.y].y + (corTable[index.y + 1].y - corTable[index.y].y) * (posRel.y - index.y / 10f) / 0.1f
        else 0f

        return posRel.minus(VectorF(corX, corY))
    }

    private fun interpol(
        pos: VectorF, ref: Array<VectorF>,
    ): VectorF {
        val dist: FloatArray = floatArrayOf(0f, 1f, 2f, 3f)
        var weightsum = 0f
        var weightedPos = 0f
        for (i in 0..3) {
            dist[i] = sqrt((pos.x - ref[i].x).pow(2) + (pos.y - ref[i].y).pow(2))
            if (dist[i] < 0.01)
                return ref[i]
            weightsum += 1f / dist[i]
            weightedPos += 1
        }

        return VectorF(0f, 0f)
    }

    fun correct2Dim(pos: VectorF): VectorF {
//        range of pos: 0f .. 1f. scale up to 0 .. 100
        pos.x = max(0f, kotlin.math.min(0.99f, pos.x))    //avoid OutOfBounds further down
        pos.y = max(0f, min(pos.y, 0.99f))

        var index = pos.divideBy(0.1f).toIntVector()

        // refpos has the vector positions of the 4 corners of the surrounding square
        //index has range of 0..10, positions have a range 0f to 1f
        val refPos: Array<VectorF> = arrayOf(
            VectorF(index.x / 10f, index.y / 10f),
            VectorF((index.x + 1) / 10f, index.y / 10f),
            VectorF(index.x + 1 / 10f, (index.y + 1) / 10f),
            VectorF((index.x) / 10f, (index.y + 1) / 10f)
        )

        // ref has the corrections at the 4 corners of the surrounding square
        val refValue: Array<VectorF> = arrayOf(
            Main.stickPad!!.corrections[index.x][index.y],
            Main.stickPad!!.corrections[index.x + 1][index.y],
            Main.stickPad!!.corrections[index.x + 1][index.y + 1],
            Main.stickPad!!.corrections[index.x][index.y + 1]
        )

        // dist has the distances of the target position to the 4 corners of the surrounding square
        val dist: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)

        // teller and noemer are used to calculate the weighted correction :
        // correction = sum(w(i) * cor(i)) / sum(w(i))
        // remember that corrections are Vectors, not scalars
        var teller = VectorF(0f, 0f)
        var noemer = VectorF(0f, 0f)
        for (i in 0..3) {
            val deltaPos = pos.minus(refPos[i])
            dist[i] = sqrt((deltaPos.x / 10f).pow(2) + (deltaPos.y).pow(2))
            // if we are close to a corner, take the value of that corner, and avoid divide by zero
            if (dist[i] < 0.001) return pos.minus(refValue[i])

            teller = teller.add(refValue[i].divideBy(dist[i]))
            noemer = noemer.add(VectorF(1f, 1f).divideBy(dist[i]))
        }
        val cor = (teller.divideBy(noemer)).mul(2f)
        val res = pos.minus(cor)
//        println("pos= $pos   corPos= $res")

        return res
//        return (pos.minus(teller.divide(noemer)))
    }

    fun correct2DimXY(pos: VectorF): VectorF {
//        range of pos.x and pos.y: 0f .. 1f. scale up to 0 .. 100
        val x0 = max(0f, kotlin.math.min(0.99f, pos.x))    //avoid OutOfBounds further down
        val y0 = max(0f, min(pos.y, 0.99f))

        var index = pos.divideBy(0.1f).toIntVector()

        val x1 = index.x / 10f
        val x2 = (index.x + 1) / 10f
        val y1 = index.y / 10f
        val y2 = (index.y + 1) / 10f

        val z11 = Main.stickPad!!.corrections[index.x][index.y]
        val z21 = Main.stickPad!!.corrections[index.x + 1][index.y]
        val z12 = Main.stickPad!!.corrections[index.x][index.y + 1]
        val z22 = Main.stickPad!!.corrections[index.x + 1][index.y + 1]

        val x01 = x0 - x1
        val x21 = x2 - x1
        val y01 = y0 - y1
        val y21 = y2 - y1

        val d0x21y21 = (z11 mul (x21 * y21 - x01 * y21 - x21 * y01 + x01 * y01)) plus
                (z21 mul (x01 * y21 - x01 * y01)) plus
                (z12 mul (x21 * y01 - x01 * y01)) plus
                (z22 mul (x01 * y01))

        val res = pos minus (d0x21y21 divideBy (x21*y21*100f))

        return res
    }

    fun calculateForces(): VectorF {
        val pos = (Main.stickPad!!.target.pos)
        val correctedSQ = correct2Dim(pos)
//        val correctedXY = correct2DimXY(Main.stickPad!!.target.pos)
//        println("------------------------------------- CORRECTIONS P=$pos SQ=$correctedSQ   XY=$correctedXY")
        corrected = if (dim2) correctedSQ
        else correctRel(Main.stickPad!!.target.pos)

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