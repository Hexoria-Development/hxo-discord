package dev.hexoria.hxo.discord.logging.listener

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.logging.LoggingRepository
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class VoiceLogListener(
    private val jda: JDA,
    private val loggingRepository: LoggingRepository,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val logChannelId = botConfig.channels.voiceLogChannelId
        if (logChannelId == 0L) return

        val member = event.member
        val channelLeft = event.channelLeft
        val channelJoined = event.channelJoined

        val (eventType, title, color) = when {
            channelLeft == null && channelJoined != null -> Triple("JOIN",  "🔊 Voice Beigetreten", COLOR_INFO)
            channelLeft != null && channelJoined == null -> Triple("LEAVE", "🔇 Voice Verlassen",   COLOR_ERROR)
            else                                         -> Triple("MOVE",  "🔀 Voice Gewechselt",  COLOR_WARNING)
        }

        coroutineScope.launch {
            loggingRepository.logVoice(
                guildId     = event.guild.idLong,
                userId      = member.idLong,
                userName    = member.user.name,
                eventType   = eventType,
                channelFrom = channelLeft?.name,
                channelTo   = channelJoined?.name,
            )

            jda.getTextChannelById(logChannelId)?.sendMessageEmbeds(embed {
                setTitle(title)
                setColor(color)
                setThumbnail(member.user.effectiveAvatarUrl)
                addField("User", "${member.asMention}\n`${member.user.name}`", true)
                addField("ID", "`${member.idLong}`", true)
                if (channelLeft != null)   addField("Von",  channelLeft.asMention,   true)
                if (channelJoined != null) addField("Nach", channelJoined.asMention, true)
                setFooter("Server: ${event.guild.name}")
                setTimestamp(Instant.now())
            })?.queue()
        }
    }
}
