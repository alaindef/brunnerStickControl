package com.alaindef.brunner

/** 230417 created by alaindef */
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import java.lang.Integer.max
import java.net.InetAddress
import java.util.regex.Pattern


class PollMaster : Thread() {
    val logTag = ">---Sendy---"
    var calibrateButton: View? = null
    var calibrating = false
    var delay = 700                 // calibration will wait for this delay before calculating

    // targetpos - stickpos
    var step = 1                 // +1 for Down, -1 for Up

    //a square of 11x11 for calibrating 121 points. interpolation required during run
    val startPos = VectorI(0, 0)
    val endPos = VectorI(10, 10)
    val bounds = SquareI(startPos, endPos)

    //a square od 101x101 for calibrating all points. no interpolation required

    var rangeI = 100
    var rangeF = 100f
    val field = SquareI(VectorI(0, 0), VectorI(100, 100))

    val corrections: Array<Array<VectorF>> =
        Array(rangeI + 1) { Array(rangeI + 1) { VectorF(0f, 0f) } }
    val correctionsProvisional: Array<Array<VectorF>> =
        Array(rangeI + 1) { Array(rangeI + 1) { VectorF(0f, 0f) } }

    var event: Int = 0
    var cnt = 0
    var deltaT = 10


    private var ipAddress: InetAddress = InetAddress.getByName("192.168.0.203")

    private var mHandler: ZeHandler? = null

    override fun run() {
        mHandler = ZeHandler(Looper.getMainLooper())
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, arg1, arg2, obj)) //todo why 0 ?
    }

    fun send(what: Int) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, 0, 0, null))
    }


    private val PATTERN: Pattern = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
    )

    private fun validate(ip: String?): Boolean {
        return PATTERN.matcher(ip).matches()
    }

    fun whileNotPolling() {
        // now we can read the dialog box for changing the IP address of the brunner interface
        Main.mReport0!!.setBackgroundColor(-13388315)  // -13388315 is holo_blue_light (chat)
        Main.mReport0!!.text = ""
        if (Main.mIPDialog!!.getText() != null) {
            val address = Main.mIPDialog!!.getText().toString()
            if (validate(address)) {
                ipAddress = InetAddress.getByName(address)
                Main.mReport0!!.text = "Brunner ip $address"
            } else
                Main.mReport0!!.text = "INVALID ip address: $address"
        }
    }

    fun moveToTarget() {
        Forces.calculateForces()
        udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
        Main.mReport3!!.text =
            "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
        val res = UdpRecObject.getCoordinates()
        // return the result to sendy range of coordinates: 0f .. 1f
        Forces.currentRel = res            //range 0f .. 1f
        Main.stickPad!!.stick.setPosV(res)
//        Main.stickPad!!.stick.pos = res
//        Main.stickPad!!.invalidate()

    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {


        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    Forces.resetForces()
                    udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
                    Main.mReport3!!.text = "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
                    Main.mReport4!!.text = "$deltaT"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    whileNotPolling()
                    Forces.resetForces()
                    cnt = 0
                    deltaT = 5
                    udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
                    Main.mReport3!!.text = "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
                    Main.mReport0!!.text = "$logTag   $cnt "
                    Main.mReport4!!.text = "$deltaT"

                    return
                }
                EV_2_start_stop -> {
                    if (running) {
                        running = false
                        whileNotPolling()
                    } else {
                        running = true
                        send(EV_3_next_round)
                        Main.mReport4!!.text = "$deltaT"
                    }
                }
                EV_3_next_round -> {
                    if (running) {
                        cnt++
                        Main.mReport0!!.text = "$cnt: running ..."
                        Main.mReport0!!.setBackgroundColor(Color.RED)
                        moveToTarget()
                        Handler().postDelayed({ send(EV_3_next_round) }, deltaT.toLong())
                    }
                }
                EV_5_calibrate -> {
                    calibrateButton = arg3 as androidx.appcompat.widget.AppCompatTextView
                    calibrateButton!!.setBackgroundColor(
                        ContextCompat.getColor(
                            Main.mContext!!,
                            R.color.buttonsecondcolor
                        )
                    )
                    Main.stickPad!!.calibrateAll()
                }
                EV_6_calibrateOne -> {
                    if ((arg1 < 10)) {
                        Handler().postDelayed(
                            { Main.stickPad!!.calibrateOne(arg1 + arg2, arg2) }, delay.toLong()
                        )
                    } else {
                        Handler().postDelayed(
                            { Main.stickPad!!.calibrateEnd(arg1) },
                            delay.toLong()
                        )
                        calibrateButton!!.setBackgroundColor(
                            ContextCompat.getColor(Main.mContext!!, R.color.buttonfirstcolor)
                        )
                    }
                }
                EV_7_calibratePos -> {
                    var xIndex = 0
                    var yIndex = 0
                    rangeI = arg1
                    rangeF = arg1.toFloat()
                    delay = arg2

                    calibrateButton = arg3 as androidx.appcompat.widget.AppCompatTextView
                    calibrateButton!!.setBackgroundColor(
                        ContextCompat.getColor(Main.mContext!!, R.color.buttonsecondcolor)
                    )
                    println("------   i=$xIndex  j=$yIndex   ------")
                    Main.stickPad!!.target.setPos(xIndex / rangeF, yIndex / rangeF)

                    Handler().postDelayed(
                        { send(EV_8_deviation, xIndex, yIndex, null) }, delay.toLong()
                    )
                }
                EV_8_deviation -> {
                    //stick should have reached the target by now
                    val xIndex = arg1
                    val yIndex = arg2
                    val targetPos = Main.stickPad!!.target.pos
                    val stickPos = Main.stickPad!!.stick.pos
                    val delta = targetPos minus stickPos

//                    println("=======================> at ($xIndex $yIndex): tar=$targetPos    deviation= $delta")
                    correctionsProvisional[xIndex][yIndex] =
                        correctionsProvisional[xIndex][yIndex] minus delta
//                    if (yIndex == rangeI){
//                        for (i in 0..10) {          //!!!!
////                            Forces.corTable[i].y = Forces.corTableProvisional[i].y * 1.3f
//                            Main.correctionView!!.setVertex(i, VectorF(0f, Main.stickPad!!.correctionsProvisional[xIndex][i].y + 0.5f))
//                            Main.correctionView!!.invalidate()
//                        }
//                    }

                    if (((yIndex < rangeI) or (step < 0)) and ((yIndex > 0) or (step > 0))) {
                        Main.stickPad!!.target.setPos((xIndex) / rangeF, (yIndex + step) / rangeF)
                        Handler().postDelayed(
                            { send(EV_8_deviation, xIndex, yIndex + step, null) }, delay.toLong()
                        )
                    } else {
                        if (xIndex < rangeI) {
                            Main.stickPad!!.target.setPos((xIndex + 1) / rangeF, yIndex / rangeF)
                            step = -step                    // reverse y direction
                            Handler().postDelayed(
                                { send(EV_8_deviation, xIndex + 1, yIndex, null) }, delay.toLong()
                            )
                        } else {
                            calibrating = false
                            calibrateButton!!.setBackgroundColor(
                                ContextCompat.getColor(Main.mContext!!, R.color.buttonfirstcolor)
                            )
                            println("================================> end CALIB  ")
                            for (i in 0..rangeI) {
                                for (j in 0..rangeI) {
                                    corrections[i][j] = correctionsProvisional[i][j]
                                    // adf test
                                    for (k in 0 .. rangeI)
                                        for (l in 0 .. rangeI)
                                            corrections[k][l] = correctionsProvisional[k][l]
                                }
                            }
                        }
                    }
                }
                EV_30_force -> {
                    Main.stickPad!!.target.setPos(.8f, Main.stickPad!!.target.pos.y + 0.1f)
                    Forces.targetRel = Main.stickPad!!.target.pos
                    Main.stickPad!!.invalidate()
                }
                EV_15_new_IP -> if (!running) whileNotPolling()

                EV_11_dt_min -> {
                    if (deltaT <= 10) deltaT -= 1 else if (deltaT <= 100) deltaT -= 10 else deltaT -= 100
                    deltaT = max(deltaT, 1)
                    Main.mReport4!!.text = "$deltaT"
                }
                EV_12_dt_plus -> {
                    if (deltaT < 10) deltaT += 1 else if (deltaT < 100) deltaT += 10 else deltaT += 100
                    Main.mReport4!!.text = "$deltaT"
                }
                EV_13_force_min -> {
                    Forces.forces.x -= 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${Forces.forces.x.toInt()}"
                }
                EV_14_force_plus -> {
                    Forces.forces.x += 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${Forces.forces.x.toInt()}"
                }
                EV_21_from_slider -> {
                    Forces.newPIDParam(arg1.toFloat(), arg3.toString())
//                    println("sendy receives pos $arg1 from $arg3")
                }
                EV_22_resetCorY -> {
                    Main.correctionView!!.resetCorY()
                }
                else -> {
                    Log.e(logTag, "EVENT $event unknown")
                    Main.mReport5a!!.text = "sendy: incoming EVENT unknown"
                    Main.mReport5!!.text = "$event"
                }
            }
        }
    }


    companion object {
        const val EV_0_reset = 0
        const val EV_1_full_reset = 1
        const val EV_2_start_stop = 2
        const val EV_3_next_round = 3
        const val EV_5_calibrate = 5
        const val EV_6_calibrateOne = 6
        const val EV_7_calibratePos = 7
        const val EV_8_deviation = 8
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        const val EV_15_new_IP = 15
        const val EV_21_from_slider = 21
        const val EV_22_resetCorY = 22
        const val EV_30_force = 30
        private var running = false

    }
}