package com.alaindef.brunner

/** 230417 created by alaindef */
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log


class RecMaster : Thread() {

    var event: Int = 0
    var cnt = 0
    private var mHandler: ZeHandler? = null

    override fun run() {
        mHandler = ZeHandler(Looper.getMainLooper())
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, arg1, arg2, obj))
    }

    fun send(what: Int) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, 0, 0, null))
    }


    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---Recky---"

            cnt++
            event = incomingMessage.what
            val arg1 = incomingMessage.arg1
            when (event) {
                EV_0 -> {
                    val res = UdpRecObject.getCoordinates(arg1)
                    // return the result to sendy range of coordinates: 0f .. 1f
                    sendy.send(PollMaster.EV_6_current_pos, 0, 0, res)
                }
                else -> {
                    Log.e(logTag, "$cnt: EVENT $event unknown")
                }
            }
        }
    }

    companion object {
        const val EV_0 = 0
    }
}