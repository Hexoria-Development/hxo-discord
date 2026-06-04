package dev.hiorcraft.nex.discord.ticket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

private val json = Json { ignoreUnknownKeys = true }

@Repository
class TicketRepository(private val jdbc: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        val type = TicketType.fromId(rs.getString("ticket_type"))!!
        Ticket(
            ticketId = UUID.fromString(rs.getString("ticket_id")),
            threadId = rs.getLong("thread_id"),
            guildId = rs.getLong("guild_id"),
            authorId = rs.getLong("author_id"),
            authorName = rs.getString("author_name"),
            authorAvatar = rs.getString("author_avatar"),
            ticketType = type,
            ticketData = json.decodeFromString(rs.getString("ticket_data")),
            createdAt = rs.getTimestamp("created_at").toLocalDateTime(),
            claimedById = rs.getLong("claimed_by_id").takeIf { !rs.wasNull() },
            claimedByName = rs.getString("claimed_by_name"),
            closedAt = rs.getTimestamp("closed_at")?.toLocalDateTime(),
            closedById = rs.getLong("closed_by_id").takeIf { !rs.wasNull() },
            closedByName = rs.getString("closed_by_name"),
            closedByAvatar = rs.getString("closed_by_avatar"),
            closedReason = rs.getString("closed_reason"),
        ).also { it.internalTicketId = rs.getLong("internal_ticket_id").takeIf { _ -> !rs.wasNull() } }
    }

    suspend fun insert(ticket: Ticket) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO tickets
               (ticket_id, thread_id, guild_id, author_id, author_name, author_avatar,
                ticket_type, ticket_data, internal_ticket_id, created_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            ticket.ticketId.toString(), ticket.threadId, ticket.guildId,
            ticket.authorId, ticket.authorName, ticket.authorAvatar,
            ticket.ticketType.id, json.encodeToString(ticket.ticketData),
            ticket.internalTicketId, Timestamp.valueOf(ticket.createdAt),
        )
    }

    suspend fun findByThreadId(threadId: Long): Ticket? = withContext(Dispatchers.IO) {
        jdbc.query("SELECT * FROM tickets WHERE thread_id = ?", rowMapper, threadId).firstOrNull()
    }

    suspend fun findOpenByAuthor(authorId: Long): List<Ticket> = withContext(Dispatchers.IO) {
        jdbc.query("SELECT * FROM tickets WHERE author_id = ? AND closed_at IS NULL", rowMapper, authorId)
    }

    suspend fun close(
        ticketId: UUID,
        closedById: Long,
        closedByName: String,
        closedByAvatar: String?,
        reason: String,
        closedAt: LocalDateTime = LocalDateTime.now(),
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """UPDATE tickets SET closed_at = ?, closed_by_id = ?, closed_by_name = ?,
               closed_by_avatar = ?, closed_reason = ? WHERE ticket_id = ?""",
            Timestamp.valueOf(closedAt), closedById, closedByName,
            closedByAvatar, reason, ticketId.toString(),
        )
    }

    suspend fun claim(ticketId: UUID, claimedById: Long, claimedByName: String) = withContext(Dispatchers.IO) {
        jdbc.update(
            "UPDATE tickets SET claimed_by_id = ?, claimed_by_name = ? WHERE ticket_id = ?",
            claimedById, claimedByName, ticketId.toString(),
        )
    }

    suspend fun unclaim(ticketId: UUID) = withContext(Dispatchers.IO) {
        jdbc.update(
            "UPDATE tickets SET claimed_by_id = NULL, claimed_by_name = NULL WHERE ticket_id = ?",
            ticketId.toString(),
        )
    }

    fun count(): Long =
        jdbc.queryForObject("SELECT COUNT(*) FROM tickets", Long::class.java) ?: 0L
}
