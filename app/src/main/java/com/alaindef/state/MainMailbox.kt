package com.alaindef.state

import android.os.Handler
import android.os.Message
import android.os.StrictMode
import android.util.Log
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

/** Created by alaindef on 230417 */
class MainMailbox  //   messages
    : Handler() {
    //    private final WeakReference<Main> currentActivity;
    private var timerep = 0
    override fun handleMessage(m: Message) {
        val logTAG = "---MAIN ---"
        when (m.what) {
            MBX_0 -> {
            }
            RECEIVE -> {
                getResponse()
            }
            TEST -> Log.wtf(logTAG, "arg1= " + m.arg1)
            SENDPACKET -> {
                Log.wtf(logTAG, "sending udp" + m.obj)
                udpSender.sendMessage(0)
                Log.wtf(logTAG, "awake now ..........." )
//                omer.send(PollMaster2.ev_poll_and_repeat)
            }
            else -> Log.wtf(logTAG, "message unknown " + m.what + "/" + m.arg1)
        }
    }

    fun convertToInts(bytes: ByteArray, nbrOfInts: Int): IntArray {
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
    fun getResponse() = runBlocking<Unit>{

        val buffer = ByteArray(4096)
        var socketR: DatagramSocket? = null


        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        try {
            socketR = DatagramSocket(portR, InetAddress.getByName("0.0.0.0"))
            socketR.broadcast = true
            socketR.soTimeout = 2000
            Main.mReport5!!.text = "waiting ........................"

            val response =
                DatagramPacket(buffer, buffer.size)
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

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        sendMessage(obtainMessage(what, arg1, arg2, obj))
    }

    fun send(what: Int) {
        sendMessage(obtainMessage(what, 0, 0, null)) // code inspection problem
    }

    companion object {
        //    messages
        const val MBX_0 = 0
        const val RECEIVE = 1
        const val RESET = 2
        const val SHUFFLE = 3
        const val REPORT_ELAPSED_TIME = 6
        const val TEST = 7
        const val SENDPACKET = 8
    }
}