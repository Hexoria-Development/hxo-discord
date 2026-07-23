package dev.hexoria.hxo.discord.voice.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.util.*
import dev.hexoria.hxo.discord.voice.TempVoiceService
import dev.hexoria.hxo.discord.voice.voiceInfoContainer
import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class VoicePanelCommand(
    private val tempVoiceService: TempVoiceService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "voice-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_VOICE_PANEL)) {
            event.replyContainers(errorContainer("Keine Berechtigung", "Nur Admins können diesen Befehl nutzen."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val config = tempVoiceService.config
        if (!config.enabled || config.creatorChannelId == 0L) {
            event.replyContainers(
                errorContainer(
                    "Nicht konfiguriert",
                    "Das Voice-System ist deaktiviert oder es ist kein `creatorChannelId` in der config.yml gesetzt.",
                )
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.channel.sendContainers(voiceInfoContainer(config.creatorChannelId)).queue()
        event.replyContainers(successContainer("Panel gepostet", "Die Willkommens-Nachricht wurde gepostet."))
            .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }
}
