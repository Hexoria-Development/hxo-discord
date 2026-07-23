package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.util.COLOR_INFO
import dev.hexoria.hxo.discord.util.container
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.replacer.ComponentReplacer
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.entities.Message

const val TICKET_PANEL_BUTTON = "ticket:panel:open"
const val TICKET_CLAIM_BUTTON = "ticket:claim"
const val TICKET_UNCLAIM_BUTTON = "ticket:unclaim"
const val TICKET_CLOSE_BUTTON = "ticket:close:btn"
const val TICKET_USERINFO_BUTTON = "ticket:userinfo"

/** Buttons unter dem Ticket-Container. Vor dem Claim kann übernommen, danach freigegeben werden. */
fun ticketActionRow(claimed: Boolean): ActionRow = ActionRow.of(
    if (claimed) Button.danger(TICKET_UNCLAIM_BUTTON, "🔓 Claim freigeben")
    else Button.success(TICKET_CLAIM_BUTTON, "🙋 Ticket übernehmen"),
    Button.danger(TICKET_CLOSE_BUTTON, "🔒 Schließen"),
    Button.secondary(TICKET_USERINFO_BUTTON, "👤 User Info"),
)

/** Das Panel, über das Tickets erstellt werden – wird beim Start und per `/ticket-panel` gepostet. */
fun ticketPanelContainer(): Container = container {
    accentColor = COLOR_INFO
    header("Ticket erstellen")
    text(
        """
        Du möchtest einen Spieler bzw. ein Problem melden oder einen Entbannungsantrag für den Server erstellen, so kannst du hier ein Ticket erstellen.

        Bitte mache dich vorher mit den unterschiedlichen Tickettypen vertraut!
        Die Übersicht findest du hier: https://hexoria.net/Support

        Allgemeine Fragen sollten in den dafür vorgesehenen öffentlichen Kanälen gestellt werden.

        Wir bemühen uns die Tickets schnellstmöglich zu bearbeiten, jedoch arbeitet das gesamte Team freiwillig, und gerade unter der Woche kann die Bearbeitung der Tickets länger dauern.
        """.trimIndent()
    )
    divider(Separator.Spacing.LARGE)
    buttons(Button.success(TICKET_PANEL_BUTTON, "🎫 Ticket öffnen"))
    footer("Support-System")
}

/**
 * Baut den Ticket-Container, der beim Erstellen im Thread gepinnt wird.
 * Die Felder liefert der jeweilige [TicketType].
 */
fun buildTicketContainer(ticket: Ticket, claimed: Boolean = false): Container {
    val type = ticket.ticketType
    val number = ticket.internalTicketId?.let { " – Ticket #$it" } ?: ""

    return container {
        accentColor = COLOR_INFO
        section(ticket.authorAvatar) {
            header("${type.emoji} ${type.displayName}$number")
            text(type.welcomeText(ticket.authorId))
        }
        for (entry in type.displayFields(ticket.ticketData)) {
            divider(Separator.Spacing.LARGE)
            field(entry.label, entry.value)
        }
        divider(Separator.Spacing.LARGE)
        row(ticketActionRow(claimed))
        footer("Ticket-ID: ${ticket.ticketId}")
    }
}

/**
 * Tauscht die Button-Reihe einer Ticket-Nachricht aus und lässt den restlichen Container unberührt.
 */
fun Message.updateTicketActionRow(claimed: Boolean) {
    val tree = componentTree.replace(
        ComponentReplacer.of(ActionRow::class.java, { _: ActionRow -> true }, { ticketActionRow(claimed) })
    )
    editMessageComponents(tree).useComponentsV2().queue()
}
