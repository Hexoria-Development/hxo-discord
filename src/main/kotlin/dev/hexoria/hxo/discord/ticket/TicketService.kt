package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.getRolesWithPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.entities.channel.concrete.ThreadChannel
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class TicketService(
    private val ticketRepository: TicketRepository,
    private val memberService: TicketMemberService,
    private val jda: JDA,
    private val ticketChannel: TextChannel?,
) {
    private val logger = componentLogger<TicketService>()

    suspend fun createTicket(
        type: TicketType,
        author: Member,
        formData: Map<String, String>,
    ): Ticket? {
        val authorId = author.idLong
        val authorName = author.user.name
        val authorAvatar = author.user.effectiveAvatarUrl
        val guildId = author.guild.idLong
        val channel = ticketChannel ?: run {
            logger.error("Ticket-Channel nicht verfügbar – Ticket kann nicht erstellt werden.")
            return null
        }

        if (!author.hasPermission(DiscordPermission.TICKET_LIMIT_BYPASS)) {
            val existingOpen = ticketRepository.findOpenByAuthorAndType(authorId, type.id)
            if (existingOpen.isNotEmpty()) {
                logger.warn("User $authorId hat bereits ein offenes ${type.id}-Ticket.")
                return null
            }
        }

        val ticketId = UUID.randomUUID()
        val internalCounter = ticketRepository.count() + 1

        val thread: ThreadChannel = channel
            .createThreadChannel("${type.id.substringBefore('_')}-$authorName", true)
            .setAutoArchiveDuration(ThreadChannel.AutoArchiveDuration.TIME_1_WEEK)
            .complete()

        val ticket = Ticket(
            ticketId     = ticketId,
            threadId     = thread.idLong,
            guildId      = guildId,
            ticketData   = formData,
            authorId     = authorId,
            authorName   = authorName,
            authorAvatar = authorAvatar,
            ticketType   = type,
            createdAt    = LocalDateTime.now(),
        ).also { it.internalTicketId = internalCounter }

        ticketRepository.insert(ticket)

        val guild = author.guild
        val viewRoleIds = type.viewPermission.getRolesWithPermission(guild.idLong)
        val bypassRoleIds = DiscordPermission.TICKET_TYPE_BYPASS.getRolesWithPermission(guild.idLong)

        addRoles(thread, (viewRoleIds + bypassRoleIds).distinct())
        memberService.addMember(ticket, author, silent = true, thread = thread)

        channel.upsertPermissionOverride(author)
            .grant(Permission.MESSAGE_SEND_IN_THREADS)
            .queue()

        thread.sendMessageComponents(buildTicketContainer(ticket))
            .useComponentsV2()
            .queue { message -> thread.pinMessageById(message.idLong).queue(null) { } }

        logger.info("Ticket #$internalCounter (${type.id}) von User $authorName ($authorId) erstellt → Thread ${thread.id}")
        return ticket
    }

    /**
     * Pingt die Rollen im Thread und gibt damit allen Mitgliedern Zugriff.
     *
     * Der Rollen-Ping wird direkt gesendet – dadurch werden die Mitglieder benachrichtigt und dem
     * Thread hinzugefügt, ohne dass die System-Meldung „hat X zum Thread hinzugefügt“ erscheint.
     * Anschließend wird die Nachricht wieder gelöscht.
     */
    private fun addRoles(thread: ThreadChannel, roleIds: Collection<Long>) {
        for (roleId in roleIds) {
            runCatching {
                val message = thread.sendMessage("<@&$roleId>").complete()
                message.delete().complete()
            }.onFailure {
                logger.warn("Rolle $roleId konnte keinen Zugriff auf Thread ${thread.id} erhalten.", it)
            }
        }
    }

    suspend fun closeTicket(
        ticket: Ticket,
        closedById: Long,
        closedByName: String,
        closedByAvatar: String?,
        reason: TicketCloseReason,
        thread: ThreadChannel? = null,
    ) {
        val resolvedThread = thread ?: ticket.getThreadChannel(jda)
        if (resolvedThread == null) {
            logger.warn("Thread für Ticket ${ticket.ticketId} nicht gefunden – Ticket wird nur in der Datenbank geschlossen.")
        }

        val closedAt = LocalDateTime.now()

        ticketRepository.close(
            ticketId       = ticket.ticketId,
            closedById     = closedById,
            closedByName   = closedByName,
            closedByAvatar = closedByAvatar,
            reason         = reason.id,
            closedAt       = closedAt,
        )

        val closedAtInstant = closedAt.toInstant(ZoneOffset.UTC)

        resolvedThread?.let { thread ->
            thread.sendContainers(
                container {
                    accentColor = COLOR_ERROR
                    section(closedByAvatar) {
                        header("Ticket geschlossen")
                        text("Dieses Ticket wurde von **$closedByName** geschlossen.")
                    }
                    divider(Separator.Spacing.LARGE)
                    field("Grund", reason.displayName)
                    field("Beschreibung", reason.description)
                    divider(Separator.Spacing.LARGE)
                    field("Ticket-Typ", ticket.ticketType.displayName)
                    field("Ersteller", "<@${ticket.authorId}>")
                    field("Geschlossen von", "<@$closedById>")
                    field("Zeitpunkt", "<t:${closedAtInstant.epochSecond}:F>")
                    footer("Ticket-ID: ${ticket.ticketId}")
                }
            ).setAllowedMentions(emptyList()).complete()

            thread.manager
                .setLocked(true)
                .setArchived(true)
                .queue()
        }

        val author = jda.getGuildById(ticket.guildId)?.getMemberById(ticket.authorId)
        if (author != null) {
            ticketChannel?.getPermissionOverride(author)?.delete()?.queue()
        }

        logger.info("Ticket ${ticket.ticketId} von $closedByName geschlossen. Grund: ${reason.id}")
    }

    suspend fun claimTicket(ticket: Ticket, claimedById: Long, claimedByName: String) {
        ticketRepository.claim(ticket.ticketId, claimedById, claimedByName)
        logger.info("Ticket ${ticket.ticketId} von $claimedByName ($claimedById) übernommen.")
    }

    suspend fun unclaimTicket(ticket: Ticket) {
        ticketRepository.unclaim(ticket.ticketId)
        logger.info("Ticket ${ticket.ticketId} Claim freigegeben.")
    }

    suspend fun getTicketByThreadId(threadId: Long): Ticket? =
        ticketRepository.findByThreadId(threadId)

    suspend fun isOpenTicket(threadId: Long): Boolean =
        getTicketByThreadId(threadId)?.isClosed() == false
}
