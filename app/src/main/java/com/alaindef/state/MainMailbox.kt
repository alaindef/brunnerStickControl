package com.alaindef.puzzle

import android.os.Handler
import android.os.Message
import android.util.Log
import android.widget.TextView
import com.example.weather_app.R

/**
 * Created by alaindef on 16.09.15.
 * changed
 */
class MainMailbox  //   messages
    : Handler() {
    //    private final WeakReference<Main> currentActivity;
    private var timerep = 0
    override fun handleMessage(m: Message) {
        val logTAG = "---MAIN ---"
        when (m.what) {
            TILE_CLICK -> Log.wtf(logTAG, "...tile click..." + m.arg1)
//            TILE_CLICK -> Main.mPuzzle?.swap0(m.obj as Tile, true)
            PLAY_NEXT_MOVE -> {
                Main.mReport!!.text =
                    String.format("time: %d msec - step %d of %d", timerep, m.arg1, m.arg2)
            }
            RESET -> Log.wtf(logTAG, "...reset..." + m.arg1)
            SHUFFLE -> {
                Main.mReport!!.text =
                    String.format("time: %d msec", 7777)
                Log.wtf(logTAG, "...shuffle..." + m.arg1)
            }
            REPORT_HINT_Q -> Main.mReport?.text =
                Main.mContext?.resources?.getString(R.string.hint)
            SET_SIZE -> Log.wtf(logTAG, "...set size..." + m.arg1)
            REPORT_ELAPSED_TIME -> {timerep = m.arg1
                Main.mReport!!.text ="report " + m.arg1 + " " + m.obj}
            TEST -> Log.wtf(logTAG, "arg1= " + m.arg1)
            else -> Log.wtf(logTAG, "message unknown " + m.what + "/" + m.arg1)
        }
    }

    fun send(what: Int, arg1: Int, arg2: Int, obj: Any?) {
        sendMessage(obtainMessage(what, arg1, arg2, obj))
    }

    fun send(what: Int) {
        sendMessage(obtainMessage(what, 0, 0, null)) // code inspection problem
    }

    companion object {
        //    messages
        const val TILE_CLICK = 0
        const val PLAY_NEXT_MOVE = 1
        const val RESET = 2
        const val SHUFFLE = 3
        const val REPORT_HINT_Q = 4
        const val SET_SIZE = 5
        const val REPORT_ELAPSED_TIME = 6
        const val TEST = 7
    }
}