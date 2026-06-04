package dev.hiorcraft.nex.discord.ticket.command

import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.permission.hasPermission
import dev.hiorcraft.nex.discord.ticket.TicketDeadlineService
import dev.hiorcraft.nex.discord.ticket.TicketService
import dev.hiorcraft.nex.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class TicketDeadlineCommand(
    private val ticketService: TicketService,
    private val deadlineService: TicketDeadlineService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "deadline") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_DEADLINE)) {
            event.replyEmbeds(
                errorEmbed("Keine Berechtigung", "Du hast keine Berechtigung, einen Antwort-Timer zu setzen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val minutes = event.getOption("minuten")?.asLong ?: run {
            event.replyEmbeds(errorEmbed("Fehler", "Keine Minutenanzahl angegeben."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        if (minutes < 1 || minutes > 10080) {
            event.replyEmbeds(errorEmbed("Ungültige Zeit", "Die Zeit muss zwischen 1 und 10080 Minuten (7 Tage) liegen."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.deferReply(false).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Ticket geschlossen", "Das Ticket ist bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            deadlineService.schedule(ticket, minutes)

            val deadlineTimestamp = Instant.now().plusSeconds(minutes * 60)

            event.hook.editOriginalEmbeds(embed {
                setTitle("⏰ Antwort-Timer gesetzt")
                setColor(COLOR_WARNING)
                setDescription(
                    """
                    <@${ticket.authorId}>, du hast **$minutes Minuten** Zeit zu antworten.

                    Wenn bis <t:${deadlineTimestamp.epochSecond}:F> keine Antwort eingeht, wird das Ticket automatisch geschlossen.
                    """.trimIndent()
                )
                setTimestamp(Instant.now())
                setFooter("Gesetzt von ${event.member!!.user.name}", event.member!!.user.effectiveAvatarUrl)
            }).queue()
        }
    }
}
