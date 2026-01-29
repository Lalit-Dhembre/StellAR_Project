package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

class UniverseSimulator {
    private val engine = PhysicsEngine()

    // Create a simple solar system for testing
    fun createSolarSystem() {
        engine.reset()

        // Sun (stationary at origin)
        engine.addBody(CelestialBody(
            id = "sun",
            position = Vector3D(0f, 0f, 0f),
            velocity = Vector3D(0f, 0f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 1989f, // kg (scaled)
            radius = 69.6f,
            name = "Sun"
        ))

        // Earth (orbiting sun)
        engine.addBody(CelestialBody(
            id = "earth",
            position = Vector3D(149.6f, 0f, 0f),
            velocity = Vector3D(0f, 29.78f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 5.972f,
            radius = 6.371f,
            name = "Earth"
        ))

        // Moon (orbiting earth)
        engine.addBody(CelestialBody(
            id = "moon",
            position = Vector3D(149.6f + 0.384f, 0f, 0f),
            velocity = Vector3D(0f, 29.78f + 1.022f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 0.07342f,
            radius = 1.737f,
            name = "Moon"
        ))

        // Mars (orbiting sun)
        engine.addBody(CelestialBody(
            id = "mars",
            position = Vector3D(227.9f, 0f, 0f),
            velocity = Vector3D(0f, 24.077f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 0.642f,
            radius = 3.389f,
            name = "Mars"
        ))

        // Jupiter (orbiting sun)
        engine.addBody(CelestialBody(
            id = "jupiter",
            position = Vector3D(778.5f, 0f, 0f),
            velocity = Vector3D(0f, 13.07f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 1898f,
            radius = 69.911f,
            name = "Jupiter"
        ))

        // Saturn (orbiting sun)
        engine.addBody(CelestialBody(
            id = "saturn",
            position = Vector3D(1433.5f, 0f, 0f),
            velocity = Vector3D(0f, 9.69f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 568.3f,
            radius = 58.232f,
            name = "Saturn"
        ))
    }

    fun step() = engine.step()
    fun stepMultiple(steps: Int) = engine.stepMultiple(steps)
    
    // Step simulation with per-body speed multipliers
    fun stepWithMultipliers(multipliers: Map<String, Float>) {
        engine.stepWithMultipliers(multipliers)
    }
    
    fun getEngine(): PhysicsEngine = engine
    fun getBodies(): List<CelestialBody> = engine.getBodies()
}