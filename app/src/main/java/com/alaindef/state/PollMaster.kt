package com.alaindef.state

/** 230417 created by alaindef */
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import java.lang.Integer.max
import java.lang.Integer.min
import java.lang.Math.abs
import java.net.InetAddress
import java.util.regex.Pattern


/** 230417 created by alaindef */
class PollMaster : Thread() {
    val logTag = ">---Sendy---"

    var event: Int = 0
    var cnt = 0
    var delta_t = 5
    var xCurrent = 0f
    var yCurrent = 0f
    var currentPosReceived = false
    var xTarget = 0f
    var yTarget = 0f
    var targetPosReceived = false
    var forceX = 0
    var forceY = 0
    var alfa = 50
    var conI = 0f
    private var ipAddress: InetAddress = InetAddress.getByName("192.168.0.203")

    private var mHandler: ZeHandler? = null

    private val miniPID = MiniPID(1f, 0f, 0f)

    override fun run() {
        mHandler = ZeHandler(Looper.getMainLooper())
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, arg1, arg2, obj)) //todo why 0 ?
    }

    fun send(what: Int) {
        mHandler!!.sendMessage(mHandler!!.obtainMessage(what, 0, 0, null))
    }

    fun calculateForcesSave() {
        forceX = alfa * (30 * abs(xTarget - xCurrent)).toInt()
        if (xTarget > xCurrent) forceX = -forceX
        forceY = alfa * min(170, abs(70 * (yTarget - yCurrent)).toInt())
        val sq = (1 + yCurrent) * (1 + yCurrent)
        forceY = (forceY * (1 + kotlin.math.abs(sq) / 10)).toInt()
        if (yTarget > yCurrent) forceY = -forceY
    }

    fun calculateForces0() {
        forceX = alfa * (50 * kotlin.math.abs(xTarget - xCurrent)).toInt()
        if (xTarget > xCurrent) forceX = -forceX
        forceY = alfa * min(250, abs(120 * (yTarget - yCurrent)).toInt())
        val sq = (1 + yCurrent) * (1 + yCurrent)
        forceY = (forceY * (1 + kotlin.math.abs(sq) / 10)).toInt()
        if (yTarget > yCurrent) forceY = -forceY
    }
    fun calculateForces() {

        miniPID.setP(50f*alfa)
        miniPID.setI(conI/100f)
        miniPID.setDirection(true)
//        miniPID.setOutputLimits(1000f)
        miniPID.setSetpoint(xTarget)

        forceX = miniPID.getOutput(xCurrent, xTarget).toInt()


//        forceX = alfa * (50 * kotlin.math.abs(xTarget - xCurrent)).toInt()
//        if (xTarget > xCurrent) forceX = -forceX


        forceY = alfa * min(250, abs(120 * (yTarget - yCurrent)).toInt())
        val sq = (1 + yCurrent) * (1 + yCurrent)
        forceY = (forceY * (1 + kotlin.math.abs(sq) / 10)).toInt()
        if (yTarget > yCurrent) forceY = -forceY
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

        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    forceX = 0
                    forceY = 0
                    udpSender.sendUDP(forceX, forceY, ipAddress, 15090)
                    Main.mReport3!!.text = "($forceX $forceY)"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    whileNotPolling()
                    forceX = 0
                    forceY = 0
                    cnt = 0
                    delta_t = 100
                    //not used: after sending forces, receiver waits for input immediately, with timeout
                    udpSender.sendUDP(forceX, forceY, ipAddress, 15090)
                    Main.mReport3!!.text = "($forceX $forceY)"
                    Main.mReport0!!.text = "$logTag   $cnt "

                    return
                }
                EV_2_start_stop -> {
                    if (running) {
                        running = false
                        whileNotPolling()
                    } else {
                        running = true
                        send(EV_3_next_round)
                    }
                }
                EV_3_next_round -> {
                    if (running) {
                        cnt++
                        Main.mReport0!!.text = "$cnt: running ..."
                        Main.mReport0!!.setBackgroundColor(Color.RED)
                        calculateForces()
                        udpSender.sendUDP(forceX, forceY, ipAddress, 15090)
                        Main.mReport3!!.text = "($forceX $forceY)"
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
                    xCurrent = res.x
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
                }
                EV_13_force_min -> {
                    forceX -= 100
                    Main.mReport3!!.text = "$logTag\nforceX = $forceX"
                }
                EV_14_force_plus -> {
                    forceX += 100
                    Main.mReport3!!.text = "$logTag\nforceX = $forceX"
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
        private var running = false

    }
}