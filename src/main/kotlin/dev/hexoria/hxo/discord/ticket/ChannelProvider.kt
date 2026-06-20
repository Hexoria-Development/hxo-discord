package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.util.componentLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class ChannelProvider(private val jda: JDA) {
    private val logger = componentLogger<ChannelProvider>()

    @Bean
    fun ticketChannel(): TextChannel? {
        val channelId = botConfig.channels.ticketChannelId
        if (channelId == 0L) {
            logger.warn("Kein Ticket-Channel konfiguriert (channels.ticketChannelId = 0).")
            return null
        }
        return jda.getTextChannelById(channelId) ?: run {
            logger.error("Ticket-Channel mit ID $channelId nicht gefunden!")
            null
        }
    }

    @Bean
    fun automodLogChannel(): TextChannel? {
        val channelId = botConfig.channels.automodLogChannelId
        if (channelId == 0L) {
            logger.warn("Kein Automod-Log-Channel konfiguriert (channels.automodLogChannelId = 0).")
            return null
        }
        return jda.getTextChannelById(channelId) ?: run {
            logger.error("Automod-Log-Channel mit ID $channelId nicht gefunden!")
            null
        }
    }
}
