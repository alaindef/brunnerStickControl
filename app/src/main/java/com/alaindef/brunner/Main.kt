package com.alaindef.brunner

/** 230417 created by alaindef */
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.net.InetAddress
import java.util.regex.Pattern

data class VectorF(var x: Float, var y: Float) {
    infix fun add(arg: VectorF): VectorF {
        return VectorF(x + arg.x, y + arg.y)
    }

    infix fun plus(arg: VectorF): VectorF {
        return VectorF(x + arg.x, y + arg.y)
    }

    infix fun minus(arg: VectorF): VectorF {
        return VectorF(x - arg.x, y - arg.y)
    }

    infix fun minus(arg: VectorI): VectorF {
        return VectorF(x - arg.x.toFloat(), y - arg.y.toFloat())
    }

    infix fun mul(arg: Float): VectorF {
        return VectorF(x * arg, y * arg)
    }

    infix fun mul(arg: VectorF): VectorF {
        return VectorF(x * arg.x, y * arg.y)
    }

    infix fun divideBy(arg: Float): VectorF {
        return VectorF(x / arg, y / arg)
    }

    infix fun divideBy(arg: VectorF): VectorF {
        return VectorF(x / arg.x, y / arg.y)
    }

    fun toIntVector(): VectorI {
        return VectorI(x.toInt(), y.toInt())
    }

    fun maxOf(arg: VectorF): VectorF {
        return VectorF(maxOf(x, arg.x), maxOf(y, arg.y))
    }

    fun minOf(arg: VectorF): VectorF {
        return VectorF(minOf(x, arg.x), minOf(y, arg.y))
    }
}

data class VectorI(val x: Int, val y: Int) {
    fun max(arg: VectorI): VectorI {
        return VectorI(kotlin.math.max(x, arg.x), kotlin.math.max(y, arg.y))
    }

    infix fun mul(arg: Int): VectorI {
        return VectorI(x * arg, y * arg)
    }

    infix fun plus(arg: VectorI): VectorI {
        return VectorI(x + arg.x, y + arg.y)
    }

}

data class Square(val l: Int, val u: Int, val r: Int, val d: Int)

data class SquareI(val topLeft: VectorI, val bottomRight: VectorI)

@Suppress("unused")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"
    fun sendEvent(view: View?) {
        when (val ss = view!!.tag) {
            "B_poll" -> {
                sendy.send(PollMaster.EV_2_start_stop)
            }
            "B_RES" -> {
//                omer.send(PollMaster.EV_0, 0, 0, ss)  //handled by onclick listener
//                because long press is also used
            }
            "dt-" -> {
                sendy.send(PollMaster.EV_11_dt_min, 0, 0, ss)
            }
            "dt+" -> {
                sendy.send(PollMaster.EV_12_dt_plus, 0, 0, ss)
            }
            "caltyp" -> {
                when (Forces.calType) {
                    "interpol" -> Forces.calType = "none"
                    "none" -> Forces.calType = "interpol"
                    else -> Forces.calType = "none"
                }
                mCalibTypeButton!!.text = Forces.calType
            }
            "calmax" -> {
                when (Forces.calibMax) {
                    4 -> Forces.calibMax = 5
                    5 -> Forces.calibMax = 10
                    10 -> Forces.calibMax = 20
                    20 -> Forces.calibMax = 50
                    50 -> Forces.calibMax = 100
                    100 -> Forces.calibMax = 4
                    else -> Forces.calibMax = 5
                }
                Forces.calibMaxF = Forces.calibMax.toFloat()
                Forces.calibJump = 100 / Forces.calibMax
                Forces.calibDelay = if (Forces.calibMax > 10) 1000 else 500
                mCalibMaxButton!!.text = Forces.calibMax.toString()
            }
            "interpol" -> {
                Forces.calType = "interpol"     //interpolate between calib points
            }
            "calibrate" -> {
                sendy.send(PollMaster.EV_7_calibratePos, 0, 0, view)
                view.setBackgroundColor(
                    ContextCompat.getColor(mContext!!, R.color.buttonsecondcolor)
                )
//                Forces.calType = "none"        //no correction
//                mCalibTypeButton!!.text = "calibrate"
                Log.i(logTag, "calmax= ${Forces.calibMax}")
            }

            "resetCorrections" -> {
                Forces.resetCorrections()
                correctionView!!.resetCorY()
                sendy.calibrating = false
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_blue))
            }
            else -> Log.wtf(logTag, "tag unknown $ss")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            "message",
            "onSaveInstanceState: orientation changed"
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StrictMode.setVmPolicy(
            VmPolicy.Builder(StrictMode.getVmPolicy())
                .detectLeakedClosableObjects()
                .build()
        )

        if (savedInstanceState != null) {
            Log.i(logTag, "savedInstanceState: orientaton changed and strings=#strings")
        }

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT -> setContentView(R.layout.portrait)
//            Configuration.ORIENTATION_LANDSCAPE -> setContentView(R.layout.mainland)
        }

        mContext = this.applicationContext
        mContextForDummies = this // found this, but why ???

        mIPDialog = findViewById<View>(R.id.IPDialog) as EditText
        mIPDialog?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                // This method is called before the text is changed
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // This method is called when the text is changed
                sendy.send(PollMaster.EV_15_new_IP)
            }

            override fun afterTextChanged(s: Editable?) {
                // This method is called after the text is changed
            }
        })


        mReport0 = findViewById<View>(R.id.report0) as TextView
        mReport1 = findViewById<View>(R.id.report1) as TextView
        mReport2 = findViewById<View>(R.id.report2) as TextView
        mReport3 = findViewById<View>(R.id.report3) as TextView
        mReport4a = findViewById<View>(R.id.report4a) as TextView
        mReport4 = findViewById<View>(R.id.report4) as TextView
        mReport5a = findViewById<View>(R.id.report5a) as TextView
        mReport5 = findViewById<View>(R.id.report5) as TextView
        mContext = this.applicationContext

        mReset = findViewById<View>(R.id.reset) as Button
        mReset?.setOnClickListener {
            sendy.send(PollMaster.EV_0_reset)
        }
        mReset?.setOnLongClickListener {
            sendy.send(PollMaster.EV_1_full_reset)
            true
        }

        conPReport = findViewById(R.id.conPreport)
        conIReport = findViewById(R.id.conIreport)
        conPvReport = findViewById(R.id.conPvreport)
        conIvReport = findViewById(R.id.conIvreport)

        correctionView = findViewById(R.id.yTable)
        stickPad = findViewById(R.id.del)

        mCalibTypeButton = findViewById(R.id.tile3)
        mCalibMaxButton = findViewById(R.id.tile4)

        if (savedInstanceState == null) {
            // sendy is an FMM (Finite Message Machine) that handles all the incomming events:
            // start and stop the polling, reset, receive target and current positions,
            // request to change the IP address of the brunner interface, change polling intervals
            sendy.start()
            recky.start()
        }
        if (sendy.isAlive) Log.e(logTag, "sendy lives") else Log.e(logTag, "sendy is dead")
        if (recky.isAlive) Log.e(logTag, "recky lives") else Log.e(logTag, "recky is dead")
    }

    companion object {
        const val version = 0.1
        const val DIM: Float = 0.5f // for Alpha, between 0 and 1 for textview

        // between 0 and 255 for imageview
        @JvmField
        var ANIMATION_LAG = 300

        @SuppressLint("StaticFieldLeak")
        var mIPDialog: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport0: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReset: Button? = null

        @SuppressLint("StaticFieldLeak")
        var mReport1: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport2: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport3: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport4a: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport4: TextView? = null

//        @SuppressLint("StaticFieldLeak")
        var mReport5a: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport5: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport6: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null

        @SuppressLint("StaticFieldLeak")
        var mContextForDummies: Context? = null

        @SuppressLint("StaticFieldLeak")
        var conPReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var conIReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var conPvReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var conIvReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var correctionView: PolylineView? = null

        @SuppressLint("StaticFieldLeak")
        var stickPad: PadView? = null

        var mCalibTypeButton: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mCalibMaxButton: TextView? = null


        const val portR = 15095
        const val portS = 15090
        var ipAddress: InetAddress = InetAddress.getByName("192.168.0.203")


        private val PATTERN: Pattern = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
        )

        private fun validate(ip: String?): Boolean {
            return PATTERN.matcher(ip.toString()).matches()
        }

        fun whilePolling(cnt: Int, deltaT: Int) {
            mReport0!!.text = "$cnt "
            mReport4!!.text = "$deltaT"
            mReport3!!.text = "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})"
            if (PollMaster.running) {
                PollMaster.cnt++
                mReport0!!.text = "$cnt: running ..."
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_red))
            } else {
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_blue))
                mReport0!!.text = ""
            }

            // report1
            val x = Forces.currentRel.x
            val y = Forces.currentRel.y
            val stickposTxt = "pos=(${String.format("%.${2}f", x)}  ${String.format("%.${2}f", y)})"
            val delta = stickPad!!.target.pos minus VectorF(x, y)
            val deltaTxt = " d=(${String.format("%.${2}f", delta.x)}  ${String.format("%.${2}f", delta.y)})"
            "$stickposTxt$deltaTxt".also { mReport1!!.text = it }

            // report2
            val tpos = stickPad!!.target.pos
            val corrp = Forces.correctInterpol(tpos)
            val tpText = "(%.2f %.2f)".format(tpos.x, tpos.y)
            val corrpText = "(%.2f %.2f)".format(corrp.x, corrp.y)
            "$tpText $corrpText".also { mReport2!!.text = it }

            // report5
            val tposIndex = (tpos mul VectorF(100f,100f)).toIntVector()
            "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})".also { mReport3!!.text = it }
            val repPos = "(%.2f %.2f)".format(tpos.x, tpos.y)
            val repCorTar = "(%.2f %.2f)".format(Forces.corrected.x, Forces.corrected.y)
            val corrections = Forces.corrections[tposIndex.x][tposIndex.y]
            val repCorrections = "(%.2f %.2f)".format(corrections.x, corrections.y)
            "$repPos  $repCorTar  $repCorrections".also { mReport5!!.text = it }
            "(target pos)  (corrected target)  (corrections)".also { mReport5a!!.text = it }
        }

        fun whileNotPolling() {
            // now we can read the dialog box for changing the IP address of the brunner interface
            if (mIPDialog!!.getText() != null) {
                val address = mIPDialog!!.getText().toString()
                if (validate(address)) {
                    ipAddress = InetAddress.getByName(address)
                    "Brunner ip $address".also { mReport0!!.text = it }
                } else
                    "INVALID ip address: $address".also { mReport0!!.text = it }
            }
        }


    }
}

//adf230511 recky not needed (2 threads seems too much for android) var recky = RecMaster()
var sendy = PollMaster()
var recky = RecMaster()
val udpSender: UdpSender = UdpSender()



