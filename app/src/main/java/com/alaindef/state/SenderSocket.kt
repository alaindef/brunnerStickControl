package com.alaindef.state

import android.util.Log
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress


val logTAG = "---MAIN ---"

object UDPSender {
    @Throws(Exception::class)
    @JvmStatic
    fun main() {
        fun byteArrayOfInts(vararg ints: Int) = ByteArray(ints.size) { pos -> ints[pos].toByte() }
//                https://stackoverflow.com/questions/51403881/creating-bytearray-in-kotlin
//        val message = "amai, das wa" // message to send
        val message = byteArrayOfInts( 174,0,0,0,0, 0x00,0x00,0x00,0x00,0x00,0x00,0x00,0xd5,0xfe,0xff,0xff,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00) // message to send
//        val message = "b'\\xae\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\xd5\\xfe\\xff\\xff\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00'" // message to send
        Main.mReport!!.text ="message len = " + message.size
        Log.wtf(logTAG, "message len = " + message.size + " mRep= " + Main.mReport!!.text)
        val port = 15090 // port number to send the packet
        val address = InetAddress.getByName("192.0.0.203") // destination address
//        val address = InetAddress.getByName("localhost") // destination address

        // create a datagram socket
        val socket = DatagramSocket()

        // create a datagram packet to hold the message
        val packet = DatagramPacket(message, message.size, address, port)

        // send the packet
        socket.send(packet)

        // close the socket
        socket.close()
    }
}