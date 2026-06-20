package dev.hexoria.hxo.discord.counting

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CountingRepository(private val jdbc: JdbcTemplate) {

    fun load(guildId: Long): Pair<Long, Long>? =
        jdbc.query(
            "SELECT current_count, last_user_id FROM counting_state WHERE guild_id = ?",
            { rs, _ -> rs.getLong("current_count") to rs.getLong("last_user_id") },
            guildId,
        ).firstOrNull()

    fun save(guildId: Long, currentCount: Long, lastUserId: Long) {
        jdbc.update(
            """INSERT INTO counting_state (guild_id, current_count, last_user_id, updated_at)
               VALUES (?, ?, ?, NOW())
               ON DUPLICATE KEY UPDATE current_count = ?, last_user_id = ?, updated_at = NOW()""",
            guildId, currentCount, lastUserId, currentCount, lastUserId,
        )
    }
}
