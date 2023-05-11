package com.alaindef.brunner

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


    //    private val xTable = intArrayOf( -10, 5, 20, 30, 40, 50, 60, 70, 85, 100, 130)
//    private val yTable = intArrayOf( -40, -5, 16, 28, 40, 50, 60, 70, 80, 95, 130)
    private val xTable = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)
    val yTable = intArrayOf(0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 100)

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

    fun calibrate() {

        Main.delete1!!.drawTarget(170f, 70f)
        targetRel = Vector(0.7f, 0.1f)
//        Main.delete1!!.drawTarget(0.7f, 0.7f)
        Main.mReport2!!.text = "(${targetRel.x} ${targetRel.y})"

        currentRel = Vector(0.1f, 0.7f)
//        Main.delete1!!.drawTarget(target.x, target.y)
        sendy.send(PollMaster.EV_4_target_pos)

        Main.mReport5!!.text =
            "target ${(targetRel.y * 100).toInt()} current ${(currentRel.y * 100).toInt()} "
    }

    private fun correct(posRel: Vector): Vector {
//        range of pos: 0f .. 1f. scale up to 0 .. 100
        if (posRel.y > 0.99F) return Vector(posRel.x, 0.99f)    //avoid outofbounds further down
        if (posRel.x > 0.99F) return Vector(0.99f, posRel.y)
        val pos100 = posRel.times(100f)
//        xTable and yTable go from 0 to 100 in steps of 10, so an index for the array is:
        var index = pos100.divide(10f).toIntVector()
        val newX = if (index.x < 10)
            xTable[index.x] + (xTable[index.x + 1] - xTable[index.x]) * (pos100.x - index.x * 10f) / 10f
        else posRel.x
        val newY = if (index.y < 10)
            yTable[index.y] + (yTable[index.y + 1] - yTable[index.y]) * (pos100.y - index.y * 10f) / 10f
        else posRel.y
//        return values in range 0f .. 1f
        return Vector(newX, newY).divide(100f)
    }


    fun calculateForces(): Vector {
        var corrected = correct(targetRel)
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
        forces = Vector(0f, 0f)
    }
}