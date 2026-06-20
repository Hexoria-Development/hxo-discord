package dev.hexoria.hxo.discord.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class LoggingRepository(private val jdbc: JdbcTemplate) {

    suspend fun logVoice(
        guildId: Long, userId: Long, userName: String,
        eventType: String, channelFrom: String?, channelTo: String?,
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO log_voice (guild_id, user_id, user_name, event_type, channel_from, channel_to, logged_at)
               VALUES (?, ?, ?, ?, ?, ?, ?)""",
            guildId, userId, userName, eventType, channelFrom, channelTo,
            Timestamp.valueOf(LocalDateTime.now()),
        )
    }

    suspend fun logMessage(
        guildId: Long, channelId: Long, channelName: String, messageId: Long,
        authorId: Long, authorName: String, eventType: String,
        oldContent: String?, newContent: String?,
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO log_messages
               (guild_id, channel_id, channel_name, message_id, author_id, author_name, event_type, old_content, new_content, logged_at)
               VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
            guildId, channelId, channelName, messageId, authorId, authorName,
            eventType, oldContent, newContent, Timestamp.valueOf(LocalDateTime.now()),
        )
    }

    suspend fun logMember(
        guildId: Long, userId: Long, userName: String,
        eventType: String, detail: String?,
    ) = withContext(Dispatchers.IO) {
        jdbc.update(
            """INSERT INTO log_members (guild_id, user_id, user_name, event_type, detail, logged_at)
               VALUES (?, ?, ?, ?, ?, ?)""",
            guildId, userId, userName, eventType, detail,
            Timestamp.valueOf(LocalDateTime.now()),
        )
    }
}
