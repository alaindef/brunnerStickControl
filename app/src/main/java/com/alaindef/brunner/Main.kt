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
    fun maxV(arg: VectorI): VectorI {
        return VectorI(kotlin.math.max(x, arg.x), kotlin.math.max(y, arg.y))
    }

    fun minV(arg: VectorI): VectorI {
        return VectorI(kotlin.math.min(x, arg.x), kotlin.math.min(y, arg.y))
    }

    infix fun mul(arg: Int): VectorI {
        return VectorI(x * arg, y * arg)
    }

    infix fun divideBy(arg: Int): VectorI {
        return VectorI(x / arg, y / arg)
    }

    infix fun plus(arg: VectorI): VectorI {
        return VectorI(x + arg.x, y + arg.y)
    }

}

//@Suppress("DEPRECATION")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"
    fun buttonClick(view: View?) {
        // called when a button is pressed. param view is the button, carrying the "tag" field.
        // see layout file portrait.xml
        when (val ss = view!!.tag) {
            "B_poll" -> sendy.send(PollMaster.EV_2_start_stop)
            "B_RES" -> sendy.resetFull()
            "dt-" -> sendy.dtMin()
            "dt+" -> sendy.dtPlus()
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
                Forces.calibDelay = if (Forces.calibMax < 10) 1000 else 500
                mCalibMaxButton!!.text = Forces.calibMax.toString()
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
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_blue))
                if (sendy.calibrateButton != null)
                    sendy.calibrateButton!!.setBackgroundColor(
                        ContextCompat.getColor(mContext!!, R.color.button_first_color)
                    ) else Log.e(logTag, "calibrateButton not yet initialised")
                // adf test for stub - put stick in center
                stickPad!!.stick.setPosV(VectorF(0.5f, 0.5f))
                stickPad!!.target.setPosV(VectorF(0.5f, 0.5f))
                udpSender.sendUDP(0f, 0f)
            }
            "B_apply_force" -> {
//                Log.wtf(logTag, "forces applied = ${Forces.forces}")
//                udpSender.sendUDP(Forces.forces.x, Forces.forces.y)
                Log.wtf(logTag, "tag not used $ss")
            }
            else -> Log.wtf(logTag, "tag unknown $ss")
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        mReport0 = null
        mReport1 = null
        mReport2 = null
        mReport3 = null
        mReport4a = null
        mReport4 = null
        mReport5a = null
        mReport5 = null
        mContext = null
        mReset = null
        conPReport = null
        conIReport = null
        conPvReport = null
        conIvReport = null
        forceReport = null
        correctionView = null
        stickPad = null
        mCalibTypeButton = null
        mCalibMaxButton = null

        mContext = null
        mContextForDummies = null
        mIPDialog = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            "message",
            "onSaveInstanceState: orientation changed"
        )
    }

    @SuppressLint("ClickableViewAccessibility", "SwitchIntDef")
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
            Configuration.ORIENTATION_PORTRAIT -> {
                setContentView(R.layout.portrait)
            }
            Configuration.ORIENTATION_LANDSCAPE -> {
                setContentView(R.layout.landscape)
            }
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

        conPReport = findViewById(R.id.conPreport)
        conIReport = findViewById(R.id.conIreport)
        conPvReport = findViewById(R.id.conPvreport)
        conIvReport = findViewById(R.id.conIvreport)
        forceReport = findViewById(R.id.forcereport)

        correctionView = findViewById(R.id.yTable)
        stickPad = findViewById(R.id.del)

        mCalibTypeButton = findViewById(R.id.tile3)
        mCalibMaxButton = findViewById(R.id.tile4)

        if (savedInstanceState == null) {
            // sendy is an FMM (Finite Message Machine) that handles all the incomming events:
            // start and stop the polling, reset, receive target and current positions,
            // request to change the IP address of the brunner interface, change polling intervals
            sendy.start()
        }
        if (sendy.isAlive) Log.i(logTag, "sendy lives") else Log.e(logTag, "sendy is dead")
        if (recky.isAlive) Log.i(logTag, "recky lives") else Log.e(logTag, "recky is dead")
    }

    companion object {
        // between 0 and 255 for imageview
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

        @SuppressLint("StaticFieldLeak")
        var mReport5a: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport5: TextView? = null

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
        var forceReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var correctionView: PolylineView? = null

        @SuppressLint("StaticFieldLeak")
        var stickPad: PadView? = null

        @SuppressLint("StaticFieldLeak")
        var mCalibTypeButton: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mCalibMaxButton: TextView? = null


        const val portR = 15095
        const val portS = 15090
        var ipAddress: InetAddress = InetAddress.getByName("192.168.0.204")


        private val PATTERN: Pattern = Pattern.compile(
            "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$"
        )

        private fun validate(ip: String?): Boolean {
            return PATTERN.matcher(ip.toString()).matches()
        }

        fun whilePolling(cnt: Int, deltaT: Int) {
            "$cnt ".also { mReport0!!.text = it }
            mReport4!!.text = "$deltaT"
            "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})".also { mReport3!!.text = it }
            if (PollMaster.running) {
                PollMaster.cnt++
                "$cnt: running ...".also { mReport0!!.text = it }
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_red))
            } else {
                mReport0!!.setBackgroundColor(ContextCompat.getColor(mContext!!, R.color.my_blue))
                mReport0!!.text = ""
            }

            // report1
            val x = stickPad!!.stick.pos.x
            val y = stickPad!!.stick.pos.y
            val stickposTxt = "pos=(${String.format("%.${2}f", x)}  ${String.format("%.${2}f", y)})"
            val delta = stickPad!!.target.pos minus VectorF(x, y)
            val deltaTxt =
                " d=(${String.format("%.${2}f", delta.x)}  ${String.format("%.${2}f", delta.y)})"
            "$stickposTxt$deltaTxt".also { mReport1!!.text = it }

            // report2
            val tPos = stickPad!!.target.pos
            val corrP = VectorF(0f, 0f)
            val tpText = "(%.2f %.2f)".format(tPos.x, tPos.y)
            val corrPText = "(%.2f %.2f)".format(corrP.x, corrP.y)
            "$tpText $corrPText".also { mReport2!!.text = it }

            // report3
            "(${Forces.forces.x.toInt()} ${Forces.forces.y.toInt()})".also { mReport3!!.text = it }

            // report5
            val tPosIndex = (tPos mul VectorF(100f, 100f)).toIntVector()
            val repPos = "(%.2f %.2f)".format(tPos.x, tPos.y)
            val repCorTar = "(%.2f %.2f)".format(Forces.corrected.x, Forces.corrected.y)
            val corIndex = tPosIndex divideBy Forces.calibJump
            val corrections = Forces.corrections[corIndex.x][corIndex.y]
            val repCorrections = "(%.2f %.2f)".format(corrections.x, corrections.y)
            "$repPos  $repCorTar  $repCorrections".also { mReport5!!.text = it }
            "(target pos)  (corrected target)  (corrections)".also { mReport5a!!.text = it }
        }

        fun whileNotRunning() {
            // now we can read the dialog box for changing the IP address of the brunner interface
            if (mIPDialog!!.getText() != null) {
                val address = mIPDialog!!.getText().toString()
                if (validate(address)) {
                    ipAddress = InetAddress.getByName(address)
                    "Brunner ip $address".also { mReport0!!.text = it }
                } else
                    "INVALID ip address: $address".also { mReport0!!.text = it }
            }
            // we also set the force according to target position relative to the center of the pad
            // this wil not affect operation while running/polling
            // it is used to send that force to the stick, for testing purposes
            Forces.forces = (stickPad!!.target.pos minus VectorF(0.5f, 0.5f)) mul 1000f

        }
    }
}

@SuppressLint("StaticFieldLeak")
var sendy = PollMaster()
var recky = RecMaster()
val udpSender: UdpSender = UdpSender()



