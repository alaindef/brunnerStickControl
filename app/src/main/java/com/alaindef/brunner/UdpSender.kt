package com.alaindef.brunner

/** 230417 created by alaindef */

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

class UdpSender() {

    val logTag = ">---UdpSender---"

    fun little2big(word: Int): Int {
        return (word and 0xff shl 24) or (word and 0xff00 shl 8) or (word and 0xff0000 shr 8) or (word shr 24 and 0xff)
    }

    fun convertTheIndians(ints: IntArray): ByteArray {
        val byteBuffer = ByteBuffer.allocate(ints.size * 4)
        val intBuffer = byteBuffer.asIntBuffer()
        for (i in 0 until ints.size) {
            intBuffer.put(little2big(ints[i]))
        }
        return byteBuffer.array()
    }

    fun sendUDP(forceX: Float, forceY: Float) {
        val policy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        val byteMessage = convertTheIndians(intArrayOf(0xAE, forceY.toInt(), 0, forceX.toInt(), 0, 0, 0, 0, 0))
        try {
            val socketS = DatagramSocket()

            val request = DatagramPacket(byteMessage, byteMessage.size, Main.ipAddress, Main.portS)
            socketS.send(request)
            socketS.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}