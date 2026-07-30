package dev.hexoria.hxo.discord.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

@Configuration
class DatabaseConfiguration {

    @Bean
    fun dataSource(): DataSource {
        val db = botConfig.database
        return HikariDataSource(HikariConfig().apply {
            jdbcUrl = "jdbc:mysql://${db.host}:${db.port}/${db.database}" +
                "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8"
            username = db.username
            password = db.password
            maximumPoolSize = 5
        })
    }

    @Bean
    fun jdbcTemplate(dataSource: DataSource): JdbcTemplate {
        val jdbc = JdbcTemplate(dataSource)
        initSchema(jdbc)
        return jdbc
    }

    private fun migrateTicketsTable(jdbc: JdbcTemplate) {
        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS tickets (
                ticket_id        VARCHAR(36)  NOT NULL,
                thread_id        BIGINT       NOT NULL,
                guild_id         BIGINT       NOT NULL,
                author_id        BIGINT       NOT NULL,
                author_name      VARCHAR(255) NOT NULL,
                author_avatar    VARCHAR(255),
                ticket_type      VARCHAR(50)  NOT NULL,
                ticket_data      TEXT         NOT NULL,
                internal_ticket_id BIGINT,
                created_at       DATETIME     NOT NULL,
                claimed_by_id    BIGINT,
                claimed_by_name  VARCHAR(255),
                closed_at        DATETIME,
                closed_by_id     BIGINT,
                closed_by_name   VARCHAR(255),
                closed_by_avatar VARCHAR(255),
                closed_reason    TEXT,
                PRIMARY KEY (ticket_id),
                INDEX idx_t_thread_id (thread_id),
                INDEX idx_t_author_id (author_id)
            )
        """.trimIndent())
    }

    private fun initSchema(jdbc: JdbcTemplate) {
        migrateTicketsTable(jdbc)

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ticket_members (
                ticket_id  VARCHAR(36)  NOT NULL,
                user_id    BIGINT       NOT NULL,
                user_name  VARCHAR(255) NOT NULL,
                PRIMARY KEY (ticket_id, user_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS log_voice (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id     BIGINT       NOT NULL,
                user_id      BIGINT       NOT NULL,
                user_name    VARCHAR(255) NOT NULL,
                event_type   VARCHAR(20)  NOT NULL,
                channel_from VARCHAR(255),
                channel_to   VARCHAR(255),
                logged_at    DATETIME     NOT NULL,
                INDEX idx_lv_guild (guild_id),
                INDEX idx_lv_user  (user_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS log_messages (
                id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id     BIGINT        NOT NULL,
                channel_id   BIGINT        NOT NULL,
                channel_name VARCHAR(255)  NOT NULL,
                message_id   BIGINT        NOT NULL,
                author_id    BIGINT        NOT NULL,
                author_name  VARCHAR(255)  NOT NULL,
                event_type   VARCHAR(20)   NOT NULL,
                old_content  TEXT,
                new_content  TEXT,
                logged_at    DATETIME      NOT NULL,
                INDEX idx_lm_guild  (guild_id),
                INDEX idx_lm_author (author_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS log_members (
                id         BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id   BIGINT       NOT NULL,
                user_id    BIGINT       NOT NULL,
                user_name  VARCHAR(255) NOT NULL,
                event_type VARCHAR(30)  NOT NULL,
                detail     TEXT,
                logged_at  DATETIME     NOT NULL,
                INDEX idx_lmem_guild (guild_id),
                INDEX idx_lmem_user  (user_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ticket_messages (
                id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                ticket_id   VARCHAR(36)  NOT NULL,
                message_id  BIGINT       NOT NULL UNIQUE,
                author_id   BIGINT       NOT NULL,
                author_name VARCHAR(255) NOT NULL,
                content     TEXT,
                attachments TEXT,
                sent_at     DATETIME     NOT NULL,
                INDEX idx_tm_ticket_id (ticket_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS automod_log (
                id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                guild_id        BIGINT        NOT NULL,
                user_id         BIGINT        NOT NULL,
                user_name       VARCHAR(255)  NOT NULL,
                channel_id      BIGINT,
                channel_name    VARCHAR(255),
                action_type     VARCHAR(50)   NOT NULL,
                reason          TEXT          NOT NULL,
                timeout_seconds BIGINT        NOT NULL DEFAULT 0,
                logged_at       DATETIME      NOT NULL,
                INDEX idx_al_guild (guild_id),
                INDEX idx_al_user  (user_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS ticket_reply_deadlines (
                id               BIGINT AUTO_INCREMENT PRIMARY KEY,
                ticket_id        VARCHAR(36)  NOT NULL,
                thread_id        BIGINT       NOT NULL,
                target_user_id   BIGINT       NOT NULL,
                target_user_name VARCHAR(255) NOT NULL,
                set_by_id        BIGINT       NOT NULL,
                set_by_name      VARCHAR(255) NOT NULL,
                deadline         DATETIME     NOT NULL,
                INDEX idx_trd_thread   (thread_id),
                INDEX idx_trd_deadline (deadline)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS deadline_notify (
                user_id BIGINT  NOT NULL,
                enabled BOOLEAN NOT NULL DEFAULT TRUE,
                PRIMARY KEY (user_id)
            )
        """.trimIndent())

        jdbc.execute("""
            CREATE TABLE IF NOT EXISTS counting_state (
                guild_id      BIGINT   NOT NULL,
                current_count BIGINT   NOT NULL DEFAULT 0,
                last_user_id  BIGINT   NOT NULL DEFAULT 0,
                updated_at    DATETIME NOT NULL,
                PRIMARY KEY (guild_id)
            )
        """.trimIndent())
    }
}
