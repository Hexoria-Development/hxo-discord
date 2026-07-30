package dev.hexoria.hxo.discord.ticket.listener

import dev.hexoria.hxo.discord.ticket.*
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class TicketCloseReasonListener(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.componentId != "ticket:close:reason") return

        val selectedValue = event.values.firstOrNull() ?: return
        val parts = selectedValue.split(":", limit = 2)
        if (parts.size != 2) return

        val (typeId, reasonId) = parts
        val type = TicketType.fromId(typeId) ?: return
        val reason = type.closeReasons.firstOrNull { it.id == reasonId } ?: return

        val thread = event.channel.asThreadChannel()
        if (thread.isArchived) {
            thread.manager.setArchived(false).complete()
        }

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
                    errorContainer("Bereits geschlossen", "Dieses Ticket ist bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            val member = event.member!!
            ticketService.closeTicket(
                ticket         = ticket,
                closedById     = member.idLong,
                closedByName   = member.user.name,
                closedByAvatar = member.user.effectiveAvatarUrl,
                reason         = reason,
                thread         = thread,
            )

            event.hook.editContainers(
                successContainer("Ticket geschlossen", "Das Ticket wurde erfolgreich geschlossen.")
            ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }
}
