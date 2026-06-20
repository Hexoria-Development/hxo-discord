package dev.hexoria.hxo.discord.counting.listener

import dev.hexoria.hxo.discord.automod.AutomodDeleteTracker
import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.counting.CountResult
import dev.hexoria.hxo.discord.counting.CountingService
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class CountingMessageListener(
    private val countingService: CountingService,
    private val automodDeleteTracker: AutomodDeleteTracker,
) : ListenerAdapter() {

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        if (!event.isFromGuild) return

        val channelId = botConfig.channels.countingChannelId
        if (channelId == 0L || event.channel.idLong != channelId) return

        val number = event.message.contentRaw.trim().toLongOrNull()

        if (number == null) {
            automodDeleteTracker.mark(event.messageIdLong)
            event.message.delete().queue()
            return
        }

        when (countingService.tryIncrement(event.author.idLong, number)) {
            CountResult.SUCCESS -> {
                event.message.addReaction(Emoji.fromUnicode("✅")).queue()
            }

            CountResult.WRONG_NUMBER -> {
                val expected = countingService.currentCount + 1
                event.message.addReaction(Emoji.fromUnicode("❌")).queue()
                countingService.reset()
                event.channel.sendMessage(
                    "❌ ${event.author.asMention} hat das Counting bei **$number** ruiniert! " +
                    "(Erwartet: **$expected**)\nDer Count startet wieder bei **1**!"
                ).queue()
            }

            CountResult.SAME_USER -> {
                event.message.addReaction(Emoji.fromUnicode("❌")).queue()
                countingService.reset()
                event.channel.sendMessage(
                    "❌ ${event.author.asMention} hat versucht zweimal hintereinander zu zählen! " +
                    "Der Count startet wieder bei **1**!"
                ).queue()
            }
        }
    }
}
