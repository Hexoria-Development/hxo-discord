package dev.hiorcraft.nex.discord.ticket

import dev.hiorcraft.nex.discord.util.componentLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.minutes

@Service
class TicketDeadlineService(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
    private val jda: JDA,
) {
    private val logger = componentLogger<TicketDeadlineService>()
    private val jobs = ConcurrentHashMap<Long, Job>()

    fun schedule(ticket: Ticket, durationMinutes: Long) {
        cancel(ticket.threadId)

        val job = coroutineScope.launch {
            delay(durationMinutes.minutes)

            val current = ticketService.getTicketByThreadId(ticket.threadId)
            if (current == null || current.isClosed()) {
                jobs.remove(ticket.threadId)
                return@launch
            }

            val self = jda.selfUser
            ticketService.closeTicket(
                ticket         = current,
                closedById     = self.idLong,
                closedByName   = self.name,
                closedByAvatar = self.effectiveAvatarUrl,
                reason         = TicketCloseReason.of(
                    "no_response",
                    "Keine Antwort",
                    "Der User hat innerhalb der gesetzten Frist ($durationMinutes Minuten) nicht geantwortet.",
                ),
            )

            jobs.remove(ticket.threadId)
            logger.info("Ticket ${ticket.ticketId} automatisch geschlossen (Deadline abgelaufen).")
        }

        jobs[ticket.threadId] = job
        logger.info("Deadline für Ticket ${ticket.ticketId} gesetzt: $durationMinutes Minuten.")
    }

    fun cancelIfAuthorResponded(threadId: Long, senderId: Long, ticketAuthorId: Long) {
        if (senderId == ticketAuthorId && jobs.containsKey(threadId)) {
            cancel(threadId)
            logger.info("Deadline für Thread $threadId aufgehoben (User hat geantwortet).")
        }
    }

    fun cancel(threadId: Long) {
        jobs.remove(threadId)?.cancel()
    }
}
