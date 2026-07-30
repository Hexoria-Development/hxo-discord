package dev.hexoria.hxo.discord.ticket.listener

import dev.hexoria.hxo.discord.ticket.*
import dev.hexoria.hxo.discord.ticket.deadline.ReplyDeadlineService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class TicketMessageListener(
    private val ticketService: TicketService,
    private val messageRepository: TicketMessageRepository,
    private val replyDeadlineService: ReplyDeadlineService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.channel.type.isThread) return

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: return@launch

            replyDeadlineService.onUserReplied(
                threadId = event.channel.idLong,
                userId   = event.author.idLong,
            )

            val attachments = event.message.attachments
                .joinToString(",") { it.url }
                .takeIf { it.isNotBlank() }

            messageRepository.insert(
                TicketMessage(
                    ticketId    = ticket.ticketId.toString(),
                    messageId   = event.messageIdLong,
                    authorId    = event.author.idLong,
                    authorName  = event.author.name,
                    content     = event.message.contentDisplay.takeIf { it.isNotBlank() },
                    attachments = attachments,
                    sentAt      = LocalDateTime.now(),
                )
            )
        }
    }
}
