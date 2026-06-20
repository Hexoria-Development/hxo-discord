package dev.hexoria.hxo.discord.automod.listener

import dev.hexoria.hxo.discord.automod.AutomodService
import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class VoiceSpamListener(
    private val automodService: AutomodService,
) : ListenerAdapter() {

    private val voiceTimestamps = ConcurrentHashMap<Long, ArrayDeque<Long>>()

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        if (event.member.user.isBot) return

        val config = botConfig.automod.voiceSpam
        if (!config.enabled) return
        if (event.member.hasPermission(DiscordPermission.AUTOMOD_BYPASS)) return

        val now = System.currentTimeMillis()
        val windowMs = config.timeWindowSeconds * 1000L

        val userTimestamps = voiceTimestamps.getOrPut(event.member.idLong) { ArrayDeque() }
        val isSpam: Boolean
        synchronized(userTimestamps) {
            userTimestamps.removeAll { now - it > windowMs }
            userTimestamps.addLast(now)
            isSpam = userTimestamps.size > config.maxEvents
            if (isSpam) userTimestamps.clear()
        }

        if (!isSpam) return

        event.guild.kickVoiceMember(event.member).queue(null) { }

        automodService.handleVoiceAction(
            member    = event.member,
            guildId   = event.guild.idLong,
            guildName = event.guild.name,
            reason    = "Zu schnelles Wechseln zwischen Sprachkanälen (${config.maxEvents}x in ${config.timeWindowSeconds}s)",
        )
    }
}