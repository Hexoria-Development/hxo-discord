package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.ticketPanelContainer
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class PrintTicketPanelCommand(
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "ticket-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_BUTTONS)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Du benötigst die Admin- oder Dev-Rolle für diesen Befehl.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.channel.sendContainers(ticketPanelContainer()).queue()

        event.replyContainers(successContainer("Panel gepostet", "Das Ticket-Panel wurde gepostet."))
            .setEphemeral(true)
            .queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }
}
