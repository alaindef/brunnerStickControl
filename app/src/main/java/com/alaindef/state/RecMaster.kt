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

    private fun convertToInts(bytes: ByteArray, nbrOfInts: Int): IntArray {
        val byteBuffer = ByteBuffer.allocate(nbrOfInts * 4)
        val intBuffer = byteBuffer.asIntBuffer()
        val result = IntArray(nbrOfInts)

        for (i in 0 until nbrOfInts) {
            byteBuffer.put(bytes[4 * i + 3])
            byteBuffer.put(bytes[4 * i + 2])
            byteBuffer.put(bytes[4 * i + 1])
            byteBuffer.put(bytes[4 * i + 0])
        }
        for (i in 0 until nbrOfInts) {
            result[i] = intBuffer.get()
        }
        return result
    }

    var socketR: DatagramSocket? = null


    data class Vector(val x: Float, val y: Float)

    fun getCoordinates(): Vector {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val buffer = ByteArray(4096)
        var x = 0f
        var y = 0f

        try {
//            Main.mReport5b!!.text = "$cnt: waiting for incomming UDP"
            val response = DatagramPacket(buffer, buffer.size)
            socketR!!.receive(response)

            val quote = convertToInts(response.data, 9)
            x = java.lang.Float.intBitsToFloat(quote[3])
            y = java.lang.Float.intBitsToFloat(quote[1])
//            Main.mPad!!.
            Main.mReport1!!.text = "CURRENT: (${String.format("%.${2}f", x)}  ${String.format("%.${2}f", y)})"
            sendy.xCurrent = x
            sendy.yCurrent = y
            sendy.send(PollMaster.EV_5_current_pos)



        } catch (ex: SocketTimeoutException) {
            println("$cnt: Timeout error: " + ex.message)
        } catch (ex: IOException) {
            println("$cnt: Client error: " + ex.message)
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
        return Vector(x, y)
    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        init {
            socketR = DatagramSocket(portR, InetAddress.getByName("0.0.0.0"))
            socketR!!.broadcast = true
            socketR!!.soTimeout = 4000
        }

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
                    getCoordinates()
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