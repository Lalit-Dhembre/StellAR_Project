package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

object CollisionDetector {
    /**
     * Check if two bodies are colliding (simple sphere collision)
     */
    fun areColliding(body1: CelestialBody, body2: CelestialBody): Boolean {
        val distance = body1.position.distance(body2.position)
        return distance < (body1.radius + body2.radius)
    }

    /**
     * Handle elastic collision between two bodies
     * Transfers momentum between them
     */
    fun handleCollision(body1: CelestialBody, body2: CelestialBody) {
        val collisionNormal = (body2.position - body1.position).normalize()

        // Relative velocity
        val relativeVelocity = body1.velocity - body2.velocity
        val velocityAlongNormal = relativeVelocity.dot(collisionNormal)

        // Don't resolve if velocities are separating
        if (velocityAlongNormal < 0) return

        // Coefficient of restitution (0 = inelastic, 1 = perfectly elastic)
        val restitution = 0.5f

        // Impulse scalar
        val totalMass = body1.mass + body2.mass
        val j = -(1 + restitution) * velocityAlongNormal / totalMass

        // Apply impulse
        val impulse = collisionNormal * j
        body1.velocity = body1.velocity + (impulse * body1.mass)
        body2.velocity = body2.velocity - (impulse * body2.mass)
    }
}