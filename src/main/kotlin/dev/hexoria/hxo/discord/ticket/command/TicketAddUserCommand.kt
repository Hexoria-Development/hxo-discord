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
class TicketAddUserCommand(
    private val ticketService: TicketService,
    private val memberService: TicketMemberService,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "add") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_ADD)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Du hast keine Berechtigung, User zu Tickets hinzuzufügen.")
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

            if (ticket.isClosed()) {
                event.hook.editContainers(
                    errorContainer("Ticket geschlossen", "Zu einem geschlossenen Ticket können keine Mitglieder hinzugefügt werden.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            memberService.addMember(ticket, targetUser, addedBy = event.member)
            event.hook.editContainers(
                successContainer("Mitglied hinzugefügt", "${targetUser.asMention} wurde dem Ticket hinzugefügt.")
            ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }
}
