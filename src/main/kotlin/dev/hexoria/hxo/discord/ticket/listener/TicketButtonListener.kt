package dev.hexoria.hxo.discord.ticket.listener

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.util.*
import dev.hexoria.hxo.discord.ticket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.ZoneOffset

@Component
class TicketButtonListener(
    private val coroutineScope: CoroutineScope,
    private val ticketService: TicketService,
    private val memberService: TicketMemberService,
) : ListenerAdapter() {


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            TICKET_PANEL_BUTTON    -> handlePanelOpen(event)
            "verify:panel:open"    -> handleVerifyOpen(event)
            TICKET_CLAIM_BUTTON    -> handleClaim(event)
            TICKET_UNCLAIM_BUTTON  -> handleUnclaim(event)
            TICKET_CLOSE_BUTTON    -> handleCloseButton(event)
            TICKET_USERINFO_BUTTON -> handleUserInfo(event)
        }
    }

    private fun handlePanelOpen(event: ButtonInteractionEvent) {
        val options = TicketType.entries.map { type ->
            SelectOption.of("${type.emoji} ${type.displayName}", type.id)
                .withDescription(type.description)
        }

        val menu = StringSelectMenu.create("ticket:type:select")
            .setPlaceholder("Wähle eine Kategorie …")
            .addOptions(options)
            .build()

        event.replyContainers(container {
            accentColor = COLOR_INFO
            text("**Welches Ticket möchtest du öffnen?**")
            buttons(menu)
        }).setEphemeral(true)
            .queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }

    private fun handleVerifyOpen(event: ButtonInteractionEvent) {
        event.replyContainers(
            infoContainer("Verify", "Dieses Feature ist noch in Arbeit und kommt bald!")
        ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }

    private fun handleClaim(event: ButtonInteractionEvent) {
        val member = event.member ?: return

        if (!member.hasPermission(DiscordPermission.TICKET_CLAIM)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Nur Teamer können ein Ticket übernehmen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val buttonMessage = event.message
        event.deferReply(false).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.sendContainers(
                    errorContainer("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue()
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.sendContainers(
                    errorContainer("Ticket geschlossen", "Dieses Ticket ist bereits geschlossen.")
                ).queue()
                return@launch
            }

            if (ticket.isClaimed()) {
                event.hook.sendContainers(
                    errorContainer(
                        "Bereits übernommen",
                        "Dieses Ticket wurde bereits von **${ticket.claimedByName}** übernommen.\n" +
                        "Nur <@${ticket.claimedById}> kann den Claim freigeben."
                    )
                ).queue()
                return@launch
            }

            ticketService.claimTicket(ticket, member.idLong, member.user.name)
            memberService.addMember(ticket, member, silent = true)

            buttonMessage.updateTicketActionRow(claimed = true)

            event.hook.sendContainers(
                infoContainer("Ticket übernommen", "${member.asMention} hat dieses Ticket übernommen. 🙋")
            ).queue()
        }
    }

    private fun handleUnclaim(event: ButtonInteractionEvent) {
        val member = event.member ?: return

        val buttonMessage = event.message
        event.deferReply(false).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.sendContainers(
                    errorContainer("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue()
                return@launch
            }

            if (ticket.claimedById != member.idLong) {
                event.hook.sendContainers(
                    errorContainer(
                        "Keine Berechtigung",
                        "Nur <@${ticket.claimedById}> kann den Claim dieses Tickets freigeben."
                    )
                ).queue()
                return@launch
            }

            ticketService.unclaimTicket(ticket)

            buttonMessage.updateTicketActionRow(claimed = false)

            event.hook.sendContainers(
                infoContainer(
                    "Claim freigegeben",
                    "${member.asMention} hat den Claim freigegeben. Das Ticket kann erneut übernommen werden."
                )
            ).queue()
        }
    }

    private fun handleCloseButton(event: ButtonInteractionEvent) {
        val member = event.member ?: return
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
                    errorContainer("Bereits geschlossen", "Dieses Ticket wurde bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }
            val hasPermission = member.hasPermission(DiscordPermission.TICKET_CLOSE)

            if (!hasPermission) {
                event.hook.editContainers(
                    errorContainer("Keine Berechtigung", "Nur ein Teamer kann dieses Ticket schließen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            event.hook.editContainers(container {
                accentColor = COLOR_INFO
                text("Bitte wähle einen Schließ-Grund:")
                buttons(buildCloseReasonMenu(ticket.ticketType))
            }).queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }

    private fun handleUserInfo(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        if (!member.hasPermission(DiscordPermission.TICKET_CLAIM)) {
            event.replyContainers(
                errorContainer("Keine Berechtigung", "Nur Support-Mitglieder können User-Infos einsehen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }
        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.replyContainers(errorContainer("Kein Ticket", "Dieser Channel ist kein aktives Ticket."))
                    .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            val createdEpoch = ticket.createdAt.toEpochSecond(ZoneOffset.UTC)

            event.replyContainers(
                container {
                    accentColor = COLOR_INFO
                    section(ticket.authorAvatar) {
                        header("👤 User Info")
                        text("<@${ticket.authorId}> (${ticket.authorName})")
                    }
                    divider()
                    field("User-ID", ticket.authorId.toString())
                    field("Ticket-Typ", "${ticket.ticketType.emoji} ${ticket.ticketType.displayName}")
                    field("Erstellt", "<t:$createdEpoch:R>")
                    field("Anliegen", ticket.ticketData["description"])
                }
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.componentId != "ticket:type:select") return

        val typeId = event.values.firstOrNull() ?: return
        val type = TicketType.fromId(typeId) ?: run {
            event.replyContainers(errorContainer("Fehler", "Ungültiger Ticket-Typ."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        if (type == TicketType.CONTENT_SUPPORT && !event.member.hasPermission(DiscordPermission.TICKET_CONTENT_CREATE)) {
            event.replyContainers(
                errorContainer("Kein Zugriff", "Nur Content Creator können ein Content Support Ticket erstellen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        event.replyModal(type.createModal()).queue()
    }

    private fun buildCloseReasonMenu(type: TicketType): StringSelectMenu {
        val builder = StringSelectMenu.create("ticket:close:reason")
            .setPlaceholder("Grund auswählen...")
        type.closeReasons.forEach { reason ->
            builder.addOption(reason.displayName, "${type.id}:${reason.id}", reason.description)
        }
        return builder.build()
    }
}
