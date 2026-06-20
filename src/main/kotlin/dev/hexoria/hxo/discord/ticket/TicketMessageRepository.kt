package dev.hexoria.hxo.discord.ticket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

data class TicketMessage(
    val id: Long = 0,
    val ticketId: String,
    val messageId: Long,
    val authorId: Long,
    val authorName: String,
    val content: String?,
    val attachments: String?,
    val sentAt: LocalDateTime,
)

@Repository
class TicketMessageRepository(private val jdbc: JdbcTemplate) {

    suspend fun insert(message: TicketMessage) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT IGNORE INTO ticket_messages
               (ticket_id, message_id, author_id, author_name, content, attachments, sent_at)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            message.ticketId, message.messageId, message.authorId,
            message.authorName, message.content, message.attachments,
            Timestamp.valueOf(message.sentAt),
        )
    }

    suspend fun findByTicketId(ticketId: String): List<TicketMessage> = withContext(Dispatchers.IO) {
        jdbc.query(
            "SELECT * FROM ticket_messages WHERE ticket_id = ? ORDER BY sent_at ASC",
            { rs, _ ->
                TicketMessage(
                    id          = rs.getLong("id"),
                    ticketId    = rs.getString("ticket_id"),
                    messageId   = rs.getLong("message_id"),
                    authorId    = rs.getLong("author_id"),
                    authorName  = rs.getString("author_name"),
                    content     = rs.getString("content"),
                    attachments = rs.getString("attachments"),
                    sentAt      = rs.getTimestamp("sent_at").toLocalDateTime(),
                )
            },
            ticketId,
        )
    }
}
