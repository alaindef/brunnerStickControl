package com.alaindef.brunner

/** 230417 created by alaindef */
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.android.material.theme.MaterialComponentsViewInflater
import java.lang.Integer.max
import java.net.InetAddress
import java.util.*
import java.util.regex.Pattern


class PollMaster : Thread() {
    val logTag = ">---Sendy---"

    var event: Int = 0
    var cnt = 0
    var delta_t = 10
    var xCurrent = 5f
    var yCurrent = 15f
    var currentPosReceived = false
    var xTarget = 0f
    var yTarget = 0f
    var targetPosReceived = false
    var forceX = 0f
    var forceY = 0f

    var conP = 50f
    var conI = 0f
    var conPv = 50f
    var conIv = 0f

    private var ipAddress: InetAddress = InetAddress.getByName("192.168.0.203")

    private var mHandler: ZeHandler? = null

    override fun run() {
        mHandler = ZeHandler(Looper.getMainLooper())
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, arg1, arg2, obj)) //todo why 0 ?
    }

    fun send(what: Int) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, 0, 0, null))
    }


    private val PATTERN: Pattern = Pattern.compile(
        "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
    )

    private fun validate(ip: String?): Boolean {
        return PATTERN.matcher(ip).matches()
    }

    fun whileNotPolling() {
        // now we can read the dialog box for changing the IP address of the brunner interface
        Main.mReport0!!.setBackgroundColor(-13388315)  // -13388315 is holo_blue_light (chat)
        Main.mReport0!!.text = ""
        if (Main.mIPDialog!!.getText() != null) {
            val address = Main.mIPDialog!!.getText().toString()
            if (validate(address)) {
                ipAddress = InetAddress.getByName(address)
                Main.mReport0!!.text = "Brunner ip $address"
            } else
                Main.mReport0!!.text = "INVALID ip address: $address"
        }
    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {

        private val queue: Queue<Float> = LinkedList(listOf(0f, 0f, 0f))

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    forceX = 0f
                    forceY = 0f
                    udpSender.sendUDP(forceX, forceY, ipAddress, 15090)
                    Main.mReport3!!.text = "(${forceX.toInt()} ${forceY.toInt()})"
                    Main.mReport4!!.text = "$delta_t"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    whileNotPolling()
                    forceX = 0f
                    forceY = 0f
                    cnt = 0
                    delta_t = 100
                    //not used: after sending forces, receiver waits for input immediately, with timeout
                    udpSender.sendUDP(forceX, forceY, ipAddress, 15090)
                    Main.mReport3!!.text = "(${forceX.toInt()} ${forceY.toInt()})"
                    Main.mReport0!!.text = "$logTag   $cnt "
                    Main.mReport4!!.text = "$delta_t"

                    return
                }
                EV_2_start_stop -> {
                    if (running) {
                        running = false
                        whileNotPolling()
                    } else {
                        running = true
                        send(EV_3_next_round)
                        Main.mReport4!!.text = "$delta_t"
                    }
                }
                EV_3_next_round -> {
                    if (running) {
                        cnt++
                        Main.mReport0!!.text = "$cnt: running ..."
                        Main.mReport0!!.setBackgroundColor(Color.RED)
                        val forces = Forces.calculateForces(
                            Vector(xCurrent, yCurrent),
                            Vector(xTarget, yTarget)
                        )
                        udpSender.sendUDP(forces.x, forces.y, ipAddress, 15090)
                        Main.mReport3!!.text = "(${forceX.toInt()} ${forceY.toInt()})"
                        if (cnt.mod(100) == 0)
                            Main.mReport5a!!.text = "break $cnt"
                        recky.send(RecMaster.EV_0)
                        Main.mPad!!.invalidate()
                    }
                }
                EV_4_target_pos -> {
                    targetPosReceived = true
                }
                EV_6_current_pos -> {
                    currentPosReceived = true
                    val res = arg3 as Vector
                    xCurrent = res.x                //range 0f .. 1f
                    yCurrent = res.y
//                    send(EV_3_next_round)
                    Handler().postDelayed({ send(EV_3_next_round) }, delta_t.toLong())

                }
                EV_9_new_IP -> {
                    if (!running) {
                        whileNotPolling()
                    }
                }
                EV_11_dt_min -> {
                    if (delta_t <= 10) delta_t -= 1 else if (delta_t <= 100) delta_t -= 10 else delta_t -= 100
                    delta_t = max(delta_t, 1)
                    Main.mReport4!!.text = "$delta_t"
                }
                EV_12_dt_plus -> {
                    if (delta_t < 10) delta_t += 1 else if (delta_t < 100) delta_t += 10 else delta_t += 100
                    Main.mReport4!!.text = "$delta_t"
                    queue.add(delta_t.toFloat())
                    val first = queue.remove()
                    val sum = queue.reduceOrNull { acc, i -> acc + i } ?: 0
                    Main.mReport5!!.text = "len=${queue.size} $first  sum=$sum  delta_t=$delta_t"
                }
                EV_13_force_min -> {
                    forceX -= 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${forceX.toInt()}"
                }
                EV_14_force_plus -> {
                    forceX += 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${forceX.toInt()}"
                }
                EV_21_from_slider -> {
                    Forces.newPIDParam(arg1.toFloat(), arg3.toString())
//                    println("sendy receives pos $arg1 from $arg3")
                }
                else -> {
                    Log.e(logTag, "EVENT $event unknown")
                    Main.mReport5a!!.text = "sendy: incoming EVENT unknown"
                    Main.mReport5!!.text = "$event"
                }
            }
        }
    }


    companion object {
        const val EV_0_reset = 0
        const val EV_1_full_reset = 1
        const val EV_2_start_stop = 2
        const val EV_3_next_round = 3
        const val EV_4_target_pos = 4
        const val EV_5_current_pos = 5
        const val EV_6_current_pos = 6
        const val EV_9_new_IP = 9
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        const val EV_21_from_slider = 21
        private var running = false

    }
}