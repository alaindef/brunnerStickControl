package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.StrictMode
import android.util.Log
import android.widget.TextView
import java.lang.Integer.max


/** 230417 created by alaindef */
class PollMaster : Thread() {

    var fState: Int = 0
    var event: Int = 0
    var cnt = 0
    var delta_t = 100
    var forceX = 0
    private var mHandler: ZeHandler? = null

    //    public Handler mHandler;    //both work
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
        private var seq = 0
        private var steps = 0
        private val mbx = Main.mainMailbox

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---OMER---"
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj

            event = incomingMessage.what

            if (event >= MAX_EVENT) {
                Log.e(logTag, "EVENT unknown")
                return
            }

            when (event){
                EV_2_Ext -> {
                    forceX = (arg1-50) * 10
                    Main.mReport2!!.text = "$logTag\n from seekBar forceX = $forceX"
                }
                else -> {
                    val fOldState = fState
                    fState = fsm_table[fState][event]
//                    if (event != EV_0) {
                    if (event != EV_1_GO) {                 // avoid too many entries
                        val repString = "${fstates[fOldState].trim()} + (${events[event]} $arg1 $arg2 $arg3) ==> + ${fstates[fState]}"
                        Log.w(logTag, repString)
                    }
                    when (fState) {
                        FST_0, FST_3-> {}
                        FST_1 -> {
                            Main.mReport1!!.text = logTag + "\n" + cnt++
                            Handler().postDelayed({ send(EV_1_GO) }, delta_t.toLong())
                        }
                        FST_2 -> {
                            Main.mReport!!.text = "UDP packet sent with force = $forceX"
                            udpSender.sendMessage(forceX)
//                    send(EV_0)                            // one time only
                            send(EV_1_GO)
                        }
                        FST_4 -> {
                            if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                            delta_t = max(delta_t, 10)
                            Main.mReport2!!.text = "$logTag\ndt = $delta_t"
                            fState = fOldState
                        }
                        FST_5 -> {
                            if (delta_t <  100) delta_t += 10 else delta_t += 100
                            Main.mReport2!!.text = "$logTag\ndt = $delta_t"
                            fState = fOldState
                        }
                        FST_6 -> {
                            forceX -= 100
                            Main.mReport2!!.text = "$logTag\nforceX = $forceX"
                            fState = fOldState
                        }
                        FST_7 -> {
                            forceX += 100
                            Main.mReport2!!.text = "$logTag\nforceX = $forceX"
                            fState = fOldState
                        }
                        else -> Log.wtf(logTag, "state or event unknown $event")
                    }
                }
            }
        }
    }


    companion object {
        const val EV_0 = 0
        const val EV_1_GO = 1
        const val EV_2_Ext = 2
        const val EV_3_PR = 3
        const val EV_4 = 4
        const val EV_5 = 5
        const val EV_6 = 6
        const val EV_7 = 7

        private val events =
            arrayOf("ev_0", "ev_1", "ev_2", "ev_3", "ev_4", "ev_5", "ev_6", "ev_7")
        private val MAX_EVENT = events.size
        private const val FST_0 = 0
        private const val FST_1 = 1
        private const val FST_2 = 2
        private const val FST_3 = 3
        private const val FST_4 = 4
        private const val FST_5 = 5
        private const val FST_6 = 6
        private const val FST_7 = 7
        private val fstates = arrayOf(
            "0  FST_IDLE      ",
            "1  FST_1_delay  ",
            "2  FST_2_doStuff",
            "3  FST_3_",
            "4  FST_4_deltaT-",
            "5  FST_5_deltaT+",
            "6  FST_6_",
            "7  FST_7_"
        )

//@formatter:off

        private val fsm_table: Array<IntArray> = arrayOf(
//                      0   1   2   3   4   5   6   7
//                    rst  go ext  PR
            intArrayOf( 0,  0,  4,  1,  4,  5,  6,  7), // 0  FST_IDLE"
            intArrayOf( 0,  2,  0,  2,  4,  5,  6,  7), // 1  FST_delay
            intArrayOf( 0,  1,  0,  1,  4,  5,  6,  7), // 2  FST_dostuff
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7), // 3  FST_
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7), // 4  FST_deltaT-
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7), // 5  FST_deltaT+
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7), // 6  FST_forceX-
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7), // 7  FST_forceX+
        )
//@formatter:on
    }
}