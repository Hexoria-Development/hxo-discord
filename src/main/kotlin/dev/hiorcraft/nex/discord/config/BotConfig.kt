package dev.hiorcraft.nex.discord.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.decodeFromStream
import dev.hiorcraft.nex.discord.automod.AutomodConfig
import dev.hiorcraft.nex.discord.selfrole.SelfRoleEntry
import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.inputStream

@ApiStatus.Internal
@Serializable
data class BotConfig(
    val botToken: String,
    val guildId: Long = 0L,
    val channels: ChannelConfig = ChannelConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val automod: AutomodConfig = AutomodConfig(),
    val selfRoles: List<SelfRoleEntry> = emptyList(),
)

@Serializable
data class ChannelConfig(
    val ticketChannelId: Long = 0L,
    val faqChannelId: Long = 0L,
    val memberLogChannelId: Long = 0L,
    val messageLogChannelId: Long = 0L,
    val voiceLogChannelId: Long = 0L,
    val countingChannelId: Long = 0L,
    val automodLogChannelId: Long = 0L,
)

@Serializable
data class DatabaseConfig(
    val host: String = "localhost",
    val port: Int = 3306,
    val database: String = "esor",
    val username: String = "root",
    val password: String = "",
)

private val CONFIG_PATH = Path("config.yml")
private val EXAMPLE_CONFIG_PATH = Path("data/example.config.yml")
private val yaml = Yaml(configuration = YamlConfiguration(strictMode = false))

val botConfig by lazy {
    if (!CONFIG_PATH.exists()) {
        if (EXAMPLE_CONFIG_PATH.exists()) {
            EXAMPLE_CONFIG_PATH.copyTo(CONFIG_PATH)
            error(
                """
                ┌─────────────────────────────────────────────────────────┐
                │  config.yml wurde automatisch aus dem Beispiel erstellt! │
                │                                                          │
                │  Bitte trage deine Werte in config.yml ein und          │
                │  starte den Bot danach erneut.                           │
                └─────────────────────────────────────────────────────────┘
                """.trimIndent()
            )
        } else {
            error(
                """
                ┌──────────────────────────────────────────────────────────┐
                │  Keine config.yml gefunden!                               │
                │                                                           │
                │  Erstelle eine config.yml im Bot-Verzeichnis.            │
                │  Eine Vorlage findest du in data/example.config.yml      │
                └──────────────────────────────────────────────────────────┘
                """.trimIndent()
            )
        }
    }

    CONFIG_PATH.inputStream().use { yaml.decodeFromStream<BotConfig>(it) }
}
