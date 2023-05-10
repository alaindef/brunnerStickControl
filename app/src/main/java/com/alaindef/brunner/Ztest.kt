package com.alaindef.brunner

import java.net.Socket

import android.util.Log


class Ztest constructor(private val serverAddress: String, private val serverPort: Int) {

    // Open a socket and connect to the server
    var socket = Socket(serverAddress, serverPort)
    var pr = serverAddress + " yep"

    //    val ip = InetAddress.getByName(serverAddress)
    val logTag = ">---PosixSender---"

    fun test() {
        val a = 3
        println("test")
        Log.i(logTag, "$a")
    }

    val a = 2
}

