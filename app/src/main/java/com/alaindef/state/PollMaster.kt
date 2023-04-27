package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.Integer.max


/** 230417 created by alaindef */
class PollMaster : Thread() {

    var event: Int = 0
    var cnt = 0
    var delta_t = 100
    var forceX = 0
    var forceY = 0
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


    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---OMER---"
//            val arg1 = incomingMessage.arg1
//            val arg2 = incomingMessage.arg2
//            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    forceX = 0
                    forceY = 0
                    udpSender.sendUDP(forceX, forceY)
                    Main.mReport3!!.text = "$logTag\n  forceX = $forceX  forceY = $forceY"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    Main.mReport!!.text = ""
                    forceX = 0
                    forceY = 0
                    cnt = 0
                    delta_t = 100
                    udpSender.sendUDP(forceX, forceY)
                    Main.mReport3!!.text = "$logTag   forceX = $forceX  forceY = $forceY"
                    Main.mReport!!.text = "$logTag   $cnt "
                    Main.mReport5!!.text = "$logTag   delta_t"
                    Main.mReport5b!!.text = "$delta_t"
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
                        udpSender.sendUDP(forceX, forceY)
//                        Handler().postDelayed({ send(EV_3_next_round) }, delta_t.toLong())
                        Main.mReport3!!.text = "$cnt: forces ($forceX $forceY)"
                        oscar.send(RecMaster.EV_0)
                    }
                }
                EV_4_current_pos -> {
                    send(EV_3_next_round)
                }
                EV_11_dt_min -> {       // not used
                    if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                    delta_t = max(delta_t, 10)
                    Main.mReport5!!.text = "$logTag   delta_t"
                    Main.mReport5b!!.text = "$delta_t"
                }
                EV_12_dt_plus -> {       // not used
                    if (delta_t < 100) delta_t += 10 else delta_t += 100
                    Main.mReport5!!.text = "$logTag   delta_t"
                    Main.mReport5b!!.text = "$delta_t"
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
        const val EV_4_current_pos = 4
        const val EV_4_extra = 9
        const val EV_11_dt_min = 11

        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        private var running = false
    }
}