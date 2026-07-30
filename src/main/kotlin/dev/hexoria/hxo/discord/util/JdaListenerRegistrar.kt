package dev.hexoria.hxo.discord.util

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.faq.Faq
import dev.hexoria.hxo.discord.ticket.ticketPanelContainer
import dev.hexoria.hxo.discord.voice.voiceInfoContainer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.Command.Choice
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class JdaListenerRegistrar(
    private val jda: JDA,
    private val listeners: List<ListenerAdapter>,
    @Autowired(required = false)
    private val ticketChannel: TextChannel?,
) {
    private val logger = componentLogger<JdaListenerRegistrar>()

    @EventListener(ApplicationReadyEvent::class)
    fun onReady() {
        jda.addEventListener(*listeners.toTypedArray())
        logger.info("${listeners.size} JDA-Listener registriert.")

        // Globale Commands löschen, damit keine Duplikate entstehen
        jda.updateCommands().queue {
            logger.info("Globale Slash-Commands geleert.")
        }

        val guild = jda.getGuildById(botConfig.guildId)
        if (guild == null) {
            logger.error("Guild mit ID ${botConfig.guildId} nicht gefunden – Slash-Commands konnten nicht registriert werden!")
            return
        }

        val faqChoices = Faq.entries.map { Choice(it.id, it.id) }

        guild.updateCommands().addCommands(
            Commands.slash("close", "Schließt das aktuelle Ticket"),

            Commands.slash("add", "Fügt einen User zum Ticket hinzu")
                .addOption(OptionType.USER, "user", "Der User, der hinzugefügt werden soll", true),

            Commands.slash("add-silent", "Fügt einen User still zum Ticket hinzu (ohne Benachrichtigung, nur Admins)")
                .addOption(OptionType.USER, "user", "Der User, der hinzugefügt werden soll", true),

            Commands.slash("remove", "Entfernt einen User aus dem Ticket")
                .addOption(OptionType.USER, "user", "Der User, der entfernt werden soll", true),

            Commands.slash("ticket-panel", "Postet das Ticket-Panel (nur Admins)"),

            Commands.slash("missing-information", "Informiert den Ticket-Ersteller über fehlende Angaben"),

            Commands.slash("reply-deadline", "Setzt eine Antwort-Frist für einen User im Ticket")
                .addOption(OptionType.USER, "user", "Der User, für den die Antwort-Frist gilt", true)
                .addOption(OptionType.INTEGER, "until", "Frist in Stunden (Standard: 24)", false),

            Commands.slash("deadline-notify", "Schaltet die DM-Benachrichtigung bei abgelaufenen Antwort-Fristen um"),

            Commands.slash("selfrole-panel", "Postet das Self-Role Panel im aktuellen Channel (nur Admins)"),

            Commands.slash("reactionrole-panel", "Postet das Reaction-Role Panel im aktuellen Channel (nur Admins)"),

            Commands.slash("voice-panel", "Postet die Willkommens-Nachricht des Voice-Systems (nur Admins)"),

            Commands.slash("faq", "Häufig gestellte Fragen anzeigen")
                .addOptions(
                    OptionData(OptionType.STRING, "question", "Die Frage, die angezeigt werden soll", true)
                        .addChoices(faqChoices)
                )
                .addOption(OptionType.USER, "user", "Der Benutzer, für den die Antwort angezeigt wird", false),
        ).queue { cmds ->
            logger.info("${cmds.size} Guild-Slash-Commands in '${guild.name}' registriert: ${cmds.map { it.name }}")
        }

        autoPostTicketPanel()
        autoPostVoicePanel()
    }

    private fun autoPostVoicePanel() {
        val config = botConfig.tempVoice
        if (!config.enabled || config.infoChannelId == 0L) return

        val channel = jda.getTextChannelById(config.infoChannelId) ?: run {
            logger.error("Voice-Info-Channel mit ID ${config.infoChannelId} nicht gefunden!")
            return
        }

        val history = try {
            channel.history.retrievePast(10).complete()
        } catch (e: Exception) {
            logger.error("Fehler beim Abrufen der Channel-History: ${e.message}")
            return
        }

        val selfId = jda.selfUser.idLong
        if (history.any { it.author.idLong == selfId && it.components.isNotEmpty() }) {
            logger.info("Voice-Panel bereits im Channel vorhanden – kein erneutes Posten.")
            return
        }

        channel.sendContainers(voiceInfoContainer(config.creatorChannelId)).queue {
            logger.info("Voice-Panel automatisch in #${channel.name} gepostet.")
        }
    }

    private fun autoPostTicketPanel() {
        val channel = ticketChannel ?: run {
            logger.warn("Kein Ticket-Channel konfiguriert – Panel wird nicht automatisch gepostet.")
            return
        }

        val history = try {
            channel.history.retrievePast(10).complete()
        } catch (e: Exception) {
            logger.error("Fehler beim Abrufen der Channel-History: ${e.message}")
            return
        }

        val selfId = jda.selfUser.idLong
        val panelExists = history.any { msg ->
            msg.author.idLong == selfId && msg.components.isNotEmpty()
        }

        if (panelExists) {
            logger.info("Ticket-Panel bereits im Channel vorhanden – kein erneutes Posten.")
            return
        }

        channel.sendContainers(ticketPanelContainer()).queue {
            logger.info("Ticket-Panel automatisch in #${channel.name} gepostet.")
        }
    }
}
