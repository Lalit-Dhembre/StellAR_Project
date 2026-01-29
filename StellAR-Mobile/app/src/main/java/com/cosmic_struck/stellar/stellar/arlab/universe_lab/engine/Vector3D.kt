package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

data class Vector3D(
    var x: Float = 0f,
    var y: Float = 0f,
    var z: Float = 0f
) {
    operator fun plus(other: Vector3D) = Vector3D(x + other.x, y + other.y, z + other.z)
    operator fun minus(other: Vector3D) = Vector3D(x - other.x, y - other.y, z - other.z)
    operator fun times(scalar: Float) = Vector3D(x * scalar, y * scalar, z * scalar)
    operator fun div(scalar: Float) = Vector3D(x / scalar, y / scalar, z / scalar)

    fun magnitude(): Float = kotlin.math.sqrt(x * x + y * y + z * z)
    fun normalize(): Vector3D {
        val mag = magnitude()
        return if (mag > 0) this / mag else this
    }
    fun dot(other: Vector3D): Float = x * other.x + y * other.y + z * other.z
    fun copy(): Vector3D = Vector3D(x, y, z)
    fun distance(other: Vector3D): Float = (this - other).magnitude()
}