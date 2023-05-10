package com.alaindef.brunner

import java.util.LinkedList
import java.util.Queue

class BasicPID(private var kP: Float, private var kI: Float, private var kD: Float) {
    private var lastError = 0f
    private var integral = 0f
    private var reversed = false
    private val queue: Queue<Float> = LinkedList(listOf())
    private val maxQsize = 3


    fun calculate(error: Float, deltaTime: Float): Float {
        val proportional = kP * error

        val quadratic    = kP * error * error /10f

        val sum = queue.fold(0f) { acc, i -> acc + i }
        queue.add(error)
        if (queue.size > maxQsize) queue.remove()

//        integral = sum * deltaTime
        integral += error * deltaTime
        val derivative = kD * (error - lastError) / deltaTime
        lastError = error
        return proportional + kI * integral + derivative  +quadratic
    }

    fun getOutput(current: Float, target: Float): Float {
        return calculate(target - current, sendy.delta_t.toFloat() / 10f)
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


    fun checkSigns() {
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
        }
    }

}
