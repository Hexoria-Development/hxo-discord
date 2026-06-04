package dev.hiorcraft.nex.discord.util

import dev.hiorcraft.nex.discord.config.botConfig
import dev.hiorcraft.nex.discord.faq.Faq
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
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
import java.time.Instant

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

            Commands.slash("remove", "Entfernt einen User aus dem Ticket")
                .addOption(OptionType.USER, "user", "Der User, der entfernt werden soll", true),

            Commands.slash("ticket-panel", "Postet das Ticket-Panel (nur Admins)"),

            Commands.slash("missing-information", "Informiert den Ticket-Ersteller über fehlende Angaben"),

            Commands.slash("selfrole-panel", "Postet das Self-Role Panel im aktuellen Channel (nur Admins)"),

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

        val panelEmbed = embed {
            setTitle("🎫 Support")
            setDescription(
                """
                Willkommen in Beim Support
                Erstelle ein Support-Ticket
                Wenn du ein Anliegen hast, dann kannst du hier ein Ticket öffnen und unsere Teammitglieder werden dir dann weiterhelfen.

                Dafür musst du einfach aus dem Menü unten eine Option wählen, woraufhin ein Channel geöffnet wird. Bitte beschreibe dort dein Anliegen.

                Bei **Verify** ist noch nicht fertig!
                """.trimIndent()
            )
            setColor(COLOR_INFO)
            setTimestamp(Instant.now())
            setFooter("Support-System")
        }

        val row = ActionRow.of(
            Button.success("ticket:panel:open", "🎫 Ticket öffnen"),
            Button.secondary("verify:panel:open", "✅ Verify"),
        )

        channel.sendMessageEmbeds(panelEmbed).setComponents(row).queue {
            logger.info("Ticket-Panel automatisch in #${channel.name} gepostet.")
        }
    }
}
