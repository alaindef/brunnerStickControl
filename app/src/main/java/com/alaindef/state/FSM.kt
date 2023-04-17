package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

/**
 * 160915 created by alaindef
 * to kotlin
 */
class FSM : Thread() {
    var fState: Int = 0
    var event: Int = 0
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

    inner class ZeHandler  /*
        A Handler allows you to send and process Message and Runnable objects associated with a
        thread's MessageQueue. Each Handler instance is associated with a single thread and that
        thread's message queue. When you create a new Handler it is bound to a Looper.
        It will deliver messages and runnables to that Looper's message queue and execute them
        on that Looper's thread.
        https://developer.android.com/reference/android/os/Handler
*/
        (looper: Looper?) : Handler(looper!!) {
        private var seq = 0
        private var steps = 0
        private val mbx = Main.mainMailbox

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---OSCAR---"
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj

            event = incomingMessage.what
            if (event >= MAX_EVENT) {
                Log.e(logTag, "EVENT unknown")
                return

            }
            val fOldState = fState
            fState = fsm_table[fState][event]
            Log.i(
                logTag,
                "incoming message: " + events[incomingMessage.what] + ", " + incomingMessage.arg1 + ", " + incomingMessage.arg2 + ", " + incomingMessage.obj
            )

            val repString = "   " + logTag + "\n${fstates[fOldState].trim()} + ${events[event]}+ ==> + ${fstates[fState].trim() }"
            Log.w(logTag, fstates[fOldState] + " + " + events[event] + " ==>   " + fstates[fState])
            Main.mReport!!.text = repString

            when (fState) {
                FST_0, FST_1, FST_2, FST_3  -> {}
                FST_4 -> {
                    val snaptime = System.nanoTime()
                    Handler().postDelayed({send(PollMaster2.ev_poll_and_repeat)},2000)
                    val elapsed = ((System.nanoTime() - snaptime) / 1000000).toInt()
                    mbx!!.send(MainMailbox.REPORT_ELAPSED_TIME, elapsed, 0, null)
                    send(EV_0)
                }
                FST_5 -> {
                    mbx!!.send(MainMailbox.SENDPACKET, 0, 0, " welwel")
                    send(EV_0)
                }
//                FST_5 -> {mbx!!.send(MainMailbox.REPORT_ELAPSED_TIME, 5, 0, arg3)
                else -> Log.wtf(logTag, "event unknown $event")
            }
        }
    }


    companion object {
        const val EV_0 = 0
        const val EV_1 = 1
        const val EV_2 = 2
        const val EV_3 = 3
        const val EV_4 = 4
        const val EV_5 = 5
//        const val EV_GO = 5
        private val events = arrayOf("event_0", "event_1", "event_2", "event_3", "event_4", "event_5")
        private val MAX_EVENT = events.size
        private const val FST_0 = 0
        private const val FST_1 = 1
        private const val FST_2 = 2
        private const val FST_3 = 3
        private const val FST_4 = 4
        private const val FST_5 = 5
        private val fstates = arrayOf(
            "0  FST_IDLE                            ",
            "1  FST_1                               ",
            "2  FST_2                               ",
            "3  FST_3                               ",
            "4  FST_4                               ",
            "5  FST_5                               "
        )

//@formatter:off
//        this controls the whole state machine
//               "0 ext", "1 slv", "2 clk", "3 mvd", "4 sv1",
//            "5 go ", "6 res", "7 shf", "8 sel", "9 shw"


        private val fsm_table: Array<IntArray> = arrayOf(
//                      0   1   2   3   4   5
//                    rst   1   2   3 lop pol
            intArrayOf( 0,  1,  2,  1,  1,  5), // 0  FST_IDLE"
            intArrayOf( 0,  1,  2,  2,  2,  5), // 1  FST_
            intArrayOf( 0,  1,  2,  3,  3,  5), // 2  FST_
            intArrayOf( 0,  0,  2,  3,  4,  5), // 3  FST_
            intArrayOf( 0,  0,  0,  0,  0,  0), // 4  FST_
            intArrayOf( 0,  0,  0,  0,  0,  0), // 5  FST_POLLING
        )
//@formatter:on
    }
}