package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

class PhysicsEngine(
    private var timeScale: Float = 1f,
    private var timeStep: Float = 0.016f // ~60 FPS
) {
    private val bodies = mutableListOf<CelestialBody>()
    private var simulationTime = 0f

    // Add a celestial body to the simulation
    fun addBody(body: CelestialBody) {
        bodies.add(body)
    }

    // Remove a body from simulation
    fun removeBody(bodyId: String) {
        bodies.removeAll { it.id == bodyId }
    }

    // Get a body by ID
    fun getBody(bodyId: String): CelestialBody? = bodies.find { it.id == bodyId }

    // Get all bodies (deep copy for Compose state reactivity)
    fun getBodies(): List<CelestialBody> = bodies.map { it.deepCopy() }

    // Set time scale for faster/slower simulation (0.5 = half speed, 2.0 = double speed)
    fun setTimeScale(scale: Float) {
        timeScale = scale.coerceIn(0.01f, 10f)
    }

    // Modify a body's properties
    fun modifyBody(
        bodyId: String,
        newPosition: Vector3D? = null,
        newVelocity: Vector3D? = null,
        newMass: Float? = null,
        newRadius: Float? = null
    ) {
        val body = getBody(bodyId) ?: return
        newPosition?.let { body.position = it }
        newVelocity?.let { body.velocity = it }
        newMass?.let { if (it > 0) body.mass = it }
        newRadius?.let { if (it > 0) body.radius = it }
    }

    // Main simulation step
    fun step() {
        val dt = timeStep * timeScale

        // Reset accelerations
        bodies.forEach { it.resetAcceleration() }

        // Apply gravitational forces
        GravityCalculator.updateGravitationalForces(bodies)

        // Check for collisions and resolve them
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                if (CollisionDetector.areColliding(bodies[i], bodies[j])) {
                    CollisionDetector.handleCollision(bodies[i], bodies[j])
                }
            }
        }

        // Update positions and velocities
        bodies.forEach { it.update(dt) }

        simulationTime += dt
    }

    // Run simulation for multiple steps
    fun stepMultiple(steps: Int) {
        repeat(steps) { step() }
    }

    // Step simulation with per-body speed multipliers
    fun stepWithMultipliers(multipliers: Map<String, Float>) {
        val dt = timeStep * timeScale

        // Reset accelerations
        bodies.forEach { it.resetAcceleration() }

        // Apply gravitational forces
        GravityCalculator.updateGravitationalForces(bodies)

        // Check for collisions and resolve them
        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                if (CollisionDetector.areColliding(bodies[i], bodies[j])) {
                    CollisionDetector.handleCollision(bodies[i], bodies[j])
                }
            }
        }

        // Update positions and velocities with per-body multipliers
        bodies.forEach { body ->
            val multiplier = multipliers[body.id] ?: 1f
            body.update(dt * multiplier)
        }

        simulationTime += dt
    }

    // Get simulation time
    fun getSimulationTime(): Float = simulationTime

    // Reset simulation
    fun reset() {
        bodies.clear()
        simulationTime = 0f
    }

    // Calculate total energy in system (for validation)
    fun getTotalEnergy(): Float {
        var kineticEnergy = 0f
        var potentialEnergy = 0f

        for (body in bodies) {
            // KE = 0.5 * m * v²
            kineticEnergy += 0.5f * body.mass * body.velocity.magnitude() * body.velocity.magnitude()
        }

        for (i in bodies.indices) {
            for (j in i + 1 until bodies.size) {
                val distance = bodies[i].position.distance(bodies[j].position)
                if (distance > 0) {
                    // PE = -G * m1 * m2 / r (simplified)
                    potentialEnergy -= (bodies[i].mass * bodies[j].mass) / distance
                }
            }
        }

        return kineticEnergy + potentialEnergy
    }
}