package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TicketMemberService(
    private val memberRepository: TicketMemberRepository,
    private val jda: JDA,
) {
    private val logger = componentLogger<TicketMemberService>()

    suspend fun addMember(ticket: Ticket, member: Member, silent: Boolean = false, thread: ThreadChannel? = null, addedBy: Member? = null) {
        if (isMember(ticket, member.idLong)) {
            logger.debug("User ${member.user.name} ist bereits Mitglied in Ticket ${ticket.ticketId}.")
            return
        }

        memberRepository.addMember(ticket.ticketId, member.idLong, member.user.name)

        val resolvedThread = thread ?: ticket.getThreadChannel(jda) ?: return

        if (silent) {
            resolvedThread.sendMessage(member.asMention).queue { msg -> msg.delete().queue(null) { } }
        } else {
            resolvedThread.sendContainers(container {
                accentColor = COLOR_INFO
                header("Willkommen im Ticket")
                text(
                    "${member.asMention}, du wurdest zu diesem Ticket hinzugefügt. Bitte sieh dir den " +
                    "Verlauf des Tickets an und warte auf eine Nachricht eines Teammitglieds."
                )
                if (addedBy != null) footer("Hinzugefügt von ${addedBy.user.name}", now) else footer(now)
            }).queue()
        }
        logger.info("User ${member.user.name} zu Ticket ${ticket.ticketId} hinzugefügt.")
    }

    suspend fun removeMember(ticket: Ticket, userId: Long) {
        memberRepository.removeMember(ticket.ticketId, userId)

        val thread = ticket.getThreadChannel(jda)
        thread?.removeThreadMember(jda.getUserById(userId) ?: return)?.queue()
        logger.info("User $userId aus Ticket ${ticket.ticketId} entfernt.")
    }

    suspend fun isMember(ticket: Ticket, userId: Long): Boolean =
        memberRepository.isMember(ticket.ticketId, userId)
}

fun Ticket.getThreadChannel(jda: JDA): ThreadChannel? =
    jda.getThreadChannelById(threadId)
