package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

object GravityCalculator {
    private const val GRAVITATIONAL_CONSTANT = 6.674e-5f // Scaled for performance
    private const val MIN_DISTANCE = 1f // Prevent division by zero

    /**
     * Calculate gravitational force between two bodies
     * F = G * (m1 * m2) / r²
     */
    fun calculateGravitationalForce(
        body1: CelestialBody,
        body2: CelestialBody
    ): Vector3D {
        val direction = body2.position - body1.position
        val distance = direction.magnitude().coerceAtLeast(MIN_DISTANCE)

        val forceMagnitude = GRAVITATIONAL_CONSTANT *
                (body1.mass * body2.mass) / (distance * distance)

        return direction.normalize() * forceMagnitude
    }

    /**
     * Apply gravitational forces between all bodies (n-body problem)
     */
    fun updateGravitationalForces(bodies: List<CelestialBody>) {
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val force = calculateGravitationalForce(bodies[i], bodies[j])
                bodies[i].applyForce(force)
                bodies[j].applyForce(force * -1f) // Newton's third law
            }
        }
    }
}