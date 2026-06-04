package dev.hiorcraft.nex.discord.logging.listener

import dev.hiorcraft.nex.discord.automod.*
import dev.hiorcraft.nex.discord.config.botConfig
import dev.hiorcraft.nex.discord.logging.*
import dev.hiorcraft.nex.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.MessageUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

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

            jda.getTextChannelById(logChannelId)?.sendMessageEmbeds(embed {
                setTitle("✏️ Nachricht Bearbeitet")
                setColor(COLOR_WARNING)
                setThumbnail(event.author.effectiveAvatarUrl)
                addField("Autor", "${event.author.asMention}\n`${event.author.name}`", true)
                addField("Autor ID", "`${event.author.idLong}`", true)
                if (oldContent != null)
                    addField("Vorher", oldContent.take(512), false)
                if (newContent != null)
                    addField("Nachher", newContent.take(512), false)
                addField("Kanal", "<#${event.channel.idLong}>", true)
                addField("Nachricht", "[Zum Sprung](${event.message.jumpUrl})", true)
                setFooter("Nachrichten-ID: ${event.messageId}")
                setTimestamp(Instant.now())
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

            jda.getTextChannelById(logChannelId)?.sendMessageEmbeds(embed {
                setTitle("🗑️ Nachricht Gelöscht")
                setColor(COLOR_ERROR)
                setThumbnail(cached.authorAvatar)
                addField("Autor", "<@${cached.authorId}>\n`${cached.authorName}`", true)
                addField("Autor ID", "`${cached.authorId}`", true)
                val content = cached.content.takeIf { it.isNotBlank() } ?: "*[kein Text]*"
                addField("Inhalt", content.take(1024), false)
                addField("Kanal", "<#${event.channel.idLong}>", true)
                setFooter("Nachrichten-ID: ${event.messageId}")
                setTimestamp(Instant.now())
            })?.queue()
        }
    }
}
