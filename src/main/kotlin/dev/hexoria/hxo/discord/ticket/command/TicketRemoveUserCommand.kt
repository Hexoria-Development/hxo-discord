package dev.hexoria.hxo.discord.ticket.command

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.ticket.*
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class TicketRemoveUserCommand(
    private val ticketService: TicketService,
    private val memberService: TicketMemberService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "remove") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_REMOVE)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Du hast keine Berechtigung, User aus Tickets zu entfernen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val targetUser = event.getOption("user")?.asMember ?: run {
            event.replyContainers(errorContainer("Fehler", "Kein User angegeben."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
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

            memberService.removeMember(ticket, targetUser.idLong)
            event.hook.editContainers(
                successContainer("Mitglied entfernt", "${targetUser.asMention} wurde aus dem Ticket entfernt.")
            ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }
}
