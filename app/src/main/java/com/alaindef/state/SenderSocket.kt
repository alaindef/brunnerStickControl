import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

object UDPSender {
    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>) {

        val message = "b'\\xae\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\xd5\\xfe\\xff\\xff\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00\\x00'" // message to send
//        val message = "Hello, World!" // message to send
        val port = 15090 // port number to send the packet
        val address = InetAddress.getByName("192.0.0.203") // destination address
//        val address = InetAddress.getByName("localhost") // destination address

        // create a datagram socket
        val socket = DatagramSocket()

        // create a datagram packet to hold the message
        val packet = DatagramPacket(message.toByteArray(), message.length, address, port)

        // send the packet
        socket.send(packet)

        // close the socket
        socket.close()
    }
}