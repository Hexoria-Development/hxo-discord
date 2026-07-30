package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.TicketService
import dev.hexoria.hxo.discord.ticket.deadline.ReplyDeadlineService
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class TicketReplyDeadlineCommand(
    private val ticketService: TicketService,
    private val replyDeadlineService: ReplyDeadlineService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "reply-deadline") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_DEADLINE)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Du hast keine Berechtigung, eine Antwort-Frist zu setzen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val target = event.getOption("user")?.asUser ?: run {
            event.replyContainers(errorContainer("Fehler", "Kein User angegeben."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val hours = event.getOption("until")?.asLong ?: 24

        if (hours < 1 || hours > 8766) {
            event.replyContainers(
                errorContainer("Ungültige Zeit", "Die Frist muss zwischen 1 und 8766 Stunden (1 Jahr) liegen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.deferReply(true).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.editContainers(
                    errorContainer("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.editContainers(
                    errorContainer("Ticket geschlossen", "Das Ticket ist bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            val deadline = LocalDateTime.now().plusHours(hours)
            val deadlineEpoch = deadline.toInstant(ZoneOffset.UTC).epochSecond

            replyDeadlineService.createDeadline(ticket, target, event.user, deadline)

            event.hook.editContainers(
                successContainer("Antwort-Frist gesetzt", "Die Antwort-Frist wurde gesendet.")
            ).queue { event.hook.deleteOriginalAfter(coroutineScope) }

            event.channel.sendContainers(container {
                accentColor = COLOR_WARNING
                section(null) {
                    header("Antwort-Frist")
                    text(
                        "${target.asMention}, bitte antworte bis <t:$deadlineEpoch:F> (<t:$deadlineEpoch:R>) " +
                        "in diesem Ticket, sonst kann es geschlossen werden."
                    )
                }
                footer("Gesetzt von ${event.user.name}")
            }).queue()
        }
    }
}
