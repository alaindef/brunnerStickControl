package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log

/**
 * 160915 created by alaindef
 * to kotlin
 */
class PollMaster2 : Thread() {
    var event: Int = 0
    var cnt = 0
    private var mHandler: ZeHandler? = null

    //    public Handler mHandler;    //both work
    override fun run() {mHandler = ZeHandler(Looper.getMainLooper())}

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
            Log.i(
                logTag,
                "incoming message: " + events[incomingMessage.what] + ", " + incomingMessage.arg1 + ", " + incomingMessage.arg2 + ", " + incomingMessage.obj
            )
//            Log.w(logTag, events[event] + " ==>   " )
            Main.mReport1!!.text = logTag + "\n" + cnt++

            when (event) {
                ev_poll_and_repeat -> {
//                    Thread.sleep(2000)            //adf 230416 not good. timing of other processes jeapardized
                    Handler().postDelayed({send(ev_poll_and_repeat)},100)
                }
                EV_5 -> {
                    mbx!!.send(MainMailbox.SENDPACKET, 0, 0, " welwel")
                    send(ev_poll_and_repeat)
                }
                else -> Log.wtf(logTag, "event unknown $event")
            }
        }
    }


    companion object {
        const val ev_poll_and_repeat = 0
        const val EV_1 = 1
        const val EV_2 = 2
        const val EV_3 = 3
        const val EV_4 = 4
        const val EV_5 = 5

        //        const val EV_GO = 5
        private val events =
            arrayOf("event_0", "event_1", "event_2", "event_3", "event_4", "event_5")
        private val MAX_EVENT = events.size
        private const val FST_0 = 0
        private const val FST_1 = 1
        private const val FST_2 = 2
        private const val FST_3 = 3
        private const val FST_4 = 4
        private const val FST_5 = 5

//@formatter:off
//@formatter:on
    }
}