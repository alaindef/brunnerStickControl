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
        var teut = ""
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
                println("++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++")
                for (j in 0..50) {
                    teut = "row $j:"
                    for (i in 45..50) {
                        teut += " (${Forces.corrections[i][j].x} ${Forces.corrections[i][j].y})"
                    }
                    println(teut)
                }
            }
            "caltyp" -> {
                when (Forces.calType){
                    "full"      -> Forces.calType = "interpol"
                    "interpol"  -> Forces.calType = "none"
                    "none"      -> Forces.calType = "full"
                    else        -> Forces.calType = "none"
                }
                mCalibTypeButton!!.text = Forces.calType
            }
            "calmax" -> {
                when (Forces.calibMax){
                    4       -> Forces.calibMax = 5
                    5       -> Forces.calibMax = 10
                    10      -> Forces.calibMax = 20
                    20      -> Forces.calibMax = 50
                    50      -> Forces.calibMax = 100
                    100     -> Forces.calibMax = 4
                    else    -> Forces.calibMax = 5
                }
                Forces.calibMaxF = Forces.calibMax.toFloat()
                Forces.calibJump = 100 /  Forces.calibMax
                Forces.calibDelay = if (Forces.calibMax > 10) 1000 else 500
                mCalibMaxButton!!.text = Forces.calibMax.toString()
            }
            "interpol" -> {
                Forces.calType = "interpol"     //interpolate between calib points
            }
            "calibrate" -> {
                sendy.send(PollMaster.EV_7_calibratePos, 0, 0, view)
                view.setBackgroundColor(
                    ContextCompat.getColor(Main.mContext!!, R.color.buttonsecondcolor)
                )
//                Forces.calType = "none"        //no correction
//                mCalibTypeButton!!.text = "calibrate"
                Log.i(logTag, "calmax= ${Forces.calibMax}")
            }

            "resetCorrections" -> {
                Forces.resetCorrections()
                correctionView!!.resetCorY()
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
                val newText = s.toString()
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
            true
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
        } else {
        }
        if (sendy.isAlive) Log.e(logTag, "sendy lives") else Log.e(logTag, "sendy is dead")
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

        @SuppressLint("StaticFieldLeak")
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

        @SuppressLint("StaticFieldLeak")
        var mCalibTypeButton: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mCalibMaxButton: TextView? = null
    }
}

const val portR = 15095
var brunnerAddress = "192.168.0.203"

//adf230511 recky not needed (2 threads seems too much for android) var recky = RecMaster()
var sendy = PollMaster()
val udpSender: UdpSender = UdpSender(brunnerAddress, 15090)



