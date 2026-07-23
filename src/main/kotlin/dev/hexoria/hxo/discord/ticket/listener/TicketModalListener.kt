package dev.hexoria.hxo.discord.ticket.listener

import dev.hexoria.hxo.discord.ticket.*
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class TicketModalListener(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onModalInteraction(event: ModalInteractionEvent) {
        val modalId = event.modalId
        if (!modalId.startsWith("ticket:modal:")) return

        val typeId = modalId.removePrefix("ticket:modal:")
        val type = TicketType.fromId(typeId) ?: run {
            event.replyContainers(errorContainer("Fehler", "Unbekannter Ticket-Typ."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val formData = type.extractFormData(event)

        val author = event.member ?: run {
            event.replyContainers(errorContainer("Fehler", "Dieser Befehl ist nur auf einem Server verfügbar."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.deferReply(true).queue()

        coroutineScope.launch {
            val ticket = ticketService.createTicket(
                type     = type,
                author   = author,
                formData = formData,
            )

            if (ticket == null) {
                event.hook.editContainers(
                    errorContainer(
                        "Ticket konnte nicht erstellt werden",
                        "Du hast bereits ein offenes Ticket oder der Ticket-Channel ist nicht konfiguriert.",
                    )
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
            } else {
                event.hook.editContainers(
                    successContainer(
                        "Ticket erstellt",
                        "Dein Ticket **#${ticket.internalTicketId}** wurde erstellt! <#${ticket.threadId}>",
                    )
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
            }
        }
    }
}
