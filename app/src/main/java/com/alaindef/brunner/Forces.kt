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
    fun newPIDParam(value: Float, source: String) {
        when (source) {
            "conP" -> {
                conP = value; Main.conPReport!!.text = " ${value.toInt()}"
            }
            "conI" -> {
                conI = value; Main.conIReport!!.text = " ${value.toInt()}"
            }
            "conPv" -> {
                conPv = value; Main.conPvReport!!.text = " ${value.toInt()}"
            }
            "conIv" -> {
                conIv = value; Main.conIvReport!!.text = " ${value.toInt()}"
            }
        }
    }

    var calType = "none"        //default
    var calibMax = 4
    var calibMaxF = calibMax.toFloat()
    var calibJump = 100/ calibMax
    var calibDelay = 1000

    //    val calibDelay = 16000 / calibMax
    var corrected = VectorF(0f, 0f)
    var correctedXY = VectorF(0f, 0f)


    // table to store corrections at the calibration points.
    // an array of 101x101, but only calibMax x calibMax will be used
    private val correctionsProvisional: Array<Array<VectorF>> =
        Array(101) { Array(101) { VectorF(0f, 0f) } }

    // table to store all the corrections in a field of 101x101
    val corrections: Array<Array<VectorF>> =
        Array(101) { Array(101) { VectorF(0f, 0f) } }

    fun resetCorrections() {
        for (i in 0..calibMax) {
            for (j in 0..calibMax) correctionsProvisional[i][j] = VectorF(0f, 0f)
        }
        for (i in 0..100) for (j in 0..100) corrections[i][j] = VectorF(0f, 0f)
    }

    private fun resetProvisionalCorrections() {
        for (i in 0..calibMax) {
            for (j in 0..calibMax) correctionsProvisional[i][j] = VectorF(0f, 0f)
        }
    }

    //  ----------------------------------- calibration for full and interpol  --------------------
    fun updateCorrections(calibX: Int, calibY: Int, delta: VectorF) {
        correctionsProvisional[calibX][calibY] = correctionsProvisional[calibX][calibY] plus delta
    }

    fun fixCorrections(cnt: Float) {
        for (i in 0 until calibMax) {
            for (j in 0 until calibMax) {
                for (incX in 0 until calibJump)
                    for (incY in 0 until calibJump)
                        corrections[i * calibJump + incX][j * calibJump + incY] =
                            (corrections[i * calibJump + incX][j * calibJump + incY] plus
                                    correctionsProvisional[i][j]) divideBy cnt
            }
        }
        resetProvisionalCorrections()
    }

    private fun interpol(pos: VectorF, ref: Array<VectorF>): VectorF {
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
//  ----------------------------------- calibration for interpol END --------------------

    //  ----------------------------------- runtime for interpol ---------------------------------
    fun correctInterpol(pos: VectorF): VectorF {
//        range of pos: 0f .. 1f.
        pos.x = max(0f, kotlin.math.min(.99f, pos.x))    //avoid OutOfBounds further down
        pos.y = max(0f, min(pos.y, .99f))
//        index : 0 .. 100, in steps of calibJump
//        index is the coord of the topleft corner of the surrounding square
        val index = ((pos mul 100f) divideBy calibJump.toFloat()).toIntVector() mul calibJump

        // refpos has the vector positions of the 4 corners of the surrounding square
        //index has range of 0..10, positions have a range 0f to 1f

        val refPos: Array<VectorI> = arrayOf(
            index,
            index plus VectorI(1, 0),
            index plus VectorI(1, 1),
            index plus VectorI(0, 1)
        )

        // ref has the corrections at the 4 corners of the surrounding square
        if (index.x > 99) println("index.x = ${index.x}         > 99")
        if (index.y > 99) println("index.y > 99")

        val refValue: Array<VectorF> = arrayOf(
            corrections[index.x][index.y],
            corrections[index.x + 1][index.y],
            corrections[index.x + 1][index.y + 1],
            corrections[index.x][index.y + 1]
        )

        // dist has the distances of the target position to the 4 corners of the surrounding square
        val dist: FloatArray = floatArrayOf(0f, 0f, 0f, 0f)

        // teller and noemer are used to calculate the weighted correction :
        // correction = sum(w(i) * cor(i)) / sum(w(i))
        // remember that corrections are Vectors, not scalars
        var teller = VectorF(0f, 0f)
        var noemer = VectorF(0f, 0f)
        for (i in 0..3) {
            val deltaPos = pos minus refPos[i]
            dist[i] = sqrt((deltaPos.x).pow(2) + (deltaPos.y).pow(2))
            // if we are close to a corner, take the value of that corner, and avoid divide by zero
            if (dist[i] < 0.001) return pos minus refValue[i]
            teller = teller.add(refValue[i] divideBy dist[i])
            noemer = noemer.add(VectorF(1f, 1f) divideBy dist[i])
        }
        val cor = (teller.divideBy(noemer)).mul(1f)             //!!!! arbitrary 2f

//        println("------------------------correct2Dim $cor")
        val res = pos plus cor
//        println("pos= $pos   corPos= $res")
        return res
    }

    fun correct2DimXY(pos: VectorF): VectorF {
//        range of pos.x and pos.y: 0f .. 1f. scale up to 0 .. 100
        val x0 = max(0f, kotlin.math.min(0.99f, pos.x))    //avoid OutOfBounds further down
        val y0 = max(0f, min(pos.y, 0.99f))

        val index = pos.divideBy(0.1f).toIntVector()

        val x1 = index.x / calibMaxF
        val x2 = (index.x + 1) / calibMaxF
        val y1 = index.y / calibMaxF
        val y2 = (index.y + 1) / calibMaxF

        val z11 = corrections[index.x][index.y]
        val z21 = corrections[index.x + 1][index.y]
        val z12 = corrections[index.x][index.y + 1]
        val z22 = corrections[index.x + 1][index.y + 1]

        val x01 = x0 - x1
        val x21 = x2 - x1
        val y01 = y0 - y1
        val y21 = y2 - y1

        val d0x21y21 = (z11 mul (x21 * y21 - x01 * y21 - x21 * y01 + x01 * y01)) plus
                (z21 mul (x01 * y21 - x01 * y01)) plus
                (z12 mul (x21 * y01 - x01 * y01)) plus
                (z22 mul (x01 * y01))

        val res = pos minus (d0x21y21 divideBy (x21 * y21 * calibMaxF * calibMaxF))

        return res
    }

    var currentRel = VectorF(0.05f, 0.15f)

    var forces = VectorF(0f, 0f)
    fun calculateForces(): VectorF {
        // called at EV_3 at each round
        val pos = (Main.stickPad!!.target.pos)
        when (calType) {
            "interpol" -> corrected = correctInterpol(pos)
            else -> corrected = pos
        }

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

        return forces
    }

    fun resetForces() {
        forces = VectorF(0f, 0f)
    }
}