package dev.hexoria.hxo.discord.ticket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.util.*

@Repository
class TicketMemberRepository(private val jdbc: JdbcTemplate) {

    suspend fun addMember(ticketId: UUID, userId: Long, userName: String) = withContext(Dispatchers.IO) {
        jdbc.update(
            "INSERT IGNORE INTO ticket_members (ticket_id, user_id, user_name) VALUES (?, ?, ?)",
            ticketId.toString(), userId, userName,
        )
    }

    suspend fun removeMember(ticketId: UUID, userId: Long) = withContext(Dispatchers.IO) {
        jdbc.update(
            "DELETE FROM ticket_members WHERE ticket_id = ? AND user_id = ?",
            ticketId.toString(), userId,
        )
    }

    suspend fun isMember(ticketId: UUID, userId: Long): Boolean = withContext(Dispatchers.IO) {
        val count = jdbc.queryForObject(
            "SELECT COUNT(*) FROM ticket_members WHERE ticket_id = ? AND user_id = ?",
            Int::class.java, ticketId.toString(), userId,
        ) ?: 0
        count > 0
    }

    suspend fun getMembersOf(ticketId: UUID): List<Long> = withContext(Dispatchers.IO) {
        jdbc.queryForList(
            "SELECT user_id FROM ticket_members WHERE ticket_id = ?",
            Long::class.java, ticketId.toString(),
        )
    }

    suspend fun removeAll(ticketId: UUID) = withContext(Dispatchers.IO) {
        jdbc.update("DELETE FROM ticket_members WHERE ticket_id = ?", ticketId.toString())
    }
}
