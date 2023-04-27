package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.StrictMode
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.lang.Integer.max
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer


/** 230417 created by alaindef */
class RecMaster : Thread() {

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
    fun getResponse() = runBlocking<Unit> {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val buffer = ByteArray(4096)

        try {
            Main.mReport5b!!.text = "$cnt: waiting for incomming UDP"

            val response =
                DatagramPacket(buffer, buffer.size)
//            Log.d("---OMER-", "connected? ${socketR!!.isConnected}")         dit moet false zijn (zie idea testcon project
//            Main.mReport5!!.text = "connected? ${socketR!!.isConnected}"
            socketR!!.receive(response)

            val quote = convertToInts(response.data, 9)
            val x = java.lang.Float.intBitsToFloat(quote[3])
            val y = java.lang.Float.intBitsToFloat(quote[1])
            Main.mReport5!!.text = "$cnt: received ( $x $y )"
            Main.mReport5b!!.text = "-----------------------"


        } catch (ex: SocketTimeoutException) {
            println("$cnt: Timeout error: " + ex.message)
//        ex.printStackTrace()
        } catch (ex: IOException) {
            println("$cnt: Client error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
//        socketR!!.close()
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
            val logTag = ">---oscar---"
//            val arg1 = incomingMessage.arg1
//            val arg2 = incomingMessage.arg2
//            val arg3 = incomingMessage.obj

            cnt++
            event = incomingMessage.what
            when (event) {
                EV_0 -> {
                    getResponse()
                    omer.send(PollMaster.EV_4_response_ok)
                    Log.d(logTag, "$cnt: msg EV_0  reveived ")
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