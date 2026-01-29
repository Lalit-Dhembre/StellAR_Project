@file:JvmName("RunSimulator")
package com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine

/**
 * Entry point for running the Console Simulator
 * This can be run as: kotlin -classpath <build-output> com.cosmic_struck.stellar.stellar.arlab.universe_lab.engine.RunSimulator
 */
object RunSimulator {
    @JvmStatic
    fun main(args: Array<String>) {
        val simulator = ConsoleSimulator()
        simulator.main()
    }
}
