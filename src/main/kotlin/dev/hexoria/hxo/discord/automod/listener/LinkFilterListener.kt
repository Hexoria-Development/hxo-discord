package dev.hexoria.hxo.discord.automod.listener


import dev.hexoria.hxo.discord.automod.AutomodAction
import dev.hexoria.hxo.discord.automod.AutomodService
import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.net.URI

@Component
class LinkFilterListener(
    private val automodService: AutomodService,
) : ListenerAdapter() {

    private val urlRegex = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.isFromGuild) return
        if (event.author.isBot) return

        val config = botConfig.automod.linkFilter
        if (!config.enabled) return
        if (event.channel.idLong in config.ignoredChannels) return
        if (event.member.hasPermission(DiscordPermission.AUTOMOD_BYPASS)) return

        val urls = urlRegex.findAll(event.message.contentRaw).map { it.value }.toList()
        if (urls.isEmpty()) return

        val blocked = urls.filter { url -> !isAllowed(url, config.allowedDomains) }
        if (blocked.isEmpty()) return

        automodService.handleMessageAction(
            message        = event.message,
            member         = event.member!!,
            action         = AutomodAction.LINK_BLOCKED,
            reason         = "Nicht erlaubter Link: ${blocked.first().take(100)}",
            timeoutSeconds = 0,
        )
    }

    private fun isAllowed(url: String, allowedEntries: List<String>): Boolean {
        if (allowedEntries.isEmpty()) return false
        val uri = runCatching { URI(url) }.getOrElse { return false }
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return false
        val path = uri.path?.lowercase() ?: ""

        return allowedEntries.any { entry ->
            val clean = entry.lowercase().removePrefix("www.")
            if ('/' in clean) {
                val entryHost = clean.substringBefore('/')
                val entryPath = "/${clean.substringAfter('/')}".trimEnd('/')
                val hostMatches = host == entryHost || host.endsWith(".$entryHost")
                val pathMatches = path.trimEnd('/') == entryPath || path.startsWith("$entryPath/")
                hostMatches && pathMatches
            } else {
                host == clean || host.endsWith(".$clean")
            }
        }
    }
}