package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.Math.abs


/** 230417 created by alaindef */
class PollMaster : Thread() {

    var event: Int = 0
    var cnt = 0
    var delta_t = 100
    var xCurrent = 0f
    var yCurrent = 0f
    var currentPosReceived = false
    var xTarget = 0f
    var yTarget = 0f
    var targetPosReceived = false
    var forceX = 0
    var forceY = 0
    var alfa = 50

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

    fun calculateForcesSave() {
        forceX = alfa * (30 * abs(xTarget - xCurrent)).toInt()
        if (xTarget > xCurrent) forceX = -forceX
        forceY = alfa * min(170, abs(70 * (yTarget - yCurrent)).toInt())
        val sq = (1 + yCurrent) * (1 + yCurrent)
        forceY = (forceY * (1 + kotlin.math.abs(sq) / 10)).toInt()
        if (yTarget > yCurrent) forceY = -forceY
    }
    fun calculateForces() {
        forceX = alfa * (50 * abs(xTarget - xCurrent)).toInt()
        if (xTarget > xCurrent) forceX = -forceX
        forceY = alfa * min(250, abs(120 * (yTarget - yCurrent)).toInt())
        val sq = (1 + yCurrent) * (1 + yCurrent)
        forceY = (forceY * (1 + kotlin.math.abs(sq) / 10)).toInt()
        if (yTarget > yCurrent) forceY = -forceY
    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---Sendy---"
//            val arg1 = incomingMessage.arg1
//            val arg2 = incomingMessage.arg2
//            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    forceX = 0
                    forceY = 0
                    udpSender.sendUDP(forceX, forceY)
                    Main.mReport3!!.text = "forces ($forceX $forceY)"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    Main.mReport!!.text = ""
                    forceX = 0
                    forceY = 0
                    cnt = 0
                    delta_t = 100   //not used: after sending forces, receiver waits for input immediately, with timeout
                    udpSender.sendUDP(forceX, forceY)
                    Main.mReport3!!.text = "forces  ($forceX $forceY)"
                    Main.mReport!!.text = "$logTag   $cnt "
                    return
                }
                EV_2_start_stop -> {
                    if (running) {
                        running = false
                        Main.mReport!!.text = ""
                    } else {
                        running = true
                        send(EV_3_next_round)
                    }
                }
                EV_3_next_round -> {
                    if (running) {
                        cnt++
                        Main.mReport!!.text = "$cnt: running ..."
                        calculateForces()
                        udpSender.sendUDP(forceX, forceY)
//                        Handler().postDelayed({ send(EV_3_next_round) }, delta_t.toLong())
                        Main.mReport3!!.text = "forces ($forceX $forceY)"
                        recky.send(RecMaster.EV_0)
                        Main.mPad!!.invalidate()
                    }
                }
                EV_4_target_pos -> {
                    targetPosReceived = true
                }
                EV_5_current_pos -> {
                    currentPosReceived = true
                    send(EV_3_next_round)
                }
                EV_11_dt_min -> {       // not used
                    if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                    delta_t = max(delta_t, 10)
                }
                EV_12_dt_plus -> {       // not used
                    if (delta_t < 100) delta_t += 10 else delta_t += 100
                }
                EV_13_force_min -> {
                    forceX -= 100
                    Main.mReport3!!.text = "$logTag\nforceX = $forceX"
                }
                EV_14_force_plus -> {
                    forceX += 100
                    Main.mReport3!!.text = "$logTag\nforceX = $forceX"
                }
                else -> {
                    Log.e(logTag, "EVENT $event unknown")
                }
            }
        }
    }


    companion object {
        const val EV_0_reset = 0
        const val EV_1_full_reset = 1
        const val EV_2_start_stop = 2
        const val EV_3_next_round = 3
        const val EV_4_target_pos = 4
        const val EV_5_current_pos = 5
        const val EV_9_extra = 9
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        private var running = false
    }
}