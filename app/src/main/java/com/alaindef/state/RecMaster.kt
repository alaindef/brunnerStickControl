package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.StrictMode
import android.util.Log
import android.view.View
import android.widget.ImageView
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

import com.alaindef.state.PadView


/** 230417 created by alaindef */
class RecMaster : Thread() {

    var event: Int = 0
    var cnt = 0
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
            val logTag = ">---Recky---"
//            val arg1 = incomingMessage.arg1
//            val arg2 = incomingMessage.arg2
//            val arg3 = incomingMessage.obj

            cnt++
            event = incomingMessage.what
            when (event) {
                EV_0 -> {
                    UdpRecObject.getCoordinates()
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