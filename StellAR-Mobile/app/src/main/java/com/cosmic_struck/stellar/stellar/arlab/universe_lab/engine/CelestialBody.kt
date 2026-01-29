package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

data class CelestialBody(
    val id: String,
    var position: Vector3D,
    var velocity: Vector3D,
    var acceleration: Vector3D,
    var mass: Float,
    var radius: Float,
    val name: String = ""
) {
    // Reset acceleration each frame (recalculated by forces)
    fun resetAcceleration() {
        acceleration = Vector3D(0f, 0f, 0f)
    }

    // Apply force using F = ma, so a = F/m
    fun applyForce(force: Vector3D) {
        if (mass > 0) {
            acceleration = acceleration + (force / mass)
        }
    }

    // Update position and velocity using Euler integration
    fun update(deltaTime: Float) {
        // v = v + a*dt
        velocity = velocity + (acceleration * deltaTime)
        // p = p + v*dt
        position = position + (velocity * deltaTime)
    }

    // Create a deep copy with new Vector3D instances for Compose state reactivity
    fun deepCopy(): CelestialBody = CelestialBody(
        id = id,
        position = Vector3D(position.x, position.y, position.z),
        velocity = Vector3D(velocity.x, velocity.y, velocity.z),
        acceleration = Vector3D(acceleration.x, acceleration.y, acceleration.z),
        mass = mass,
        radius = radius,
        name = name
    )
}