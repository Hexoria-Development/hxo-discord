package dev.hexoria.hxo.discord.ticket.listener

import dev.hexoria.hxo.discord.ticket.TicketService
import dev.hexoria.hxo.discord.util.componentLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateArchivedEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class TicketArchivingListener(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    private val logger = componentLogger<TicketArchivingListener>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.channelType.isThread) return
        val type = event.message.type
        if (type == MessageType.DEFAULT || type == MessageType.INLINE_REPLY) return

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: return@launch
            if (!ticket.isClosed()) {
                event.channel.asThreadChannel().deleteMessageById(event.messageIdLong).queue(null) { }
            }
        }
    }

    override fun onChannelUpdateArchived(event: ChannelUpdateArchivedEvent) {
        val channel = event.channel
        if (!channel.type.isThread) return

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(channel.idLong) ?: return@launch

            if (ticket.isClosed()) {
                logger.debug("Ticket ${ticket.ticketId} ist geschlossen – Archivierung erlaubt.")
                return@launch
            }

            channel.asThreadChannel().manager.setArchived(false).queue()
            logger.info("Archivierung von aktivem Ticket-Thread '${channel.name}' verhindert.")
        }
    }
}
