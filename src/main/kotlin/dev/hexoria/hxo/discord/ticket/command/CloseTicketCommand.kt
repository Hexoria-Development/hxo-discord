package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.*
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class CloseTicketCommand(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "close") return

        event.deferReply(true).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.editContainers(
                    errorContainer("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.editContainers(
                    errorContainer("Bereits geschlossen", "Dieses Ticket wurde bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            val member = event.member ?: return@launch
            val hasPermission = member.hasPermission(DiscordPermission.TICKET_CLOSE)

            if (!hasPermission) {
                event.hook.editContainers(
                    errorContainer("Keine Berechtigung", "Nur ein Teamer kann dieses Ticket schließen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            event.hook.editContainers(container {
                accentColor = COLOR_INFO
                text("Bitte wähle einen Schließ-Grund:")
                buttons(buildCloseReasonMenu(ticket.ticketType))
            }).queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }

    private fun buildCloseReasonMenu(type: TicketType): StringSelectMenu {
        val builder = StringSelectMenu.create("ticket:close:reason")
            .setPlaceholder("Grund auswählen...")

        type.closeReasons.forEach { reason ->
            builder.addOption(
                reason.displayName,
                "${type.id}:${reason.id}",
                reason.description,
            )
        }
        return builder.build()
    }
}
