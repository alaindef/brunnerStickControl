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

    fun getResponse() = runBlocking<Unit> {

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val buffer = ByteArray(4096)
        var socketR: DatagramSocket? = null

        try {

            socketR = DatagramSocket(portR, InetAddress.getByName("0.0.0.0"))
            socketR.broadcast = true
            socketR.soTimeout = 2000
            Main.mReport5!!.text = "waiting ........................"

            val response =
                DatagramPacket(buffer, buffer.size)
//            Log.d("---OMER-", "connected? ${socketR!!.isConnected}")         dit moet false zijn (zie idea testcon project
//            Main.mReport5!!.text = "connected? ${socketR!!.isConnected}"
            socketR.receive(response)
            val quote = convertToInts(response.data, 9)
            val x = java.lang.Float.intBitsToFloat(quote[3])
            val y = java.lang.Float.intBitsToFloat(quote[1])
            Main.mReport5!!.text = "received ( $x $y )"
//                println("chat x y: $x  $y  from ${response.address}")


        } catch (ex: SocketTimeoutException) {
            println("Timeout error: " + ex.message)
//        ex.printStackTrace()
        } catch (ex: IOException) {
            println("Client error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
        socketR!!.close()
    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {
        private val mbx = Main.mainMailbox

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val logTag = ">---OMER---"
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj

            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    forceX = 0
                    forceY = 0
                    udpSender.sendMessage(forceX, forceY)
                    Main.mReport2!!.text = "$logTag\n  forceX = $forceX  forceY = $forceY"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    Main.mReport!!.text = ""
                    forceX = 0
                    forceY = 0
                    cnt = 0
                    delta_t = 100
                    udpSender.sendMessage(forceX, forceY)
                    Main.mReport2!!.text = "$logTag\n  forceX = $forceX  forceY = $forceY"
                    Main.mReport1!!.text = logTag + "\n" + cnt
                    Main.mReport3!!.text = "$logTag\ndt = $delta_t"
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
                        Main.mReport!!.text = "running ..."
                        Main.mReport1!!.text = logTag + "\n" + cnt++
                        udpSender.sendMessage(forceX, forceY)
                        Handler().postDelayed({ send(EV_3_next_round) }, delta_t.toLong())
                    }
                }
                EV_11_dt_min -> {
                    if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                    delta_t = max(delta_t, 10)
                    Main.mReport3!!.text = "$logTag\ndt = $delta_t"
                }
                EV_12_dt_plus -> {
                    if (delta_t < 100) delta_t += 10 else delta_t += 100
                    Main.mReport3!!.text = "$logTag\ndt = $delta_t"
                }
                EV_13_force_min -> {
                    forceX -= 100
                    Main.mReport2!!.text = "$logTag\nforceX = $forceX"
                }
                EV_14_force_plus -> {
                    forceX += 100
                    Main.mReport2!!.text = "$logTag\nforceX = $forceX"
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
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        private var running = false
    }
}