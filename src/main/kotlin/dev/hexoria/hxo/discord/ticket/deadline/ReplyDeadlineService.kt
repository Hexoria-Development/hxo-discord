package dev.hexoria.hxo.discord.ticket.deadline

import dev.hexoria.hxo.discord.ticket.Ticket
import dev.hexoria.hxo.discord.ticket.TicketRepository
import dev.hexoria.hxo.discord.util.componentLogger
import dev.hexoria.hxo.discord.util.sendContainers
import dev.hexoria.hxo.discord.util.warningContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.minutes

@Service
class ReplyDeadlineService(
    private val deadlineRepository: ReplyDeadlineRepository,
    private val notifyRepository: DeadlineNotifyRepository,
    private val ticketRepository: TicketRepository,
    private val coroutineScope: CoroutineScope,
    private val jda: JDA,
) {
    private val logger = componentLogger<ReplyDeadlineService>()

    @EventListener(ApplicationReadyEvent::class)
    fun startExpiryLoop() {
        coroutineScope.launch {
            while (isActive) {
                delay(1.minutes)
                runCatching { checkExpiredDeadlines() }.onFailure {
                    logger.error("Fehler beim Prüfen abgelaufener Antwort-Fristen.", it)
                }
            }
        }
    }

    suspend fun createDeadline(ticket: Ticket, target: User, setBy: User, deadline: LocalDateTime) {
        // Pro User und Thread gilt nur eine Frist – eine neue ersetzt die alte.
        deadlineRepository.deleteForUserInThread(ticket.threadId, target.idLong)
        deadlineRepository.create(
            ticketId       = ticket.ticketId,
            threadId       = ticket.threadId,
            targetUserId   = target.idLong,
            targetUserName = target.name,
            setById        = setBy.idLong,
            setByName      = setBy.name,
            deadline       = deadline,
        )
        logger.info("Antwort-Frist für ${target.name} in Ticket ${ticket.ticketId} gesetzt: $deadline (von ${setBy.name}).")
    }

    suspend fun onUserReplied(threadId: Long, userId: Long) {
        deadlineRepository.deleteForUserInThread(threadId, userId)
    }

    suspend fun checkExpiredDeadlines() {
        for (deadline in deadlineRepository.findExpired(LocalDateTime.now())) {
            if (!deadlineRepository.delete(deadline.id)) continue

            val ticket = ticketRepository.findByThreadId(deadline.threadId)
            if (ticket == null || ticket.isClosed()) continue

            if (notifyRepository.isEnabled(deadline.setById)) {
                notifySetter(deadline)
            }
        }
    }

    private fun notifySetter(deadline: ReplyDeadline) {
        try {
            jda.openPrivateChannelById(deadline.setById).complete()
                .sendContainers(
                    warningContainer(
                        "Antwort-Frist abgelaufen",
                        "**${deadline.targetUserName}** (<@${deadline.targetUserId}>) hat im Ticket " +
                        "<#${deadline.threadId}> nicht innerhalb der Frist geantwortet.",
                    )
                )
                .setAllowedMentions(emptyList())
                .complete()
        } catch (e: ErrorResponseException) {
            when (e.errorResponse.code) {
                50007, 50278 -> logger.info(
                    "Antwort-Frist-DM an User ${deadline.setById} nicht möglich (DMs deaktiviert/blockiert, Code ${e.errorResponse.code})."
                )
                else -> logger.warn("Antwort-Frist-DM an User ${deadline.setById} fehlgeschlagen.", e)
            }
        } catch (e: Exception) {
            logger.warn("Antwort-Frist-DM an User ${deadline.setById} fehlgeschlagen.", e)
        }
    }
}
