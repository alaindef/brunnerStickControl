package com.alaindef.state

import java.net.Socket


class Ztest constructor(private val serverAddress: String, private val serverPort: Int) {

    // Open a socket and connect to the server
    var socket = Socket(serverAddress, serverPort)
    var pr = serverAddress + " yep"
//    val ip = InetAddress.getByName(serverAddress)
    val logTag = ">---PosixSender---"


}

