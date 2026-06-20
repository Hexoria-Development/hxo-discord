package dev.hexoria.hxo.discord

import dev.hexoria.hxo.discord.util.componentLogger
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
