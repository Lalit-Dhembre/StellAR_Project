package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

import kotlin.math.abs

// ==================== Console Simulator Main App ====================
class ConsoleSimulator {

    fun main() {
        println("╔════════════════════════════════════════════════════════════╗")
        println("║       UNIVERSE PHYSICS ENGINE - CONSOLE SIMULATOR          ║")
        println("╚════════════════════════════════════════════════════════════╝")
        println()

        // Choose a test scenario
        println("Select test scenario:")
        println("1. Solar System Simulation")
        println("2. Two Body Gravity Test")
        println("3. Collision Test")
        println("4. Energy Conservation Test")
        println("5. Interactive Universe")
        println()

        // For console app, we'll run all tests automatically
        testSolarSystem()
        println("\n" + "─".repeat(60) + "\n")
        testTwoBodyGravity()
        println("\n" + "─".repeat(60) + "\n")
        testCollision()
        println("\n" + "─".repeat(60) + "\n")
        testEnergyConservation()
        println("\n" + "─".repeat(60) + "\n")
        testCustomUniverse()
    }

    // ==================== Test: Solar System ====================
    fun testSolarSystem() {
        println("TEST 1: SOLAR SYSTEM SIMULATION")
        println("Creating Sun, Earth, and Moon...")
        println()

        val universe = UniverseSimulator()
        universe.createSolarSystem()

        val engine = universe.getEngine()
        printBodiesInfo(engine.getBodies(), "Initial State")

        println("\nRunning simulation for 100 steps (simulating ~1.6 seconds)...")
        repeat(100) {
            engine.step()
        }

        printBodiesInfo(engine.getBodies(), "After 100 Steps")

        println("\nObservations:")
        println("✓ Earth should be orbiting the Sun")
        println("✓ Moon should be orbiting Earth while following its orbit")
        println("✓ Total energy should be roughly conserved")
        val energy = engine.getTotalEnergy()
        println("✓ Total Energy: ${String.format("%.2f", energy)}")
    }

    // ==================== Test: Two Body Gravity ====================
    fun testTwoBodyGravity() {
        println("TEST 2: TWO BODY GRAVITY TEST")
        println("Creating two equal-mass bodies 50 units apart...")
        println()

        val engine = PhysicsEngine()

        val body1 = CelestialBody(
            id = "obj1",
            position = Vector3D(-25f, 0f, 0f),
            velocity = Vector3D(0f, 0f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 100f,
            radius = 2f,
            name = "Object 1"
        )

        val body2 = CelestialBody(
            id = "obj2",
            position = Vector3D(25f, 0f, 0f),
            velocity = Vector3D(0f, 0f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 100f,
            radius = 2f,
            name = "Object 2"
        )

        engine.addBody(body1)
        engine.addBody(body2)

        val initialDistance = body1.position.distance(body2.position)
        println("Initial distance: ${String.format("%.2f", initialDistance)} units")
        printBodiesInfo(engine.getBodies(), "Step 0")

        println("\nSimulating gravitational attraction for 200 steps...")
        for (step in 1..200) {
            engine.step()

            if (step % 50 == 0) {
                val distance = body1.position.distance(body2.position)
                println("Step $step: Distance = ${String.format("%.2f", distance)} units")
                printBodiesInfo(engine.getBodies(), "Step $step")
            }
        }

        val finalDistance = body1.position.distance(body2.position)
        println("\nFinal distance: ${String.format("%.2f", finalDistance)} units")
        println(
            "Distance reduction: ${
                String.format(
                    "%.2f",
                    initialDistance - finalDistance
                )
            } units"
        )
        println("\nObservations:")
        println("✓ Bodies should attract each other and get closer")
        println("✓ Velocities should increase as they approach")
        println("✓ They should eventually collide or orbit around common center")
    }

    // ==================== Test: Collision ====================
    fun testCollision() {
        println("TEST 3: COLLISION TEST")
        println("Creating two bodies on collision course...")
        println()

        val engine = PhysicsEngine()

        val body1 = CelestialBody(
            id = "projectile",
            position = Vector3D(0f, 0f, 0f),
            velocity = Vector3D(10f, 0f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 10f,
            radius = 2f,
            name = "Projectile"
        )

        val body2 = CelestialBody(
            id = "target",
            position = Vector3D(50f, 0f, 0f),
            velocity = Vector3D(0f, 0f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 20f,
            radius = 3f,
            name = "Target"
        )

        engine.addBody(body1)
        engine.addBody(body2)

        printBodiesInfo(engine.getBodies(), "Before Collision")

        var collisionOccurred = false
        var collisionStep = 0

        println("\nSimulating until collision...")
        for (step in 1..500) {
            engine.step()

            val distance = body1.position.distance(body2.position)
            val minDistance = body1.radius + body2.radius

            if (distance < minDistance && !collisionOccurred) {
                collisionOccurred = true
                collisionStep = step
                println("💥 COLLISION DETECTED at step $step!")
                printBodiesInfo(engine.getBodies(), "At Collision")
            }

            if (collisionOccurred && step == collisionStep + 10) {
                println("\n10 steps after collision:")
                printBodiesInfo(engine.getBodies(), "10 Steps After Collision")
                break
            }
        }

        if (!collisionOccurred) {
            println("⚠ No collision occurred")
        }

        println("\nObservations:")
        println("✓ Projectile should move towards target")
        println("✓ Collision should be detected when bodies overlap")
        println("✓ Velocities should change after collision (momentum transfer)")
    }

    // ==================== Test: Energy Conservation ====================
    fun testEnergyConservation() {
        println("TEST 4: ENERGY CONSERVATION TEST")
        println("Monitoring total energy in an isolated system...")
        println()

        val engine = PhysicsEngine()

        val body1 = CelestialBody(
            id = "body1",
            position = Vector3D(-10f, 0f, 0f),
            velocity = Vector3D(0f, 5f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 50f,
            radius = 1f,
            name = "Body 1"
        )

        val body2 = CelestialBody(
            id = "body2",
            position = Vector3D(10f, 0f, 0f),
            velocity = Vector3D(0f, -5f, 0f),
            acceleration = Vector3D(0f, 0f, 0f),
            mass = 50f,
            radius = 1f,
            name = "Body 2"
        )

        engine.addBody(body1)
        engine.addBody(body2)

        val initialEnergy = engine.getTotalEnergy()
        println("Initial Total Energy: ${String.format("%.2f", initialEnergy)}")

        println("\nStep | Total Energy | Kinetic Energy | Potential Energy | Error %")
        println("─".repeat(70))

        for (step in 0..500 step 50) {
            repeat(50) { engine.step() }

            val currentEnergy = engine.getTotalEnergy()
            val energyError = abs(currentEnergy - initialEnergy) / abs(initialEnergy) * 100

            var kineticEnergy = 0f
            var potentialEnergy = 0f

            for (body in engine.getBodies()) {
                kineticEnergy += 0.5f * body.mass * body.velocity.magnitude() * body.velocity.magnitude()
            }

            for (i in engine.getBodies().indices) {
                for (j in i + 1 until engine.getBodies().size) {
                    val distance =
                        engine.getBodies()[i].position.distance(engine.getBodies()[j].position)
                    if (distance > 0) {
                        potentialEnergy -= (engine.getBodies()[i].mass * engine.getBodies()[j].mass) / distance
                    }
                }
            }

            println(
                "$step | ${String.format("%.2f", currentEnergy).padStart(12)} | ${
                    String.format(
                        "%.2f",
                        kineticEnergy
                    ).padStart(14)
                } | ${String.format("%.2f", potentialEnergy).padStart(16)} | ${
                    String.format(
                        "%.2f",
                        energyError
                    ).padStart(6)
                }"
            )
        }

        println("\nObservations:")
        println("✓ Total energy should remain roughly constant")
        println("✓ Energy error should be < 10% due to Euler integration")
        println("✓ Energy converts between kinetic and potential forms")
    }

    // ==================== Test: Interactive Custom Universe ====================
    fun testCustomUniverse() {
        println("TEST 5: CUSTOM UNIVERSE - BINARY STARS WITH PLANET")
        println("Creating a system with two stars and an orbiting planet...")
        println()

        val engine = PhysicsEngine()

        // Star 1
        engine.addBody(
            CelestialBody(
                id = "star1",
                position = Vector3D(-20f, 0f, 0f),
                velocity = Vector3D(0f, 2f, 0f),
                acceleration = Vector3D(0f, 0f, 0f),
                mass = 500f,
                radius = 5f,
                name = "Star A"
            )
        )

        // Star 2
        engine.addBody(
            CelestialBody(
                id = "star2",
                position = Vector3D(20f, 0f, 0f),
                velocity = Vector3D(0f, -2f, 0f),
                acceleration = Vector3D(0f, 0f, 0f),
                mass = 500f,
                radius = 5f,
                name = "Star B"
            )
        )

        // Planet
        engine.addBody(
            CelestialBody(
                id = "planet",
                position = Vector3D(0f, 30f, 0f),
                velocity = Vector3D(15f, 0f, 0f),
                acceleration = Vector3D(0f, 0f, 0f),
                mass = 10f,
                radius = 2f,
                name = "Planet"
            )
        )

        printBodiesInfo(engine.getBodies(), "Initial Configuration")

        println("\nSimulating for 300 steps...")
        println("Time | Star A Pos | Star B Pos | Planet Pos | Star Sep | Energy")
        println("─".repeat(75))

        for (step in 0..300 step 30) {
            repeat(30) { engine.step() }

            val bodies = engine.getBodies()
            val star1 = bodies[0]
            val star2 = bodies[1]
            val planet = bodies[2]

            val starSeparation = star1.position.distance(star2.position)
            val energy = engine.getTotalEnergy()

            print("$step | ")
            print(
                "(${String.format("%6.1f", star1.position.x)}, ${
                    String.format(
                        "%6.1f",
                        star1.position.y
                    )
                }) | "
            )
            print(
                "(${String.format("%6.1f", star2.position.x)}, ${
                    String.format(
                        "%6.1f",
                        star2.position.y
                    )
                }) | "
            )
            print(
                "(${String.format("%6.1f", planet.position.x)}, ${
                    String.format(
                        "%6.1f",
                        planet.position.y
                    )
                }) | "
            )
            print("${String.format("%.2f", starSeparation).padStart(7)} | ")
            println(String.format("%.2f", energy))
        }

        printBodiesInfo(engine.getBodies(), "Final State")

        println("\nObservations:")
        println("✓ Stars should orbit their common center of mass")
        println("✓ Planet experiences gravitational pull from both stars")
        println("✓ Complex orbital dynamics create interesting trajectories")
    }

    // ==================== Helper Functions ====================
    fun printBodiesInfo(bodies: List<CelestialBody>, label: String) {
        println("╭─ $label " + "─".repeat(50 - label.length))
        for (body in bodies) {
            println(
                "│ ${body.name.padEnd(15)} | Pos: (${
                    String.format(
                        "%7.2f",
                        body.position.x
                    )
                }, ${String.format("%7.2f", body.position.y)}, ${
                    String.format(
                        "%7.2f",
                        body.position.z
                    )
                }) | Vel: ${
                    String.format("%.2f", body.velocity.magnitude()).padStart(6)
                } m/s | Mass: ${String.format("%.2f", body.mass).padStart(6)} | Radius: ${
                    String.format(
                        "%.2f",
                        body.radius
                    ).padStart(5)
                }"
            )
        }
        println("╰" + "─".repeat(60))
    }

    fun formatVector(v: Vector3D, precision: Int = 2): String {
        val formatter = "%.${precision}f"
        return "(${formatter.format(v.x)}, ${formatter.format(v.y)}, ${formatter.format(v.z)})"
    }
}