package com.alaindef.state

import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.StrictMode
import android.util.Log
import android.widget.TextView
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

    var fState: Int = 0
    var event: Int = 0
    var cnt = 0
    var delta_t = 100
    var forceX = 0
    var forceY = 0
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

            if (event >= MAX_EVENT) {
                Log.e(logTag, "EVENT unknown")
                return
            }

            when (event) {
                EV_2_Ext -> {
                    getResponse()
                }
                else -> {
                    val fOldState = fState
                    fState = fsm_table[fState][event]
//                    if (event != EV_0) {
                    if (event != EV_1_GO) {                 // avoid too many entries
                        val repString =
                            "${fstates[fOldState].trim()} + (${events[event]} $arg1 $arg2 $arg3) ==> + ${fstates[fState]}"
                        Log.w(logTag, repString)
                    }
                    when (fState) {
                        FST_0 -> {}
                        FST_1 -> {
                            Main.mReport1!!.text = logTag + "\n" + cnt++
                            Handler().postDelayed({ send(EV_1_GO) }, delta_t.toLong())
                        }
                        FST_2 -> {
                            Main.mReport4!!.text = "UDP packet sent force = ($forceX $forceY)"
                            udpSender.sendMessage(forceX, forceY)
//                            oscar.send(PollMaster.EV_7)
//                            getResponse()
//                            udpReceiver.get()
//                            mbx!!.send(MainMailbox.RECEIVE)
//                    send(EV_0)                            // one time only
                            send(EV_1_GO)
                        }
                        FST_3 -> {
//                            Log.i(logTag, "trying ???????????")
                            oscar.send(PollMaster.EV_2_Ext,0,0,null)
//                            getResponse()
//                            udpReceiver.get()
//                            mbx!!.send(MainMailbox.RECEIVE)
//                            Runnable { }
                            send(EV_1_GO)
                        }
                        FST_4 -> {
                            if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                            delta_t = max(delta_t, 10)
                            Main.mReport3!!.text = "$logTag\ndt = $delta_t"
                            fState = fOldState
                        }
                        FST_5 -> {
                            if (delta_t < 100) delta_t += 10 else delta_t += 100
                            Main.mReport3!!.text = "$logTag\ndt = $delta_t"
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
                        FST_8 -> {
                            forceX = 0
                            forceY = 0
                            udpSender.sendMessage(forceX, forceY)
                            Main.mReport2!!.text = "$logTag\n  forceX = $forceX  forceY = $forceY"
                            fState = fOldState
                        }
                        FST_9 -> {
                            forceX = 0
                            forceY = 0
                            cnt = 0
                            delta_t = 100

                            udpSender.sendMessage(forceX, forceY)
                            Main.mReport2!!.text = "$logTag\n  forceX = $forceX  forceY = $forceY"
                            Main.mReport1!!.text = logTag + "\n" + cnt
                            Main.mReport3!!.text = "$logTag\ndt = $delta_t"
                            send(EV_1_GO)
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
        const val EV_8 = 8

        private val events =
            arrayOf("ev_0", "ev_1", "ev_2", "ev_3", "ev_4", "ev_5", "ev_6", "ev_7", "ev_8")
        private val MAX_EVENT = events.size
        private const val FST_0 = 0
        private const val FST_1 = 1
        private const val FST_2 = 2
        private const val FST_3 = 3
        private const val FST_4 = 4
        private const val FST_5 = 5
        private const val FST_6 = 6
        private const val FST_7 = 7
        private const val FST_8 = 8
        private const val FST_9 = 9
        private val fstates = arrayOf(
            "0  FST_IDLE      ",
            "1  FST_1_delay  ",
            "2  FST_2_send",
            "3  FST_3_recv",
            "4  FST_4_deltaT-",
            "5  FST_5_deltaT+",
            "6  FST_6_",
            "7  FST_7_",
            "8  FST_8_reset",
            "9  FST_8_full_reset"
        )

//@formatter:off

        private val fsm_table: Array<IntArray> = arrayOf(
//                      0   1   2   3   4   5   6   7  8
//                    rst  go ext  PR
            intArrayOf( 8,  0,  4,  1,  4,  5,  6,  7,  9), // 0  FST_IDLE"
            intArrayOf( 8,  2,  0,  0,  4,  5,  6,  7,  9), // 1  FST_delay
            intArrayOf( 8,  3,  0,  0,  4,  5,  6,  7,  9), // 2  FST_send
            intArrayOf( 8,  1,  0,  0,  4,  5,  6,  7,  9), // 3  FST_recv
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7,  9), // 4  FST_deltaT-
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7,  9), // 5  FST_deltaT+
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7,  9), // 6  FST_forceX-
            intArrayOf( 0,  0,  0,  0,  4,  5,  6,  7,  9), // 7  FST_forceX+
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0,  9), // 8  FST_reset
            intArrayOf( 0,  0,  0,  0,  0,  0,  0,  0,  9), // 9  FST_full_reset
        )
//@formatter:on
    }
}