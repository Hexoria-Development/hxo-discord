package dev.hexoria.hxo.discord.automod.listener

import dev.hexoria.hxo.discord.automod.AutomodAction
import dev.hexoria.hxo.discord.automod.AutomodService
import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class ChatSpamListener(
    private val automodService: AutomodService,
) : ListenerAdapter() {

    private val timestamps = ConcurrentHashMap<Long, ArrayDeque<Long>>()

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        if (event.author.isBot) return

        val config = botConfig.automod.chatSpam
        if (!config.enabled) return
        if (event.channel.idLong in config.ignoredChannels) return
        if (event.member.hasPermission(DiscordPermission.AUTOMOD_BYPASS)) return

        val now = System.currentTimeMillis()
        val windowMs = config.timeWindowSeconds * 1000L

        val userTimestamps = timestamps.getOrPut(event.author.idLong) { ArrayDeque() }
        val isSpam: Boolean
        synchronized(userTimestamps) {
            userTimestamps.removeAll { now - it > windowMs }
            userTimestamps.addLast(now)
            isSpam = userTimestamps.size > config.maxMessages
            if (isSpam) userTimestamps.clear()
        }

        if (!isSpam) return

        automodService.handleMessageAction(
            message        = event.message,
            member         = event.member!!,
            action         = AutomodAction.CHAT_SPAM,
            reason         = "Zu viele Nachrichten in kurzer Zeit (${config.maxMessages} in ${config.timeWindowSeconds}s)",
            timeoutSeconds = config.timeoutSeconds,
        )
    }
}