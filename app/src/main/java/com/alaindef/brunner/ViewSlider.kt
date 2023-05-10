package com.alaindef.brunner

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import com.google.android.material.slider.Slider

class ViewSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : Slider(context, attrs, defStyleAttr) {

    init {
        addOnChangeListener { slider: Slider, value, fromUser: Boolean ->
            sendy.send(PollMaster.EV_21_from_slider, slider.value.toInt(), 0, resources.getResourceEntryName(id))
        }
    }
}