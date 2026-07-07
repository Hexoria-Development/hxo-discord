package dev.hexoria.hxo.discord.faq.command

import com.github.benmanes.caffeine.cache.Caffeine
import dev.hexoria.hxo.discord.util.*
import dev.hexoria.hxo.discord.faq.Faq
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.utils.FileUpload
import org.springframework.stereotype.Component
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@Component
class FaqCommand : ListenerAdapter() {

    private val cooldownCache = Caffeine.newBuilder()
        .expireAfterWrite(30.seconds.toJavaDuration())
        .build<Long, Pair<Faq, Long>>()

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "faq") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_FAQ)) {
            event.replyEmbeds(errorEmbed("Keine Berechtigung", "Du hast keine Berechtigung, diesen Befehl zu nutzen."))
                .setEphemeral(true).queue()
            return
        }

        val question = event.getOption("question")?.asString ?: return
        val user = event.getOption("user")?.asUser
        val faq = Faq.entries.find { it.id == question }

        if (faq == null) {
            event.replyEmbeds(errorEmbed("Nicht gefunden", "Die FAQ **$question** wurde nicht gefunden."))
                .setEphemeral(true).queue()
            return
        }

        val channelId = event.messageChannel.idLong
        if (cooldownCache.asMap().any { it.value.first == faq && it.value.second == channelId }) {
            event.replyEmbeds(errorEmbed("Cooldown", "Diese FAQ wurde in diesem Channel kürzlich bereits gesendet."))
                .setEphemeral(true).queue()
            return
        }
        cooldownCache.put(System.currentTimeMillis(), faq to channelId)

        val file = faq.attachmentPath?.let(::File)
        val embedMsg = embed {
            setTitle(faq.question)
            setDescription(faq.answer)
            setColor(COLOR_INFO)
            if (file != null) setImage("attachment://${file.name}")
        }

        val reply = if (user != null) {
            event.reply(user.asMention).addEmbeds(embedMsg)
        } else {
            event.replyEmbeds(embedMsg)
        }
        if (file != null) reply.addFiles(FileUpload.fromData(file))
        reply.queue()
    }
}
