package com.alaindef.puzzle

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
            val event = incomingMessage.what
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
            Log.w(logTag, fstates[fOldState] + " + " + events[event] + " ==>   " + fstates[fState])
            when (fState) {
                FST_IDLE, FST_WAIT_FOR_MOV_DONE_AND_RESET, FST_WAIT_FOR_MOV_DONE_AND_SHUFFLE, FST_WAIT_FOR_COMPLETION, FST_WAIT_FOR_MOVE_DONE -> {}
                FST_SOLVE -> {
                    val snaptime = System.nanoTime()
                    sleep(1000)
                    val elapsed = ((System.nanoTime() - snaptime) / 1000000).toInt()
                    mbx!!.send(MainMailbox.REPORT_ELAPSED_TIME, elapsed, 0, null)
                    send(EV_EXTRA)
                }
                FST_CHECK_TAIL -> send(EV_EXTRA)
                FST_PLAY_NXT_MOVE -> {
                    send(EV_GO)
                }
                FST_RESET -> send(EV_GO)
                FST_SHUFFLE -> send(EV_GO)
                FST_DELAYED_REPORT_HINT -> send(EV_GO)
                FST_DISCARD -> send(EV_GO)
                FST_STUCK -> Log.e(logTag, "State machine stopped - ERROR 12")
                FST_SET_SIZE -> {
                    mbx!!.send(MainMailbox.SET_SIZE, 0,0, incomingMessage.obj
                    ) //m.obj is the extra button
                    send(EV_GO)
                }
                FST_MOVE_AFTER_TILE_CLICK -> {
                    mbx!!.send(MainMailbox.TILE_CLICK, 0, 0, incomingMessage.obj)
                    send(EV_GO)
                }
                else -> Log.wtf(logTag, "event unknown $event")
            }
        }
    }


    companion object {
        private const val EV_EXTRA = 0
        const val EV_SOLVE_REQ = 1
        const val EV_CLICK = 2
        const val EV_MOVE_DONE = 3
        const val EV_SOLVE_1 = 4
        private const val EV_GO = 5
        const val EV_RESET = 6
        const val EV_SHUFFLE = 7
        const val EV_SET_SIZE = 8
        private val events = arrayOf(
            "0 ext", "1 slv", "2 clk", "3 mvd", "4 sv1",
            "5 go ", "6 res", "7 shf", "8 sel", "9 shw"
        )
        private val MAX_EVENT = events.size
        private const val FST_IDLE = 0
        private const val FST_SOLVE = 1
        private const val FST_WAIT_FOR_MOVE_DONE = 2
        private const val FST_CHECK_TAIL = 3
        private const val FST_PLAY_NXT_MOVE = 4
        private const val FST_RESET = 5
        private const val FST_SHUFFLE = 6
        private const val FST_WAIT_FOR_MOV_DONE_AND_RESET = 7
        private const val FST_WAIT_FOR_MOV_DONE_AND_SHUFFLE = 8
        private const val FST_DELAYED_REPORT_HINT = 9
        private const val FST_WAIT_FOR_COMPLETION = 10
        private const val FST_DISCARD = 11
        private const val FST_STUCK = 12
        private const val FST_SET_SIZE = 13
        private const val FST_MOVE_AFTER_TILE_CLICK = 14
        private val fstates = arrayOf(
            "0  FST_IDLE                            ",
            "1  FST_SOLVE                           ",
            "2  FST_WAIT_FOR_MOVE_DONE              ",
            "3  FST_CHECK_TAIL                      ",
            "4  FST_PLAY_NXT_MOVE                   ",
            "5  FST_RESET                           ",
            "6  FST_SHUFFLE                         ",
            "7  FST_WAIT_FOR_MOV_DONE_AND_RESET     ",
            "8  FST_WAIT_FOR_MOV_DONE_AND_SHUFFLE   ",
            "9  FST_DELAYED_REPORT_HINT             ",
            "10 FST_WAIT_FOR_COMPLETION             ",
            "11 FST_DISCARD                         ",
            "12 FST_STUCK                           ",
            "13 FST_SET_SIZE                        ",
            "14 FST_MOVE_AFTER_TILE_CLICK           ",
            "15 FST_SHOW                            "
        )

//@formatter:off
//        this controls the whole state machine
//               "0 ext", "1 slv", "2 clk", "3 mvd", "4 sv1",
//            "5 go ", "6 res", "7 shf", "8 sel", "9 shw"


        private val fsm_table: Array<IntArray> = arrayOf(
//                      0   1   2   3   4   5   6   7   8   9
//                    ext slv clk mvd sv1  go res shf sel shw
            intArrayOf( 0,  1, 14,  0,  1,  0,  5,  6, 13), // 0  FST_IDLE"
            intArrayOf( 0, 11, 11, 11, 11,  2, 11, 11, 11), // 1  FST_SOLVE
            intArrayOf(11, 11, 11,  3, 11, 11,  7,  8, 11), // 2  FST_WAIT_FOR_MOVE_DONE
            intArrayOf( 9, 11, 11, 11, 11,  4, 11, 11, 11), // 3  FST_CHECK_TAIL
            intArrayOf(11, 11, 11, 11, 11,  2, 11, 11, 11), // 4  FST_PLAY_NXT_MOVE
            intArrayOf(11, 11, 11, 11, 11, 10, 11, 11, 11), // 5  FST_RESET
            intArrayOf(11, 11, 11, 11, 11, 10, 11, 11, 11), // 6  FST_SHUFFLE
            intArrayOf(11, 11, 11,  5, 11, 11, 11, 11, 11), // 7  FST_WAIT_FOR_MOV_DONE_AND_RESET
            intArrayOf(11, 11, 11,  6, 11, 11, 11, 11, 11), // 8  FST_WAIT_FOR_MOV_DONE_AND_SHUFFLE
            intArrayOf(11, 11, 11, 11, 11,  0, 11, 11, 11), // 9  FST_DELAYED_REPORT_HINT
            intArrayOf(11, 11, 11,  0, 11, 11, 11, 11, 11), // 10 FST_WAIT_FOR_COMPLETION
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0,  0), // 11 FST_DISCARD
            intArrayOf(12, 12, 12, 12, 12, 12,  0, 12,  0), // 12 FST_STUCK
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0,  0), // 13 FST_SET_SIZE
            intArrayOf(11, 11, 11, 11, 11, 10, 11, 11, 11)  // 14 FST_MOVE_AFTER_TILE_CLICK
        )
//@formatter:on
    }
}