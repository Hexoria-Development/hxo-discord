package dev.hiorcraft.nex.discord.automod

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class AutomodRepository(private val jdbc: JdbcTemplate) {

    suspend fun log(
        guildId: Long,
        userId: Long,
        userName: String,
        channelId: Long?,
        channelName: String?,
        actionType: String,
        reason: String,
        timeoutSeconds: Long = 0,
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO automod_log
               (guild_id, user_id, user_name, channel_id, channel_name, action_type, reason, timeout_seconds, logged_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            guildId, userId, userName, channelId, channelName,
            actionType, reason, timeoutSeconds,
            Timestamp.valueOf(LocalDateTime.now()),
        )
    }
}