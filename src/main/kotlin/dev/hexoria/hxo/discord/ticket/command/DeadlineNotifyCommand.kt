package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.deadline.DeadlineNotifyRepository
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class DeadlineNotifyCommand(
    private val notifyRepository: DeadlineNotifyRepository,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "deadline-notify") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_DEADLINE)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Nur Teamer können diese Einstellung ändern.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.deferReply(true).queue()

        coroutineScope.launch {
            val enabled = !notifyRepository.isEnabled(event.user.idLong)
            notifyRepository.setEnabled(event.user.idLong, enabled)

            event.hook.editContainers(
                if (enabled) successContainer(
                    "Benachrichtigung aktiviert",
                    "Du wirst per DM benachrichtigt, wenn eine von dir gesetzte Antwort-Frist abläuft.",
                ) else successContainer(
                    "Benachrichtigung deaktiviert",
                    "Du wirst nicht mehr per DM benachrichtigt, wenn eine von dir gesetzte Antwort-Frist abläuft.",
                )
            ).queue()
        }
    }
}
