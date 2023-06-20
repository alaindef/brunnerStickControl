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
            Forces.newParam(value, resources.getResourceEntryName(id).toString())
        }
    }
}