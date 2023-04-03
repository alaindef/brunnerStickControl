package com.alaindef.puzzle

import android.content.Context
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import com.example.weather_app.R
import java.lang.Thread.sleep


@Suppress("unused")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"

    fun sendEvent(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_0, 0, 0, ss)
    }

    fun send0(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_0, 0, 0, ss)
        mReport!!.text = ss as CharSequence?
    }

    fun send1(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_1, 0, 0, ss)
        mReport!!.text = ss as CharSequence?
    }

    fun send2(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_2, 0, 0, ss)
        mReport!!.text = ss as CharSequence?
    }

    fun send3(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_3, 0, 0, ss)
        mReport!!.text = ss as CharSequence?
    }

    fun send4(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_4)
    }
    fun send5(view: View?) {
        var ss = view!!.tag
        oscar.send(FSM.EV_5)
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

//        adf 230404  https://wh0.github.io/2020/08/12/closeguard.html
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
            Configuration.ORIENTATION_LANDSCAPE -> setContentView(R.layout.mainland)
        }
        mainMailbox = MainMailbox()
        mContext = this.applicationContext
        mContextForDummies = this // found this, but why ???
        mReport = findViewById<View>(R.id.report) as TextView
        mtile1 = findViewById<View>(R.id.tile1) as AppCompatTextView
        mtile2 = findViewById<View>(R.id.tile2) as AppCompatTextView
        mtile3 = findViewById<View>(R.id.tile3) as AppCompatTextView
        val backgroundtileview = ImageView(this) //fixed background (green)
        backgroundtileview.setImageResource(R.drawable.fluosunroundedcorner)
//        mPuzzle = findViewById<View>(R.id.puzzleView) as MyPuzzleView
//        mPuzzle!!.init(currentSlide, backgroundtileview)

        if (savedInstanceState == null) oscar.start()
        else {
            oscar.interrupt()       // kill him!
            oscar = FSM()           // new one
            oscar.start()
        }

        Log.i(logTag, "bundle created ...................")

//        mPuzzle!!.post { mPuzzle!!.resetAndSortTiles() }
//        Toast.makeText(this, "from Main : $version", Toast.LENGTH_LONG).show()
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
        var oscar = FSM()

        @JvmField
        var mainMailbox: MainMailbox? = null    // a handler to extend UI event handling
        var mReport: TextView? = null           //pane to publish progress etc
        var mtile1: TextView? = null
        var mtile2: TextView? = null
        var mtile3: TextView? = null

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

        private const val RQ_PICK_PICTURE = 100
        private const val RQ_TAKE_PICTURE = 200
    }
}