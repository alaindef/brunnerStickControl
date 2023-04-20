package com.alaindef.state


import android.util.Log

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import java.nio.ByteBuffer

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel

class Testcon {

    private val logTag = "---Main--testcon"
    fun babble(args: Array<String>) = runBlocking<Unit> {

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

        var hostname = "192.168.0.203"
//    val hostname = "192.168.0.203"
        val byteMessage = convertTheIndians(intArrayOf(0xAE, 0, 0, 0, 0, 0, 0, 0, 0))
        var ip = InetAddress.getByName(hostname)

        val socketS = DatagramSocket()
        if (socketS.isConnected)  Log.d(logTag, " socketS is ok") else Log.d(logTag, " socketS is NOK")
        val socketR = DatagramSocket(portR)


        socketR.soTimeout = 2000

        try {
//        socketR.soTimeout = 2000
            val buffer = ByteArray(4096)
            val response = DatagramPacket(buffer, buffer.size, InetAddress.getByName("0.0.0.0"), portR)
            while (true) {
                val request = DatagramPacket(byteMessage, byteMessage.size, ip, portR)
                socketS.send(request)
//            println("connection: ${socketR.isConnected}")      // gives false ??
                socketR.receive(response)
                val quote = convertToInts(response.data, 9)
                val x = java.lang.Float.intBitsToFloat(quote[3])
                val y = java.lang.Float.intBitsToFloat(quote[1])
                println("chat x y: $x  $y  from ${response.address}")

                Thread.sleep(500)
            }
        } catch (ex: SocketTimeoutException) {
            println("Timeout error: " + ex.message)
//        ex.printStackTrace()
        } catch (ex: IOException) {
            println("Client error: " + ex.message)
            ex.printStackTrace()
        } catch (ex: InterruptedException) {
            ex.printStackTrace()
        }

        // Try adding program arguments via Run/Debug configuration.
        // Learn more about running applications: https://www.jetbrains.com/help/idea/running-applications.html.
//    println("Program arguments: ${args.joinToString()}")
    }

}