package dev.hiorcraft.nex.discord.faq.command

import com.github.benmanes.caffeine.cache.Caffeine
import dev.hiorcraft.nex.discord.util.*
import dev.hiorcraft.nex.discord.faq.Faq
import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.permission.hasPermission
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

        if (user != null) {
            val message = event.channel.sendMessage(user.asMention).setEmbeds(embedMsg)
            if (file != null) message.addFiles(FileUpload.fromData(file))
            message.queue()
        } else {
            val message = event.channel.sendMessageEmbeds(embedMsg)
            if (file != null) message.addFiles(FileUpload.fromData(file))
            message.queue()
        }

        event.replyEmbeds(successEmbed("FAQ gesendet", "Die FAQ wurde erfolgreich gepostet."))
            .setEphemeral(true).queue()
    }
}
