package dev.hiorcraft.nex.discord.automod.listener

import dev.hiorcraft.nex.discord.automod.AutomodService
import dev.hiorcraft.nex.discord.config.botConfig
import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.permission.hasPermission
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