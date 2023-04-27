package com.alaindef.state

//adf
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.lang.Integer.max
import java.lang.Integer.min
import kotlin.math.roundToInt


@Suppress("unused")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"
    fun sendEvent(view: View?) {
        when (val ss = view!!.tag) {
            "B_poll" -> {
                omer.send(PollMaster.EV_2_start_stop)
//                omer.send(PollMaster.EV_2_start_stop)
            }
            "B_RES" -> {
//                omer.send(PollMaster.EV_0, 0, 0, ss)  //handled by onclick listener
//                because long press is also used
            }
            "B_4" -> {
                omer.send(PollMaster.EV_11_dt_min, 0, 0, ss)
            }
            "B_5" -> {
                omer.send(PollMaster.EV_12_dt_plus, 0, 0, ss)
            }
            "B_6" -> {
                omer.send(PollMaster.EV_13_force_min, 0, 0, ss)
            }
            "B_7" -> {
                omer.send(PollMaster.EV_14_force_plus, 0, 0, ss)
            }
            else -> Log.wtf(logTag, "tag unknown $ss")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mReport = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            "message",
            "onSaveInstanceState: orientation changed"
        )    //test of saveInstanceState
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
            Configuration.ORIENTATION_PORTRAIT -> setContentView(R.layout.mainportraitsimple)
//            Configuration.ORIENTATION_LANDSCAPE -> setContentView(R.layout.mainland)
        }

        mContext = this.applicationContext
        mContextForDummies = this // found this, but why ???
        mReport = findViewById<View>(R.id.report) as TextView
        mReport1 = findViewById<View>(R.id.report1) as TextView
        mReport2 = findViewById<View>(R.id.report2) as TextView
        mReport3 = findViewById<View>(R.id.report3) as TextView
        mReport4 = findViewById<View>(R.id.report4) as TextView
        mReport4b = findViewById<View>(R.id.report4b) as TextView
        mReport5 = findViewById<View>(R.id.report5) as TextView
        mReport5b = findViewById<View>(R.id.report5b) as TextView

        mReset = findViewById<View>(R.id.reset) as Button
        mReset?.setOnClickListener {
            omer.send(PollMaster.EV_0_reset)
            true
        }
        mReset?.setOnLongClickListener {
            omer.send(PollMaster.EV_1_full_reset)
            true
        }


//adf
        seekBar = findViewById(R.id.seek)

        // Set an event listener for the SeekBar
        seekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Do something when the SeekBar value changes
                mReport4!!.text = "seekbar:"
                mReport4b!!.text = "$progress"
                omer.alfa = progress
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do something when the user starts touching the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do something when the user stops touching the SeekBar
            }
        })

        var pad: ImageView? = null
        pad = findViewById(R.id.pad)

        data class Vector(val x: Float, val y: Float)

        fun useTargetXY(pad: ImageView, x: Float, y: Float): Vector {
            val padWidth = pad.width.toFloat()
            val padHeight = pad.height
            val xRel = (minOf(maxOf(((x * 100F) / padWidth), 0F), 100F))/100f
            val yRel = (minOf(maxOf(((y * 100F) / padHeight), 0F), 100F))/100f

            omer.xTarget = xRel
            omer.yTarget = yRel
            omer.send(PollMaster.EV_4_target_pos)
            mReport2!!.text =
                "TARGET: ${String.format("%.${2}f", xRel)}  ${String.format("%.${2}f", yRel)})"
            return Vector(xRel, yRel)
        }

        pad?.setOnTouchListener { _, event ->
//            we use the finger position on the pad as target position of the stick
            when (event?.action) {
                MotionEvent.ACTION_MOVE -> {
                    val xT = event.x            // event.x is Float
                    val yT = event.y
                    useTargetXY(pad, xT, yT)
                }
                MotionEvent.ACTION_DOWN -> {
                    val xT = event.x
                    val yT = event.y
                    useTargetXY(pad, xT, yT)
                }
            }
            true   //not  return v?.onTouchEvent(event) ?: true
        }

        val backgroundtileview = ImageView(this) //fixed background (green)
        backgroundtileview.setImageResource(R.drawable.fluosunroundedcorner)

        if (savedInstanceState == null) {
            oscar.start()
            omer.start()
        } else {
        }

        if (oscar.isAlive) Log.e(logTag, "oscar lives")
        else Log.e(logTag, "oscar is dead")

        if (omer.isAlive) Log.e(logTag, "omer lives")
        else Log.e(logTag, "omer is dead")
    }

    companion object {
        const val version = 43
        const val DIM: Float = 0.5f // for Alpha, between 0 and 1 for textview

        // between 0 and 255 for imageview
        @JvmField
        var ANIMATION_LAG = 300

        @SuppressLint("StaticFieldLeak")
        var mReport: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReset: Button? = null

        @SuppressLint("StaticFieldLeak")
        var mReport1: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport2: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport3: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport4: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport4b: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport5: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport5b: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mReport6: TextView? = null

        @SuppressLint("StaticFieldLeak")
        var mContext: Context? = null

        @SuppressLint("StaticFieldLeak")
        var mContextForDummies: Context? = null

        //adf
        @SuppressLint("StaticFieldLeak")
        var seekBar: SeekBar? = null

    }
}

@JvmField
val portR = 15095

var oscar = RecMaster()
var omer = PollMaster()
val udpSender: UdpSender = UdpSender("192.168.0.203", 15090)
val udpReceiver: UdpReceiver = UdpReceiver(portR)

