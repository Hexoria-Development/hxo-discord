package dev.hexoria.hxo.discord.ticket.deadline

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class DeadlineNotifyRepository(private val jdbc: JdbcTemplate) {

    /** Ohne Eintrag sind Benachrichtigungen standardmäßig aktiviert. */
    suspend fun isEnabled(userId: Long): Boolean = withContext(Dispatchers.IO) {
        jdbc.query(
            "SELECT enabled FROM deadline_notify WHERE user_id = ?",
            { rs, _ -> rs.getBoolean("enabled") },
            userId,
        ).firstOrNull() ?: true
    }

    suspend fun setEnabled(userId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO deadline_notify (user_id, enabled) VALUES (?, ?)
               ON DUPLICATE KEY UPDATE enabled = VALUES(enabled)""",
            userId, enabled,
        )
    }
}
