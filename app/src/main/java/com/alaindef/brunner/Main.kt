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
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.slider.Slider.OnChangeListener

data class Vector(var x: Float, var y: Float) {
    fun add(arg: Vector): Vector {
        return Vector(x + arg.x, y + arg.y)
    }

    fun times(arg: Float): Vector {
        return Vector(x * arg, y * arg)
    }

    fun divide(arg: Float): Vector {
        return Vector(x / arg, y / arg)
    }

    fun toIntVector(): IntVector {
        return IntVector(x.toInt(), y.toInt())
    }
}

data class IntVector(val x: Int, val y: Int) {
    fun max(arg: IntVector): IntVector {
        return IntVector(kotlin.math.max(x, arg.x), kotlin.math.max(y, arg.y))
    }
}

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
            "calibrate" -> {
                sendy.send(PollMaster.EV_23_calibrate, 0, 0, ss)
            }
            "resetCorY" -> {
                correctionView!!.resetCorY()
//                sendy.send(PollMaster.EV_22_resetCorY, 0, 0, ss)
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
                sendy.send(PollMaster.EV_9_new_IP)
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
//        mCircleView = CircleView(this, null)
//        mPad!!.setImageDrawable(mCircleView!!.background)
//        mCircleView!!.visibility = View.GONE

        mReset = findViewById<View>(R.id.reset) as Button
        mReset?.setOnClickListener {
            sendy.send(PollMaster.EV_0_reset)
            true
        }
        mReset?.setOnLongClickListener {
            sendy.send(PollMaster.EV_1_full_reset)
            true
        }

        conPBar = findViewById(R.id.conP)
        conPReport = findViewById(R.id.conPreport)
        conPBar?.addOnChangeListener(object : OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                Forces.conP = value
                conPReport!!.text = value.toInt().toString()
            }
        })


        conIBar = findViewById(R.id.conI)
        conIReport = findViewById(R.id.conIreport)
        conIBar?.addOnChangeListener(object : OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                Forces.conI = value
                conIReport!!.text = value.toInt().toString()
            }
        })

        conPvBar = findViewById(R.id.conPv)
        conPvReport = findViewById(R.id.conPvreport)
        conPvBar?.addOnChangeListener(object : OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                Forces.conPv = value
                conPvReport!!.text = value.toInt().toString()
            }
        })


        conIvBar = findViewById(R.id.conIv)
        conIvReport = findViewById(R.id.conIvreport)
        conIvBar?.addOnChangeListener(object : OnChangeListener {
            override fun onValueChange(slider: Slider, value: Float, fromUser: Boolean) {
                Forces.conIv = value
                conIvReport!!.text = value.toInt().toString()
            }
        })

        correctionView = findViewById(R.id.yTable)
        mPad = findViewById<View>(R.id.pad) as AppCompatImageView?
        delete1 = findViewById(R.id.del)

        delete1!!.teut

        if (savedInstanceState == null) {
            // sendy is an FMM (Finite Message Machine) that handles all the incomming events:
            // start and stop the polling, reset, receive target and current positions,
            // request to change the IP address of the brunner interface, change polling intervals
            sendy.start()
//adf230511            recky.start()
        } else {
        }
//adf230511        if (recky.isAlive) Log.e(logTag, "oscar lives") else Log.e(logTag, "recky is dead")

        if (sendy.isAlive) Log.e(logTag, "omer lives") else Log.e(logTag, "sendy is dead")
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
        var conPBar: Slider? = null

        @SuppressLint("StaticFieldLeak")
        var conPReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var conIBar: Slider? = null

        @SuppressLint("StaticFieldLeak")
        var conIReport: TextView? = null


        @SuppressLint("StaticFieldLeak")
        var conPvBar: Slider? = null

        @SuppressLint("StaticFieldLeak")
        var conPvReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var conIvBar: Slider? = null

        @SuppressLint("StaticFieldLeak")
        var conIvReport: TextView? = null
//
//        @SuppressLint("StaticFieldLeak")
//        var mCircleView: CircleView? = null

        @SuppressLint("StaticFieldLeak")
        var mPad: AppCompatImageView? = null

        @SuppressLint("StaticFieldLeak")
        var correctionView: PolylineView? = null


        @SuppressLint("StaticFieldLeak")
        var delete1: delete? = null

    }
}

private val PadView.teut: Unit
    get() {}
const val portR = 15095
var brunnerAddress = "192.168.0.203"

//adf230511 recky not needed (2 threads seems too much for android) var recky = RecMaster()
var sendy = PollMaster()
val udpSender: UdpSender = UdpSender(brunnerAddress, 15090)



