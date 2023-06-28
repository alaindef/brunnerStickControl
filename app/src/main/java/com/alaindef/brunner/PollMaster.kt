package com.alaindef.brunner

/** 230417 created by alaindef */
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import java.lang.Integer.max


class PollMaster : Thread() {

    companion object {
        const val EV_0_reset = 0
        const val EV_1_full_reset = 1
        const val EV_2_start_stop = 2
        const val EV_3_next_round = 3
        const val EV_6_current_pos = 6
        const val EV_7_calibratePos = 7
        const val EV_8_stick_should_arrive = 8
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_15_new_IP = 15
        const val EV_21_from_slider = 21
        const val EV_22_resetCorY = 22
        const val EV_30_force = 30
        var running = false
        var cnt = 0
    }

    val logTag = ">---Sendy---"

    var calibrateButton: View? = null
    var calibrating = false
    var calibDelay =
        Forces.calibDelay            // calibration will wait for this delay before calculating

    // targetpos - stickpos
    var stepX = 1                 // +1 for right, -1 for left
    var stepY = 1                 // +1 for Down, -1 for Up

    var event: Int = 0
    var deltaT = 10

    private var mHandler: ZeHandler? = null

    override fun run() {
        mHandler = ZeHandler(Looper.getMainLooper())
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, arg1, arg2, obj)) //todo why 0 ?
    }

    fun resetFull() {
        running = false
        calibrating = false
        Forces.resetForces()
        cnt = 0
        deltaT = 5
        udpSender.sendUDP(Forces.forces.x, Forces.forces.y)
    }

    fun dtMin() {
        deltaT -= if (deltaT <= 10) 1 else if (deltaT <= 100) 10 else 100
        deltaT = max(deltaT, 1)
    }

    fun dtPlus() {
        deltaT += if (deltaT < 10) 1 else if (deltaT < 100) 10 else 100
    }

    fun send(what: Int) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, 0, 0, null))
    }

    fun moveToTarget() {
        val forces = Forces.calculateForces()
        udpSender.sendUDP(forces.x, forces.y)
        Main.stickPad!!.stick.setPosV(UdpRecObject.getCoordinates(cnt))
    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj
            Main.whilePolling(cnt, deltaT)
            event = incomingMessage.what
            when (event) {
                EV_2_start_stop -> {
                    if (running) running = false
                    else {
                        running = true
                        send(EV_3_next_round)
                    }
                }
                EV_3_next_round -> {
                    if (running) {
                        moveToTarget()
                        Handler(Looper.getMainLooper()).postDelayed(
                            { send(EV_3_next_round) }, deltaT.toLong()
                        )
                    }
                }
                EV_7_calibratePos -> {
                    // calibX goes from 0 to 100 in (calibMax+1) times
                    // e.g. for calibMax=10, there wil be 11 calibration points along each x-line
                    val calibX = 0
                    val calibY = 0

                    calibrating = true
                    calibrateButton = arg3 as androidx.appcompat.widget.AppCompatTextView
                    calibrateButton!!.setBackgroundColor(
                        ContextCompat.getColor(Main.mContext!!, R.color.buttonsecondcolor)
                    )
//                    calibX and calibY not yet needed. start with (0 0
                    Main.stickPad!!.target.setPos(
                        calibX * Forces.calibMax.toFloat() / 100f,
                        calibY * Forces.calibMax.toFloat() / 100f
                    )
                    Handler(Looper.getMainLooper()).postDelayed(
                        { send(EV_8_stick_should_arrive, calibX, calibY, null) },
                        calibDelay.toLong()
                    )
                    Forces.resetCorrectionsProvisional()
                }
                EV_8_stick_should_arrive -> {
                    //stick should have reached the target by now
                    val calibX = arg1
                    val calibY = arg2

                    val calibMax = Forces.calibMax
                    val calibMaxF = Forces.calibMaxF

                    val targetPos = Main.stickPad!!.target.pos
                    val stickPos = Main.stickPad!!.stick.pos

                    Forces.updateCorrectionsProvisional(calibX, calibY, targetPos minus stickPos)

                    if (!calibrating) return
                    if (((calibY < calibMax) or (stepY < 0)) and ((calibY > 0) or (stepY > 0))) {
                        //we are between y=0 and y=max
                        Main.stickPad!!.target.setPos(
                            calibX / calibMaxF,
                            (calibY + stepY) / calibMaxF
                        )
                        Handler(Looper.getMainLooper()).postDelayed(
                            { send(EV_8_stick_should_arrive, calibX, calibY + stepY, null) },
                            calibDelay.toLong()
                        )
                    } else {
                        if ((calibX < calibMax) or (stepX < 0) and ((calibX > 0) or (stepX > 0))) {
                            //we are at y=0 or at y=max, move to next column and reverse y direction
                            Main.stickPad!!.target.setPos(
                                (calibX + stepX) / calibMaxF,
                                calibY / calibMaxF
                            )
                            stepY = -stepY                    // reverse y direction
                            Handler(Looper.getMainLooper()).postDelayed(
                                { send(EV_8_stick_should_arrive, calibX + stepX, calibY, null) },
                                calibDelay.toLong()
                            )
                        } else {
                            //we are at x=0 or x=max
                            if (stepX > 0) {
                                //we are at x=max, so reverse stepX and move to column at left
                                stepX = -stepX
                                stepY = -stepY
                                Main.stickPad!!.target.setPos(
                                    (calibX) / calibMaxF,
                                    (calibY + stepY) / calibMaxF
                                )
                                Handler(Looper.getMainLooper()).postDelayed(
                                    {
                                        send(
                                            EV_8_stick_should_arrive,
                                            calibX,
                                            (calibY + stepY),
                                            null
                                        )
                                    },
                                    calibDelay.toLong()
                                )
                                Log.i(logTag, "===> REVERSE CALIB  ")
//                                Forces.fixCorrections(1f)
                            } else {
                                // we are back at x=0, we can stop now
                                calibrating = false
                                calibrateButton!!.setBackgroundColor(
                                    ContextCompat.getColor(
                                        Main.mContext!!,
                                        R.color.button_first_color
                                    )
                                )
                                stepX = 1; stepY = 1
                                Forces.fixCorrections(1.5f)
                                Log.i(logTag, "===> END CALIB  ")
                            }
                        }
                    }
                }
                EV_15_new_IP -> if (!running) Main.whileNotRunning()
                else -> {
                    Log.wtf(logTag, "EVENT $event unknown")
                }
            }
        }
    }


}