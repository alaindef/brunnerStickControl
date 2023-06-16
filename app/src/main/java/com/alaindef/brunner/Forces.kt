package com.alaindef.brunner
//230500 created ADF

import java.lang.Float.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

object Forces {

    private val horizontalPID = BasicPID(1f, 0f, 0f)
    private val verticalPID = BasicPID(1f, 0f, 0f)

    // initial position not shown
    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f
    var specialForce = 0f       //used while not polling: set by slider "F",
    // activated by button "forceReport" (next to slider)
    fun newParam(value: Float, source: String) {
        when (source) {
            "conP" -> {
                conP = value; " ${value.toInt()}".also { Main.conPReport!!.text = it }
            }
            "conI" -> {
                conI = value; " ${value.toInt()}".also { Main.conIReport!!.text = it }
            }
            "conPv" -> {
                conPv = value; " ${value.toInt()}".also { Main.conPvReport!!.text = it }
            }
            "conIv" -> {
                conIv = value; " ${value.toInt()}".also { Main.conIvReport!!.text = it }
            }
            "force" -> {
                Main.stickPad!!.multiplier = VectorF(1 + value*100f, 1+value*200f)
                " ${value.toInt()}".also { Main.forceReport!!.text = it }
            }
        }
    }

    var calType = "none"        //default
    var calibMax = 5
    var calibMaxF = calibMax.toFloat()
    var calibJump = 100 / calibMax                       // changed by button
    var calibDelay = 1000
    var corrected = VectorF(0f, 0f)                // range 0 .. 1f
    var correctedXY = VectorF(0f, 0f)

    // table to store corrections at the calibration points.
    // an array of 101x101, but only calibMax x calibMax will be used
    private val correctionsProvisional: Array<Array<VectorF>> =
        Array(101) { Array(101) { VectorF(0f, 0f) } }

    // table to store all the corrections in a field of 101x101
    val corrections: Array<Array<VectorF>> =
        Array(101) { Array(101) { VectorF(0f, 0f) } }

    fun resetCorrectionsProvisional() {
        for (i in 0..calibMax)
            for (j in 0..calibMax) correctionsProvisional[i][j] = VectorF(0f, 0f)
    }

    fun resetCorrections() {
        resetCorrectionsProvisional()
        for (i in 0..100) for (j in 0..100) corrections[i][j] = VectorF(0f, 0f)
    }

    //  ----------------------------------- calibration for full and interpol  --------------------
    fun updateCorrectionsProvisional(calibX: Int, calibY: Int, delta: VectorF) {
        correctionsProvisional[calibX][calibY] = correctionsProvisional[calibX][calibY] plus delta
    }

    fun fixCorrections(cnt: Float) {
        for (i in 0..calibMax) {
            for (j in 0..calibMax) {
                corrections[i][j] = (correctionsProvisional[i][j]) divideBy cnt
//                corrections[i][j] = (corrections[i][j] plus (correctionsProvisional[i][j]) divideBy cnt)
            }
        }
//        resetCorrectionsProvisional()
    }

    //  ----------------------------------- runtime for interpol ---------------------------------
    fun correctInterpol(pos: VectorF): VectorF {
//        range of pos: 0f .. 1f.
        pos.x = max(0f, kotlin.math.min(1f, pos.x))    //avoid OutOfBounds further down
        pos.y = max(0f, min(pos.y, 1f))
//        index : 0 .. 100, in steps of calibJump
//        index is the coord of the topleft corner of the surrounding square
        val calibIndex = ((pos mul 100f) divideBy calibJump.toFloat()).toIntVector()
        val index = calibIndex mul calibJump

        // refpos has the vector positions of the 4 corners of the surrounding square
        // index has range of 0..10, positions have a range 0f to 1f

        val refPos: Array<VectorI> = arrayOf(
            index,
            (index plus VectorI(calibJump, 0)).minV(VectorI(100, 100)),
            (index plus VectorI(calibJump, calibJump)).minV(VectorI(100, 100)),
            (index plus VectorI(0, calibJump)).minV(VectorI(100, 100))
        )

        val refValue: Array<VectorF> = arrayOf(
            corrections[calibIndex.x][calibIndex.y],
            corrections[minOf(calibIndex.x + 1, 100)][calibIndex.y],
            corrections[minOf(calibIndex.x + 1, 100)][minOf(calibIndex.y + 1, 100)],
            corrections[calibIndex.x][minOf(calibIndex.y + 1, 100)]
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
        return pos plus cor
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

    var currentRel = VectorF(0.5f, 0.5f)

    var forces = VectorF(0f, 0f)
    fun calculateForces(): VectorF {
        // called at EV_3 at each round
        val pos = (Main.stickPad!!.target.pos)
        corrected = when (calType) {
            "interpol" -> correctInterpol(pos)
            else -> pos
        }

        horizontalPID.setP(100f * conP)
        horizontalPID.setI(conI * 2f)
        horizontalPID.setDirection(true)
        horizontalPID.setOutputLimits(4000f)

        forces.x = horizontalPID.getOutput(currentRel.x, corrected.x)

        verticalPID.setP(160f * conPv)
        verticalPID.setI(conIv * 5f)
        verticalPID.setDirection(true)
        verticalPID.setOutputLimits(4000f)

        forces.y = verticalPID.getOutput(currentRel.y, corrected.y)

        return forces
    }

    fun resetForces() {
        forces = VectorF(0f, 0f)
    }
}