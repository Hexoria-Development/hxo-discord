package dev.hiorcraft.nex.discord.automod

import dev.hiorcraft.nex.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.TimeUnit

enum class AutomodAction(val label: String) {
    LINK_BLOCKED("🔗 Link geblockt"),
    CHAT_SPAM("💬 Chat Spam"),
    VOICE_SPAM("🔊 Voice Spam"),
}

@Service
class AutomodService(
    private val automodRepository: AutomodRepository,
    private val automodDeleteTracker: AutomodDeleteTracker,
    private val coroutineScope: CoroutineScope,
) {

    fun handleMessageAction(
        message: Message,
        member: Member,
        action: AutomodAction,
        reason: String,
        timeoutSeconds: Long = 0,
    ) {
        automodDeleteTracker.mark(message.idLong)
        message.delete().queue(null) { }

        coroutineScope.launch {
            if (timeoutSeconds > 0) {
                member.timeoutFor(timeoutSeconds, TimeUnit.SECONDS).queue(null) { }
            }

            sendDm(member, action, reason, timeoutSeconds, message.guild.name)

            automodRepository.log(
                guildId        = message.guild.idLong,
                userId         = member.idLong,
                userName       = member.user.name,
                channelId      = message.channel.idLong,
                channelName    = message.channel.name,
                actionType     = action.name,
                reason         = reason,
                timeoutSeconds = timeoutSeconds,
            )
        }
    }

    fun handleVoiceAction(
        member: Member,
        guildId: Long,
        guildName: String,
        reason: String,
    ) {
        coroutineScope.launch {
            sendDm(member, AutomodAction.VOICE_SPAM, reason, 0, guildName)

            automodRepository.log(
                guildId     = guildId,
                userId      = member.idLong,
                userName    = member.user.name,
                channelId   = null,
                channelName = null,
                actionType  = AutomodAction.VOICE_SPAM.name,
                reason      = reason,
            )
        }
    }

    private fun sendDm(member: Member, action: AutomodAction, reason: String, timeoutSeconds: Long, guildName: String) {
        val description = buildString {
            appendLine("**Grund:** $reason")
            if (timeoutSeconds > 0) appendLine("**Timeout:** ${timeoutSeconds} Sekunden")
            appendLine("**Server:** $guildName")
        }

        member.user.openPrivateChannel().queue({ channel ->
            channel.sendMessageEmbeds(embed {
                setTitle(action.label)
                setDescription(description.trim())
                setColor(COLOR_ERROR)
                setTimestamp(Instant.now())
            }).queue(null) { }
        }, { })
    }
}