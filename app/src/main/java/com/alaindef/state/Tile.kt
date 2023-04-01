/**
 * Created by alaindef on 30.07.15.
 */
package com.alaindef.puzzle

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import android.widget.FrameLayout
import android.view.Gravity
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.view.animation.Animation.AnimationListener

class Tile : AppCompatTextView {
    // tileIndex is permanently linked to the tile
    var tileIndex = 0
    private var newLeft = 0
    private var newTop = 0

    constructor(contextIn: Context?, tileIndex: Int) : super(contextIn!!, null, 0) {
        //        setScaleType(ImageView.ScaleType.FIT_XY); // was CENTER_CROP
        setPadding(0, 0, 0, 0)
        this.tileIndex = tileIndex //tileIndex is a fixed reference to a tile
        val layoutParams = FrameLayout.LayoutParams(100, 100)
        setLayoutParams(layoutParams)
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    )

    fun showTileIndex(s: String?) {
        text = s
        gravity = Gravity.CENTER
        val h = height
        //        Main.mainMailbox.send(MainMailbox.TEST, h, 0, null);
        val textSize = 80 * h / 360
        setTextSize(textSize.toFloat())
        setTextColor(Color.WHITE)
    }

    fun movNow(newX: Int, newY: Int) {
        this.left = newX
        this.top = newY
        visibility = VISIBLE
    }

    fun mov(newX: Int, newY: Int, animationLag: Int) {
        newLeft = newX
        newTop = newY
        val animation: Animation =
            TranslateAnimation(0F, (newX - left).toFloat(), 0F, (newY - top).toFloat())
        animation.duration = animationLag.toLong()
        animation.fillAfter = true //adf false or true does nothing
        animation.setAnimationListener(object : MyAnimationListener() {})
        startAnimation(animation) //150823 blocks here when in thread mode
        visibility = VISIBLE
    }

    private open inner class MyAnimationListener : AnimationListener {
        override fun onAnimationEnd(animation: Animation) {
            clearAnimation()
//            val tileSize = width
            val tileWidth = width
            val tileHeight = height
            val layoutParams = FrameLayout.LayoutParams(tileWidth, tileHeight)
            layoutParams.setMargins(newLeft, newTop, newLeft + tileWidth, newTop + tileHeight)
            setLayoutParams(layoutParams)
            if (tileIndex == 0) {             //single move
                Main.oscar.send(FSM.EV_MOVE_DONE) //inform oscar that next move can begin
            }
        }

        override fun onAnimationRepeat(animation: Animation) {}
        override fun onAnimationStart(animation: Animation) {}
    }
}