package com.asaad27

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration().apply {
            setWindowedMode(2060, 1400)
        }
        config.setForegroundFPS(60)
        config.setTitle("PhysicSim")
        Lwjgl3Application(PhysicSim(), config)
    }
}