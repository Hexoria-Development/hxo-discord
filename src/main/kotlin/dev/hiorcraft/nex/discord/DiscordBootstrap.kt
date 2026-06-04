package dev.hiorcraft.nex.discord

import dev.hiorcraft.nex.discord.util.componentLogger
import org.springframework.stereotype.Component

@Component
class DiscordBootstrap {

    private val logger = componentLogger<DiscordBootstrap>()

    fun onLoad() {
        logger.info("Loading Discord Bot...")
    }

    fun onDisable() {
        logger.info("Stopping Discord Bot...")
    }
}
