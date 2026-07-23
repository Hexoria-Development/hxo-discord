package dev.hexoria.hxo.discord.voice.listener

import dev.hexoria.hxo.discord.util.componentLogger
import dev.hexoria.hxo.discord.util.sendContainers
import dev.hexoria.hxo.discord.voice.TempVoiceService
import dev.hexoria.hxo.discord.voice.voiceManagerContainer
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class TempVoiceListener(
    private val tempVoiceService: TempVoiceService,
) : ListenerAdapter() {

    private val logger = componentLogger<TempVoiceListener>()

    override fun onGuildVoiceUpdate(event: GuildVoiceUpdateEvent) {
        val config = tempVoiceService.config
        if (!config.enabled || config.creatorChannelId == 0L) return

        val joined = event.channelJoined
        val left = event.channelLeft

        if (joined?.idLong == config.creatorChannelId) {
            createChannel(event)
        }

        // Der verlassene Kanal wird gelöscht, sobald niemand mehr drin ist.
        if (left != null && left.idLong != config.creatorChannelId) {
            val voiceChannel = left.asVoiceChannel()
            if (tempVoiceService.isTempChannel(voiceChannel.idLong)) {
                tempVoiceService.deleteIfEmpty(voiceChannel)
            }
        }
    }

    private fun createChannel(event: GuildVoiceUpdateEvent) {
        val member = event.member
        val channel = tempVoiceService.createFor(member) ?: return

        // Das Manager-Panel läuft in den Text-Chat des neuen Kanals – so sieht es nur, wer drin ist.
        channel.sendContainers(voiceManagerContainer(channel, member.idLong)).queue(
            { message ->
                tempVoiceService.rememberPanel(channel.idLong, message.idLong)
                channel.pinMessageById(message.idLong).queue(null) { }
            },
            { logger.warn("Temp-Voice: Manager-Panel konnte nicht gepostet werden: ${it.message}") },
        )
    }
}
