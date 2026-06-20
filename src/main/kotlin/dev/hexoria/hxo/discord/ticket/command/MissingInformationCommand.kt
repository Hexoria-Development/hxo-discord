package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.util.*
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.TicketService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MissingInformationCommand(
    private val ticketService: TicketService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "missing-information") return

        if (!event.member.hasPermission(DiscordPermission.TICKET_CLOSE)) {
            event.replyEmbeds(errorEmbed("Keine Berechtigung", "Nur Support-Mitglieder können diesen Befehl nutzen."))
                .setEphemeral(true).queue()
            return
        }

        if (!event.channel.type.isThread) {
            event.replyEmbeds(errorEmbed("Falscher Channel", "Dieser Befehl kann nur in Ticket-Channels verwendet werden."))
                .setEphemeral(true).queue()
            return
        }

        event.deferReply(true).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Ticket geschlossen", "Dieses Ticket ist bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            event.channel.asThreadChannel().sendMessage("<@${ticket.authorId}>")
                .setEmbeds(embed {
                    setTitle("⚠️ Fehlende Informationen")
                    setDescription(
                        """
                        Dein Ticket kann aktuell nicht bearbeitet werden, da **wichtige Informationen fehlen**.

                        Bitte ergänze deine Angaben, damit wir dir weiterhelfen können.
                        Das Ticket wird erst weiterbearbeitet, sobald alle Informationen vorliegen.

                        **Folgende Informationen könnten fehlen:**
                        - Spieler-Name
                        - Koordinaten
                        - Problembeschreibung
                        - Ungenaue Angaben
                        """.trimIndent()
                    )
                    setColor(COLOR_WARNING)
                    setTimestamp(Instant.now())
                    setFooter("Ticket #${ticket.internalTicketId}")
                }).queue()

            event.hook.deleteOriginal().queue()
        }
    }
}
