package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.getRolesWithPermission
import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
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

        val existingOpen = ticketRepository.findOpenByAuthor(authorId)
        if (existingOpen.isNotEmpty()) {
            logger.warn("User $authorId hat bereits ${existingOpen.size} offenes Ticket.")
            return null
        }

        val ticketId = UUID.randomUUID()
        val internalCounter = ticketRepository.count() + 1

        val userTicketNumber = (existingOpen.size + 1).toString().padStart(2, '0')
        val thread: ThreadChannel = channel
            .createThreadChannel("${type.id}-${authorName}-${internalCounter}", true)
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

        val addedUserIds = mutableSetOf<Long>()
        for (roleId in viewRoleIds + bypassRoleIds) {
            val role = guild.getRoleById(roleId) ?: continue
            guild.getMembersWithRoles(role).forEach { member ->
                if (addedUserIds.add(member.idLong)) {
                    memberService.addMember(ticket, member, silent = true, thread = thread)
                }
            }
        }

        if (addedUserIds.add(author.idLong)) {
            memberService.addMember(ticket, author, silent = true, thread = thread)
        }

        channel.upsertPermissionOverride(author)
            .grant(Permission.MESSAGE_SEND_IN_THREADS)
            .queue()

        thread.sendMessageEmbeds(
            embed {
                setTitle("${type.emoji} ${type.displayName} – Ticket #$internalCounter")
                setDescription(buildTicketDescription(type, authorId, formData))
                setColor(COLOR_INFO)
                setTimestamp(Instant.now())
                setFooter("Ticket-ID: $ticketId")
            }
        ).setComponents(
            ActionRow.of(
                Button.success("ticket:claim", "🙋 Ticket übernehmen"),
                Button.danger("ticket:close:btn", "🔒 Schließen"),
                Button.secondary("ticket:userinfo", "👤 User Info"),
            )
        ).queue()

        logger.info("Ticket #$internalCounter (${type.id}) von User $authorName ($authorId) erstellt → Thread ${thread.id}")
        return ticket
    }

    suspend fun closeTicket(
        ticket: Ticket,
        closedById: Long,
        closedByName: String,
        closedByAvatar: String?,
        reason: TicketCloseReason,
    ) {
        val thread = ticket.getThreadChannel(jda) ?: run {
            logger.warn("Thread für Ticket ${ticket.ticketId} nicht gefunden.")
            return
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

        thread.sendMessageEmbeds(
            embed {
                setTitle("🔒 Ticket geschlossen")
                setColor(COLOR_ERROR)
                setDescription(
                    """
                    Dieses Ticket wurde von **$closedByName** geschlossen.

                    **Grund:** ${reason.displayName}
                    **Beschreibung:** ${reason.description}
                    """.trimIndent()
                )
                addField("Ersteller", "<@${ticket.authorId}>", true)
                addField("Geschlossen von", "<@$closedById>", true)
                addField("Zeitpunkt", "<t:${closedAtInstant.epochSecond}:F>", true)
                setTimestamp(closedAtInstant)
            }
        ).complete()

        val author = jda.getGuildById(ticket.guildId)?.getMemberById(ticket.authorId)
        if (author != null) {
            ticketChannel?.getPermissionOverride(author)?.delete()?.queue()
        }

        thread.manager
            .setLocked(true)
            .setArchived(true)
            .queue()

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

private fun buildTicketDescription(type: TicketType, authorId: Long, data: Map<String, String>): String =
    when (type) {
        TicketType.REPORT -> buildString {
            appendLine("Willkommen <@$authorId>! Dein Report wurde erstellt.")
            appendLine()
            appendLine("**Gemeldeter Spieler:** ${data["reported_name"] ?: "–"}")
            data["reported_user"]?.let { appendLine("**Discord:** $it") }
            appendLine()
            appendLine("**Anliegen:**")
            append(data["description"] ?: "")
        }
        TicketType.UNBAN -> buildString {
            appendLine("Willkommen <@$authorId>! Dein Entbannungsantrag wurde erstellt.")
            appendLine()
            appendLine("**Minecraft-Name:** ${data["minecraft_name"] ?: "–"}")
            appendLine("**Punish-ID:** ${data["punish_id"] ?: "–"}")
            appendLine()
            appendLine("**Begründung:**")
            append(data["description"] ?: "")
        }
        TicketType.CONTENT_SUPPORT -> buildString {
            appendLine("Willkommen <@$authorId>! Dein Content Support Ticket wurde erstellt.")
            appendLine()
            appendLine("**Art des Contents:** ${data["content_type"] ?: "–"}")
            appendLine()
            appendLine("**Anliegen:**")
            appendLine(data["description"] ?: "")
            appendLine()
            append("Das Management wird sich so schnell wie möglich um dein Anliegen kümmern.")
        }
        else -> buildString {
            appendLine("Willkommen <@$authorId>! Dein Ticket wurde erstellt.")
            appendLine()
            appendLine("**Dein Anliegen:**")
            appendLine(data["description"] ?: "")
            appendLine()
            append("Ein Teamer wird sich so schnell wie möglich um dein Anliegen kümmern.")
        }
    }
