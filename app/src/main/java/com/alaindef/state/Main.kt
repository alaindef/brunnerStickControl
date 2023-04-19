package com.alaindef.state

//adf
import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.opengl.ETC1.getWidth
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import java.io.IOException
import java.lang.Integer.max
import java.lang.Integer.min
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.math.roundToInt


class SoftOptions {
    var RemoteHost: String = "192.168.1.255"
    var RemotePort: Int = 6454

    constructor()

    init {}
}

// Global
val Settings = SoftOptions()


@Suppress("unused")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"

    fun sendEvent(view: View?) {
        when (val ss = view!!.tag) {
            "B_poll" -> {
                omer.send(PollMaster.EV_3_PR, 0, 0, ss)
//                omer.send(PollMaster2.ev_poll$_and_repeat, 0, 0, ss)
//                oscar.send(FSM.EV_5, 0, 0, ss)
                mReport1!!.text = ("sendEvent: ss") as CharSequence?
            }
            "B_RES" -> {
                omer.send(PollMaster.EV_0, 0, 0, ss)
                mReport1!!.text = ("sendEvent: $ss") as CharSequence?
            }
            "B_4" -> {
                omer.send(PollMaster.EV_4, 0, 0, ss)
                mReport1!!.text = ("sendEvent: $ss") as CharSequence?
            }
            "B_5" -> {
                omer.send(PollMaster.EV_5, 0, 0, ss)
                mReport1!!.text = ("sendEvent: $ss") as CharSequence?
            }
            "B_6" -> {
                omer.send(PollMaster.EV_6, 0, 0, ss)
                mReport3!!.text = ("from button $ss") as CharSequence?
            }
            "B_7" -> {
                omer.send(PollMaster.EV_7, 0, 0, ss)
                mReport3!!.text = ("from Button $ss") as CharSequence?
            }
            else -> Log.wtf(logTag, "tag unknown $ss")
        }
//        oscar.send(FSM.EV_0, 0, 0, ss)
    }


    fun View?.show() {}


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(
            "message",
            "onSaveInstanceState: orientation changed"
        )    //test of saveInstanceState
    }

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

        mainMailbox = MainMailbox()
        mContext = this.applicationContext
        mContextForDummies = this // found this, but why ???
        mReport = findViewById<View>(R.id.report) as TextView
        mReport1 = findViewById<View>(R.id.report1) as TextView
        mReport2 = findViewById<View>(R.id.report2) as TextView
        mReport3 = findViewById<View>(R.id.report3) as TextView
        mtile1 = findViewById<View>(R.id.tile1) as AppCompatTextView
        mtile2 = findViewById<View>(R.id.tile2) as AppCompatTextView
        mtile3 = findViewById<View>(R.id.tile3) as AppCompatTextView

//adf
        seekBar = findViewById(R.id.seek)

        // Set an event listener for the SeekBar
        com.alaindef.state.Main.Companion.seekBar?.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Do something when the SeekBar value changes
                mReport!!.text = progress.toString()
                omer.forceX = (50 - progress) * 10
//                omer.send(PollMaster.EV_2_Ext, progress, 0, null)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                // Do something when the user starts touching the SeekBar
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Do something when the user stops touching the SeekBar
            }
        })


//adf

        var pad: ImageView? = null
        pad = findViewById(R.id.pad)

//        pad?.setOnTouchListener(object : View.OnTouchListener {
//            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
//                when (event?.action) {
//                    MotionEvent.ACTION_DOWN -> {
//                        val x = event.x
//                        val y = event.y
//                        mReport!!.text = " touch ${x.toString()} touch ${x.toString()}"
//                    }
//                }
//
//                return v?.onTouchEvent(event) ?: true
//            }
//        })
        pad?.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                when (event?.action) {
                    MotionEvent.ACTION_MOVE -> {
                        Log.d(logTag, "moving ${event.x}")
                        val x = event.x
                        val y = event.y
                        val padWidth = pad.width
                        val padHeight = pad.height
                        val xRel = min(max(((x *100) /padWidth).roundToInt(),0),100)
                        val yRel = min(max(((y *100) /padHeight).roundToInt(),0),100)
                        omer.forceX = (50 - xRel) * 10
                        omer.forceY = (50 - yRel) * 30
                        mReport2!!.text = " move ${x.toString()}  ${y.toString()} xrel ${xRel.toString()} yrel ${yRel.toString()}"
                    }
                }

                return true   //not  return v?.onTouchEvent(event) ?: true
            }
        })


        val backgroundtileview = ImageView(this) //fixed background (green)
        backgroundtileview.setImageResource(R.drawable.fluosunroundedcorner)

        if (savedInstanceState == null) {
            oscar.start()
            omer.start()
        } else {
            oscar.interrupt()       // kill him!
            oscar = FSM()           // new one
            oscar.start()
        }

        Log.i(logTag, "bundle created ...................")
//        omer.send(0)                //crash!
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.action_help ->
                Toast.makeText(
                    this, """$version""".trimIndent(), Toast.LENGTH_LONG
                ).show()
            R.id.action_settings ->
                Toast.makeText(
                    this, """Hahahahahaaaa""".trimIndent(), Toast.LENGTH_LONG
                ).show()
            R.id.animation_lag_300 -> ANIMATION_LAG = 300
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val version = 43
        const val DIM: Float = 0.5f // for Alpha, between 0 and 1 for textview

        // between 0 and 255 for imageview
        @JvmField
        var ANIMATION_LAG = 300

        @JvmField


        var mainMailbox: MainMailbox? = null    // a handler to extend UI event handling
        var mReport: TextView? = null           //pane to publish progress etc
        var mReport1: TextView? = null           //pane to publish progress etc
        var mReport2: TextView? = null           //pane to publish progress etc
        var mReport3: TextView? = null           //pane to publish progress etc
        var mtile1: TextView? = null
        var mtile2: TextView? = null
        var mtile3: TextView? = null

        //adf
        var seekBar: SeekBar? = null

//        var mPuzzle: MyPuzzleView? = null       //contains the puzzle

        @JvmField
        var mContext: Context? = null
        var mContextForDummies: Context? = null

        private fun calculateInSampleSize(
            options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int
        ): Int {
            // Raw height and width of image
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1
            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2
                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }

        fun sendUDP(messageStr: String) {
            // Hack Prevent crash (sending should be done using an async task)
            val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
            try {
                //Open a port to send the package
                val socket = DatagramSocket()
                socket.broadcast = true
                val sendData = byteArrayOf(175.toByte(), 101, 108, 108, 111)
//                val sendData = byteArrayOf('<Iiiiiiiii', 0xAE, 101, 108, 108, 111)
//                val sendData = messageStr.toByteArray()
                val sendPacket = DatagramPacket(
                    sendData,
                    sendData.size,
                    InetAddress.getByName(Settings.RemoteHost),
                    Settings.RemotePort
                )
                socket.send(sendPacket)
                println("fun sendBroadcast: packet sent to: " + InetAddress.getByName(Settings.RemoteHost) + ":" + Settings.RemotePort)
            } catch (e: IOException) {
                //            Log.e(FragmentActivity.TAG, "IOException: " + e.message)
            }
        }


        private const val RQ_PICK_PICTURE = 100
        private const val RQ_TAKE_PICTURE = 200
    }
}

@JvmField
var oscar = FSM()
var omer = PollMaster()
val udpSender: UdpSender = UdpSender("192.168.0.203", 15090)

