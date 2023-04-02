package com.alaindef.puzzle

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.AssetManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.updateLayoutParams
import com.example.weather_app.R
import java.io.BufferedReader
import java.io.InputStreamReader

@Suppress("unused")
class Main : AppCompatActivity() {
    private val logTag = ">----MAIN---"
    private fun buttonsInit() {
        //--------------------------------------------------------------------------- PICTURE
        val slide = findViewById<View>(R.id.currentSlide) as ImageView
        slide.setOnLongClickListener {
            getTileFromGallery() //next picture
            true
        }
    }

    fun View?.layoutp() {mPuzzle!!.updateLayoutParams { height = 300 }}
    fun View?.shuffle() {oscar.send(FSM.EV_SHUFFLE, 0, 0, null)}
    fun View?.solve() {mPuzzle!!.solve()}
    fun View?.reset() {oscar.send(FSM.EV_RESET, 0, 0, null)}
    fun setSize(view: View?) {oscar.send(FSM.EV_SET_SIZE, 0, 0, view)}
    fun View?.show() {mPuzzle!!.showTileIndex()    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("message", "onSaveInstanceState: orientation changed")    //test of saveInstanceState
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            Log.i(logTag, "savedInstanceState: orientaton changed and strings=#strings")
        }

        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_PORTRAIT  -> setContentView(R.layout.mainportrait)
            Configuration.ORIENTATION_LANDSCAPE -> setContentView(R.layout.mainland)
        }
        mainMailbox = MainMailbox()
        mContext = this.applicationContext
        mContextForDummies = this // found this, but why ???
        mReport = findViewById<View>(R.id.report) as TextView
        val currentSlide: ImageView = findViewById<View>(R.id.currentSlide) as ImageView
        val backgroundtileview = ImageView(this) //fixed background (green)
        backgroundtileview.setImageResource(R.drawable.fluosunroundedcorner)
        mPuzzle = findViewById<View>(R.id.puzzleView) as MyPuzzleView
        mPuzzle!!.init(currentSlide, backgroundtileview)
        buttonsInit()
//        if (!oscar.isAlive) oscar.start()
//        adf 221022 this works better
        if (savedInstanceState == null) oscar.start()
        else {
            oscar.interrupt()       // kill him!
            oscar = FSM()           // new one
            oscar.start()
        }

        Log.i(logTag, "bundle created ...................")

        mPuzzle!!.post { mPuzzle!!.resetAndSortTiles() }
//        Toast.makeText(this, "from Main : $version", Toast.LENGTH_LONG).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == RQ_PICK_PICTURE) treatActionPick(resultCode, intent)
        else if (requestCode == RQ_TAKE_PICTURE) treatActionTake(resultCode, intent)
    }

    private fun treatActionPick(resultCode: Int, intent: Intent?) {
        if (resultCode == RESULT_OK) {
            val photoUri = intent!!.data
            if (photoUri != null) {
                try {
//                    see note 20150815 below
//                    20150815
//                    http://stackoverflow.com/questions/24135445/pre-guess-size-of-bitmap-from-the-actual-uri-before-scale-loading
//                    http://developer.android.com/training/displaying-bitmaps/load-bitmap.html
                    val options = BitmapFactory.Options()
                    val fileDescriptor = this.contentResolver.openAssetFileDescriptor(photoUri, "r")
                    options.inJustDecodeBounds = true // no memory allocation allowed
                    assert(fileDescriptor != null)
                    BitmapFactory.decodeFileDescriptor(
                        fileDescriptor!!.fileDescriptor, null, options
                    )
                    options.inJustDecodeBounds = false //
                    val reqWidth = mPuzzle!!.width
                    val reqHeight = mPuzzle!!.height
                    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
                    val zeBitmap = BitmapFactory.decodeFileDescriptor(
                        fileDescriptor.fileDescriptor, null, options
                    )
                    val bmd = BitmapDrawable(resources, zeBitmap)
//                    mPuzzle!!.setSlideAndHak(bmd)

                    mPuzzle!!.post{ mPuzzle!!.setSlideAndHak(bmd)}
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext,
                        "Image not available - try another one",
                        Toast.LENGTH_LONG
                    ).show()
                    Log.wtf(logTag, "bitmap failure catch $photoUri")
                }
            }
        }
    }

    fun View?.takePicture() {
        if (oscar.fState != 0) {
            Toast.makeText(this@Main, "busy, try later", Toast.LENGTH_LONG).show()
            return
        }
        // create Intent to take a picture and return control to the calling application
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // start the image capture Intent
        startActivityForResult(intent, RQ_TAKE_PICTURE) // 221023 do not follow AS
    }

    private fun treatActionTake(resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK) {
            val extras = data!!.extras
            val zeBitmap: Bitmap? = extras!!["data"] as Bitmap?  // 221023 do not follow AS
            val bmd = BitmapDrawable(resources, zeBitmap)
            mPuzzle!!.setSlideAndHak(bmd)
            mPuzzle!!.post{ mPuzzle.shuffle()}
        }
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
                    this, """
     try short press on slide at bottom
     try also long press $version
     """.trimIndent(), Toast.LENGTH_LONG
                ).show()
            R.id.action_settings ->
                Toast.makeText(
                    this, """
     Settings? for this???
     
     You must be kidding
     
     Hahahahahaaaa $version
     """.trimIndent(), Toast.LENGTH_LONG
                ).show()
            R.id.animation_lag_300 -> ANIMATION_LAG = 300
            R.id.animation_lag_500 -> ANIMATION_LAG = 500
            R.id.animation_lag_1000 -> ANIMATION_LAG = 1000
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

        var mPuzzle: MyPuzzleView? = null       //contains the puzzle

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
        private fun getTileFromGallery() {
            val intent = Intent()
            intent.type = "image/*"
            // intent.setAction(Intent.ACTION_GET_CONTENT);  //adf 151127: not OK anymore (pitctures not shown)
            intent.action = Intent.ACTION_PICK
            (mContextForDummies as Activity?)!!.startActivityForResult(
                Intent.createChooser(/* target = */ intent, /* title = */ "Select Picture"),
                this.RQ_PICK_PICTURE
            ) //ADF SELECT_PICTURE);
        }
    }
}