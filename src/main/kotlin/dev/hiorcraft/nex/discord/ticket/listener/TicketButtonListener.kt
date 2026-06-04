package dev.hiorcraft.nex.discord.ticket.listener

import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.permission.hasPermission
import dev.hiorcraft.nex.discord.util.*
import dev.hiorcraft.nex.discord.ticket.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.selections.SelectOption
import net.dv8tion.jda.api.components.selections.StringSelectMenu
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.ZoneOffset

@Component
class TicketButtonListener(
    private val coroutineScope: CoroutineScope,
    private val ticketService: TicketService,
    private val memberService: TicketMemberService,
) : ListenerAdapter() {


    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            "ticket:panel:open"  -> handlePanelOpen(event)
            "verify:panel:open"  -> handleVerifyOpen(event)
            "ticket:claim"       -> handleClaim(event)
            "ticket:unclaim"     -> handleUnclaim(event)
            "ticket:close:btn"   -> handleCloseButton(event)
            "ticket:userinfo"    -> handleUserInfo(event)
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

        event.reply("**Welches Ticket möchtest du öffnen?**")
            .addComponents(ActionRow.of(menu))
            .setEphemeral(true)
            .queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }

    private fun handleVerifyOpen(event: ButtonInteractionEvent) {
        event.replyEmbeds(
            infoEmbed("Verify", "Dieses Feature ist noch in Arbeit und kommt bald!")
        ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }

    private fun handleClaim(event: ButtonInteractionEvent) {
        val member = event.member ?: return

        if (!member.hasPermission(DiscordPermission.TICKET_CLAIM)) {
            event.replyEmbeds(
                errorEmbed("Keine Berechtigung", "Nur Teamer können ein Ticket übernehmen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val buttonMessage = event.message
        event.deferReply(false).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.sendMessageEmbeds(
                    errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue()
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.sendMessageEmbeds(
                    errorEmbed("Ticket geschlossen", "Dieses Ticket ist bereits geschlossen.")
                ).queue()
                return@launch
            }

            if (ticket.isClaimed()) {
                event.hook.sendMessageEmbeds(
                    errorEmbed(
                        "Bereits übernommen",
                        "Dieses Ticket wurde bereits von **${ticket.claimedByName}** übernommen.\n" +
                        "Nur <@${ticket.claimedById}> kann den Claim freigeben."
                    )
                ).queue()
                return@launch
            }

            ticketService.claimTicket(ticket, member.idLong, member.user.name)
            memberService.addMember(ticket, member, silent = true)

            buttonMessage.editMessageComponents(
                ActionRow.of(
                    Button.danger("ticket:unclaim", "🔓 Claim freigeben"),
                    Button.danger("ticket:close:btn", "🔒 Schließen"),
                    Button.secondary("ticket:userinfo", "👤 User Info"),
                )
            ).queue()

            event.hook.sendMessageEmbeds(
                infoEmbed("Ticket übernommen", "${member.asMention} hat dieses Ticket übernommen. 🙋")
            ).queue()
        }
    }

    private fun handleUnclaim(event: ButtonInteractionEvent) {
        val member = event.member ?: return

        val buttonMessage = event.message
        event.deferReply(false).queue()

        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.hook.sendMessageEmbeds(
                    errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue()
                return@launch
            }

            if (ticket.claimedById != member.idLong) {
                event.hook.sendMessageEmbeds(
                    errorEmbed(
                        "Keine Berechtigung",
                        "Nur <@${ticket.claimedById}> kann den Claim dieses Tickets freigeben."
                    )
                ).queue()
                return@launch
            }

            ticketService.unclaimTicket(ticket)

            buttonMessage.editMessageComponents(
                ActionRow.of(
                    Button.success("ticket:claim", "🙋 Ticket übernehmen"),
                    Button.danger("ticket:close:btn", "🔒 Schließen"),
                    Button.secondary("ticket:userinfo", "👤 User Info"),
                )
            ).queue()

            event.hook.sendMessageEmbeds(
                infoEmbed(
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
                event.hook.editOriginalEmbeds(
                    errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            if (ticket.isClosed()) {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Bereits geschlossen", "Dieses Ticket wurde bereits geschlossen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }
            val hasPermission = member.hasPermission(DiscordPermission.TICKET_CLOSE)

            if (!hasPermission) {
                event.hook.editOriginalEmbeds(
                    errorEmbed("Keine Berechtigung", "Nur ein Teamer kann dieses Ticket schließen.")
                ).queue { event.hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            event.hook.editOriginal("Bitte wähle einen Schließ-Grund:")
                .setComponents(ActionRow.of(buildCloseReasonMenu(ticket.ticketType)))
                .queue { event.hook.deleteOriginalAfter(coroutineScope) }
        }
    }

    private fun handleUserInfo(event: ButtonInteractionEvent) {
        val member = event.member ?: return
        if (!member.hasPermission(DiscordPermission.TICKET_CLAIM)) {
            event.replyEmbeds(
                errorEmbed("Keine Berechtigung", "Nur Support-Mitglieder können User-Infos einsehen.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }
        coroutineScope.launch {
            val ticket = ticketService.getTicketByThreadId(event.channel.idLong) ?: run {
                event.replyEmbeds(errorEmbed("Kein Ticket", "Dieser Channel ist kein aktives Ticket."))
                    .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
                return@launch
            }

            val createdEpoch = ticket.createdAt.toEpochSecond(ZoneOffset.UTC)

            event.replyEmbeds(
                embed {
                    setTitle("👤 User Info")
                    setDescription("<@${ticket.authorId}> (${ticket.authorName})")
                    addField("User-ID", ticket.authorId.toString(), true)
                    addField("Ticket-Typ", "${ticket.ticketType.emoji} ${ticket.ticketType.displayName}", true)
                    addField("Erstellt", "<t:$createdEpoch:R>", true)
                    ticket.ticketData["description"]?.let { addField("Anliegen", it, false) }
                    ticket.authorAvatar?.let { setThumbnail(it) }
                    setColor(COLOR_INFO)
                    setTimestamp(Instant.now())
                }
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
        }
    }

    override fun onStringSelectInteraction(event: StringSelectInteractionEvent) {
        if (event.componentId != "ticket:type:select") return

        val typeId = event.values.firstOrNull() ?: return
        val type = TicketType.fromId(typeId) ?: run {
            event.replyEmbeds(errorEmbed("Fehler", "Ungültiger Ticket-Typ."))
                .setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
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
