package com.alaindef.brunner

/** 230417 created by alaindef */
import android.os.StrictMode
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

import java.io.IOException
import java.net.SocketTimeoutException

object UdpRecObject {
    var socketR: DatagramSocket? = null

    init {
        socketR = DatagramSocket(portR, InetAddress.getByName("0.0.0.0"))
        socketR!!.broadcast = true
        socketR!!.soTimeout = 4000
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


    fun getCoordinates(): Vector {
//        returns values from brunner range 0.00 .. 1.00
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val buffer = ByteArray(4096)
        var x = 0f
        var y = 0f

        try {
            val response = DatagramPacket(buffer, buffer.size)
            socketR!!.receive(response)

            val quote = convertToInts(response.data, 9)
            x = java.lang.Float.intBitsToFloat(quote[3])
            y = java.lang.Float.intBitsToFloat(quote[1])
            Main.mReport1!!.text =
                "(${String.format("%.${2}f", x)}  ${String.format("%.${2}f", y)})"
        } catch (ex: SocketTimeoutException) {
//            Main.mReport5!!.text = ex.message
//            Main.mReport5a!!.text = "Timeout error"
        } catch (ex: IOException) {
//            Main.mReport5!!.text = ex.message
//            Main.mReport5a!!.text = "Client error"
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
        return Vector(x, y)
    }

}