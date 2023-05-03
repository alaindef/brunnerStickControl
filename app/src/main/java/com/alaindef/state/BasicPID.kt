package com.alaindef.state

import kotlin.math.max
import kotlin.math.min

class BasicPID(private var kP: Float, private var kI: Float, private var kD: Float) {
    private var lastError = 0f
    private var integral = 0f
    private var reversed = false

    fun calculate(error: Float, deltaTime: Float): Float {
//        adf
//        kP = sendy.conPv
        val proportional = kP * error
        integral += error * deltaTime
//        if (integral > 0) integral = min(integral, 30f) else integral = max(integral, 30f)
        integral += error * deltaTime
        val derivative = kD * (error - lastError) / deltaTime
        lastError = error
        return proportional + kI * integral + derivative
    }

    fun getOutput(current: Float, target: Float): Float {
        return calculate(target - current, sendy.delta_t.toFloat()/10f)
    }

    fun setP(p: Float) {
        kP = p
        checkSigns()
    }

    fun setI(i: Float) {
        kI = i
        checkSigns()
        // Implementation note:
        // This Scales the accumulated error to avoid output errors.
        // As an example doubling the I term cuts the accumulated error in half, which results in the
        // output change due to the I term constant during the transition.
    }

    fun setDirection(reversed: Boolean) {
        this.reversed = reversed
    }

    fun setOutputLimits(output: Float) {
//        setOutputLimits(-output, output)
    }

    fun setSetpoint(setpoint: Float) {
//        this.setpoint = setpoint
    }



    fun checkSigns(){
        if (reversed) {  // all values should be below zero
            if (kP > 0) kP *= -1f
            if (kI > 0) kI *= -1f
            if (kD > 0) kD *= -1f
//            if (kF > 0) kF *= -1f
        } else {  // all values should be above zero
            if (kP < 0) kP *= -1f
            if (kI < 0) kI *= -1f
            if (kD < 0) kD *= -1f
//            if (kF < 0) kF *= -1f
        }}

}
