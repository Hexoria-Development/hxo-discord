package dev.hiorcraft.nex.discord

import dev.hiorcraft.nex.discord.config.botConfig
import dev.hiorcraft.nex.discord.util.componentLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.ChunkingFilter
import net.dv8tion.jda.api.utils.MemberCachePolicy
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Service
import kotlin.system.exitProcess

@Service
class DiscordBot {

    private val logger = componentLogger<DiscordBot>()

    @Bean
    fun jda(): JDA {
        val botToken = botConfig.botToken

        val builder = JDABuilder.createDefault(botToken)

        builder.enableIntents(gatewayIntents)
        builder.setMemberCachePolicy(MemberCachePolicy.ALL)
        builder.setChunkingFilter(ChunkingFilter.ALL)
        builder.setStatus(OnlineStatus.ONLINE)
        builder.setActivity(Activity.playing("play.hexoria.net"))

        val jda = builder.build()

        try {
            jda.awaitReady()
            logger.info("Discord Bot is ready. Loading commands...")
        } catch (exception: InterruptedException) {
            logger.error("Failed to await ready.", exception)
            exitProcess(1)
        }

        return jda
    }

    private val gatewayIntents = listOf(
        GatewayIntent.GUILD_MEMBERS,
        GatewayIntent.GUILD_PRESENCES,
        GatewayIntent.GUILD_VOICE_STATES,
        GatewayIntent.GUILD_MODERATION,
        GatewayIntent.SCHEDULED_EVENTS,

        GatewayIntent.MESSAGE_CONTENT,
        GatewayIntent.GUILD_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS,
        GatewayIntent.GUILD_MESSAGE_TYPING,


        GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.DIRECT_MESSAGE_REACTIONS,
        GatewayIntent.DIRECT_MESSAGE_TYPING
    )
}
