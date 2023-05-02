package com.alaindef.state

class PIDController(private val kP: Double, private val kI: Double, private val kD: Double) {
    private var lastError = 0.0
    private var integral = 0.0

    fun calculate(error: Double, deltaTime: Double): Double {
        val proportional = kP * error
        integral += error * deltaTime
        val derivative = kD * (error - lastError) / deltaTime
        lastError = error
        return proportional + kI * integral + derivative
    }
}
