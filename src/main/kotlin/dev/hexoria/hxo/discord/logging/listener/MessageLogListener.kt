package dev.hexoria.hxo.discord.logging.listener

import dev.hexoria.hxo.discord.automod.*
import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.logging.*
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class MessageLogListener(
    private val jda: JDA,
    private val loggingRepository: LoggingRepository,
    private val messageCache: MessageCache,
    private val automodDeleteTracker: AutomodDeleteTracker,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        if (event.author.isBot) return
        messageCache.put(
            event.messageIdLong,
            CachedMessage(
                authorId = event.author.idLong,
                authorName = event.author.name,
                authorAvatar = event.author.effectiveAvatarUrl,
                content = event.message.contentDisplay,
                channelId = event.channel.idLong,
                channelName = event.channel.name,
                isBot = false,
            )
        )
    }

    override fun onMessageUpdate(event: MessageUpdateEvent) {
        if (!event.isFromGuild) return
        if (event.author.isBot) return
        val logChannelId = botConfig.channels.messageLogChannelId
        if (logChannelId == 0L) return

        val cached = messageCache.get(event.messageIdLong)
        val oldContent = cached?.content?.takeIf { it.isNotBlank() }
        val newContent = event.message.contentDisplay.takeIf { it.isNotBlank() }

        if (oldContent != null && oldContent == newContent) return

        messageCache.put(
            event.messageIdLong,
            CachedMessage(
                authorId    = event.author.idLong,
                authorName  = event.author.name,
                authorAvatar = event.author.effectiveAvatarUrl,
                content     = event.message.contentDisplay,
                channelId   = event.channel.idLong,
                channelName = event.channel.name,
                isBot       = false,
            )
        )

        coroutineScope.launch {
            loggingRepository.logMessage(
                guildId     = event.guild.idLong,
                channelId   = event.channel.idLong,
                channelName = event.channel.name,
                messageId   = event.messageIdLong,
                authorId    = event.author.idLong,
                authorName  = event.author.name,
                eventType   = "EDIT",
                oldContent  = oldContent,
                newContent  = newContent,
            )

            jda.getTextChannelById(logChannelId)?.sendSilentContainers(container {
                accentColor = COLOR_WARNING
                section(event.author.effectiveAvatarUrl) {
                    header("✏️ Nachricht Bearbeitet")
                    text("${event.author.asMention}\n`${event.author.name}` • `${event.author.idLong}`")
                }
                divider()
                field("Vorher", oldContent?.take(512))
                field("Nachher", newContent?.take(512))
                field("Kanal", "<#${event.channel.idLong}> • [Zum Sprung](${event.message.jumpUrl})")
                footer("Nachrichten-ID: ${event.messageId}", now)
            })?.queue()
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        if (automodDeleteTracker.consume(event.messageIdLong)) return

        val logChannelId = botConfig.channels.messageLogChannelId
        if (logChannelId == 0L) return

        val cached = messageCache.get(event.messageIdLong) ?: return
        messageCache.invalidate(event.messageIdLong)

        coroutineScope.launch {
            loggingRepository.logMessage(
                guildId     = event.guild.idLong,
                channelId   = event.channel.idLong,
                channelName = event.channel.name,
                messageId   = event.messageIdLong,
                authorId    = cached.authorId,
                authorName  = cached.authorName,
                eventType   = "DELETE",
                oldContent  = cached.content,
                newContent  = null,
            )

            jda.getTextChannelById(logChannelId)?.sendSilentContainers(container {
                accentColor = COLOR_ERROR
                section(cached.authorAvatar) {
                    header("🗑️ Nachricht Gelöscht")
                    text("<@${cached.authorId}>\n`${cached.authorName}` • `${cached.authorId}`")
                }
                divider()
                field("Inhalt", (cached.content.takeIf { it.isNotBlank() } ?: "*[kein Text]*").take(1024))
                field("Kanal", "<#${event.channel.idLong}>")
                footer("Nachrichten-ID: ${event.messageId}", now)
            })?.queue()
        }
    }
}
