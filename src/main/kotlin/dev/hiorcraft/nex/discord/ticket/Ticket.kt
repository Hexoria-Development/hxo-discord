package dev.hiorcraft.nex.discord.ticket

import java.time.LocalDateTime
import java.util.*

typealias TicketData = Map<String, String>

data class Ticket(
    val ticketId: UUID,
    val threadId: Long,
    val guildId: Long,
    val ticketData: TicketData,
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val ticketType: TicketType,
    val createdAt: LocalDateTime,
    val claimedById: Long? = null,
    val claimedByName: String? = null,
    val closedAt: LocalDateTime? = null,
    val closedById: Long? = null,
    val closedByName: String? = null,
    val closedByAvatar: String? = null,
    val closedReason: String? = null,
) {
    var internalTicketId: Long? = null

    fun isClosed() = closedAt != null
    fun isClaimed() = claimedById != null
}
