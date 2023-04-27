package com.alaindef.state

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer

import java.io.IOException
import java.net.SocketTimeoutException


class UdpReceiver(val portReceiver: Int) {

//    val socketR = DatagramSocket(port)
//    val ip = InetAddress.getByName("0.0.0.0")
    val logTag = ">---UdpReceiver---"

//    fun init() {
//        socketR.bind(ip, port)
//    }

//    private fun close() {
//        try {
//            socketR.close()
//        } catch (e: Exception) {
//            Log.w(logTag, "socket close failed")
//        }
//    }

    // finalize() method is called when the object is garbage collected
//    protected fun finalize() {
//        close()
//    }

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

    fun get() {

        val buffer = ByteArray(4096)
        var socketR: DatagramSocket? = null

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
        } catch (ex: IOException) {
            println("Client error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }
        socketR!!.close()
    }
}