package com.alaindef.brunner

/** 230417 created by alaindef */
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import java.lang.Integer.max
import java.net.InetAddress
import java.util.regex.Pattern


class PollMaster : Thread() {
    val logTag = ">---Sendy---"
    var calibrateButton: View? = null

    var event: Int = 0
    var cnt = 0
    var delta_t = 10



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

    fun moveToTarget() {
        Forces.calculateForces()
        udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
        Main.mReport3!!.text =
            "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
        if (cnt.mod(100) == 0)
            Main.mReport5a!!.text = "break $cnt"
        val res = UdpRecObject.getCoordinates()
        // return the result to sendy range of coordinates: 0f .. 1f
        Forces.currentRel = res            //range 0f .. 1f
//        Main.stickPad!!.invalidate()
        Main.stickPad!!.stick.pos = res
//        Main.stickPad!!.stickPos.show()

    }

    inner class ZeHandler  /*  https://developer.android.com/reference/android/os/Handler */
        (looper: Looper?) : Handler(looper!!) {


        override fun handleMessage(incomingMessage: Message) {
            // process incoming messages here
            val arg1 = incomingMessage.arg1
            val arg2 = incomingMessage.arg2
            val arg3 = incomingMessage.obj
            event = incomingMessage.what
            when (event) {
                EV_0_reset -> {
                    Forces.resetForces()
                    udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
                    Main.mReport3!!.text = "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
                    Main.mReport4!!.text = "$delta_t"
                    return
                }
                EV_1_full_reset -> {
                    running = false
                    whileNotPolling()
                    Forces.resetForces()
                    cnt = 0
                    delta_t = 5
                    udpSender.sendUDP(Forces.forces.x, Forces.forces.y, ipAddress, 15090)
                    Main.mReport3!!.text = "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
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
                        moveToTarget()
                        Handler().postDelayed({ send(EV_3_next_round) }, delta_t.toLong())
                    }
                }
                EV_4_calibrateOne -> {
                    Main.stickPad!!.drawTarget(0.75f, arg1 / 10f)
                    if ((arg1 < 10)) {
                        Handler().postDelayed(
                            { Forces.calibrateOne(arg1 + arg2, arg2) }, 1000.toLong()
                        )
                    } else {
                        Handler().postDelayed({ Forces.calibrateEnd(arg1) }, 1000.toLong())
                        calibrateButton!!.setBackgroundColor(
                            ContextCompat.getColor(
                                Main.mContext!!,
                                R.color.buttonfirstcolor
                            )
                        )
                    }

                }
                EV_5_calibratePos -> {
                    val xpos = arg1
                    val ypos = arg2
                    val bounds = arg3 as Square

//                    Main.stickPad!!.invalidate()
                    println("i=$xpos  j=$ypos   square= $bounds")
                    Main.stickPad!!.target.setPos(xpos/10f, ypos/10f)
//                    Forces.stickPos.setPositionRel((xpos+.3f)/10f, ypos/10f)
//                    Main.stickPad!!.invalidate()
//                    Main.stickPad!!.drawTarget(xpos / 10f, ypos / 10f)

                    Handler().postDelayed(
                        { send(EV_6_deviation, xpos, ypos, bounds) }, 1000.toLong()
                    )
//                    if (xpos < bounds.r)
//                        Handler().postDelayed(
//                            { send(EV_5_calibratePos, xpos + 1, ypos, bounds) }, 500.toLong()
//                        )
//                    else {
//                        if (ypos < bounds.d)
//                            Handler().postDelayed(
//                                { send(EV_5_calibratePos, bounds.l, ypos + 1, bounds) }, 500.toLong()
//                            )
//                    }
                }
                EV_6_deviation -> {
                    val xpos = arg1
                    val ypos = arg2
                    val bounds = arg3 as Square

                    val stickx = Main.stickPad!!.stick.pos.x
                    val sticky = Main.stickPad!!.stick.pos.y
                    Main.stickPad!!.stick.setPos(stickx, sticky + 0.12f)
                    println("deviation= ${Main.stickPad!!.target}   ${Main.stickPad!!.stick}")
                    if (xpos < bounds.r)
                        send(EV_5_calibratePos, xpos + 1, ypos, bounds)
                    else {
                        if (ypos < bounds.d)
                          send(EV_5_calibratePos, bounds.l, ypos + 1, bounds)
                    }

//                    Main.stickPad!!.invalidate()
                }
                EV_7_force -> {
                    Main.stickPad!!.target.setPos(.8f, Main.stickPad!!.target.pos.y + 0.1f)
                    Forces.targetRel = Main.stickPad!!.target.pos
                    Main.stickPad!!.invalidate()
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
                    Forces.forces.x -= 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${Forces.forces.x.toInt()}"
                }
                EV_14_force_plus -> {
                    Forces.forces.x += 100f
                    Main.mReport3!!.text = "$logTag\nforceX = ${Forces.forces.x.toInt()}"
                }
                EV_21_from_slider -> {
                    Forces.newPIDParam(arg1.toFloat(), arg3.toString())
//                    println("sendy receives pos $arg1 from $arg3")
                }
                EV_22_resetCorY -> {
                    Main.correctionView!!.resetCorY()
                }
                EV_23_calibrate -> {
                    calibrateButton = arg3 as androidx.appcompat.widget.AppCompatTextView
                    calibrateButton!!.setBackgroundColor(
                        ContextCompat.getColor(
                            Main.mContext!!,
                            R.color.buttonsecondcolor
                        )
                    )
                    Forces!!.calibrateAll()
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
        const val EV_4_calibrateOne = 4
        const val EV_5_calibratePos = 5
        const val EV_6_deviation = 6
        const val EV_7_force = 7
        const val EV_9_new_IP = 9
        const val EV_11_dt_min = 11
        const val EV_12_dt_plus = 12
        const val EV_13_force_min = 13
        const val EV_14_force_plus = 14
        const val EV_21_from_slider = 21
        const val EV_22_resetCorY = 22
        const val EV_23_calibrate = 23
        private var running = false

    }
}