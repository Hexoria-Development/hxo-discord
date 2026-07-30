package dev.hexoria.hxo.discord.ticket.deadline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.RowMapper
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

@Repository
class ReplyDeadlineRepository(private val jdbc: JdbcTemplate) {

    private val rowMapper = RowMapper { rs, _ ->
        ReplyDeadline(
            id             = rs.getLong("id"),
            ticketId       = UUID.fromString(rs.getString("ticket_id")),
            threadId       = rs.getLong("thread_id"),
            targetUserId   = rs.getLong("target_user_id"),
            targetUserName = rs.getString("target_user_name"),
            setById        = rs.getLong("set_by_id"),
            setByName      = rs.getString("set_by_name"),
            deadline       = rs.getTimestamp("deadline").toLocalDateTime(),
        )
    }

    suspend fun create(
        ticketId: UUID,
        threadId: Long,
        targetUserId: Long,
        targetUserName: String,
        setById: Long,
        setByName: String,
        deadline: LocalDateTime,
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO ticket_reply_deadlines
               (ticket_id, thread_id, target_user_id, target_user_name, set_by_id, set_by_name, deadline)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            ticketId.toString(), threadId, targetUserId, targetUserName, setById, setByName,
            Timestamp.valueOf(deadline),
        )
    }

    suspend fun deleteForUserInThread(threadId: Long, userId: Long) = withContext(Dispatchers.IO) {
        jdbc.update(
            "DELETE FROM ticket_reply_deadlines WHERE thread_id = ? AND target_user_id = ?",
            threadId, userId,
        )
    }

    suspend fun findExpired(now: LocalDateTime): List<ReplyDeadline> = withContext(Dispatchers.IO) {
        jdbc.query(
            "SELECT * FROM ticket_reply_deadlines WHERE deadline <= ?",
            rowMapper, Timestamp.valueOf(now),
        )
    }

    /** Löscht die Deadline und meldet, ob sie noch existierte – schützt vor doppelter Benachrichtigung. */
    suspend fun delete(id: Long): Boolean = withContext(Dispatchers.IO) {
        jdbc.update("DELETE FROM ticket_reply_deadlines WHERE id = ?", id) > 0
    }
}
