package dev.hiorcraft.nex.discord.ticket.command

import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.permission.hasPermission
import dev.hiorcraft.nex.discord.util.*
import kotlinx.coroutines.CoroutineScope
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class PrintTicketPanelCommand(
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "ticket-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_TICKET_BUTTONS)) {
            event.replyEmbeds(
                errorEmbed("Keine Berechtigung", "Du benötigst die Admin- oder Dev-Rolle für diesen Befehl.")
            ).setEphemeral(true).queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
            return
        }

        val panelEmbed = embed {
            setTitle("Ticket erstellen")
            setDescription(
                """
                Du möchtest einen Spieler bzw. ein Problem melden oder einen Entbannungsantrag für den Server erstellen, so kannst du hier ein Ticket erstellen.

                Bitte mache dich vorher mit den unterschiedlichen Tickettypen vertraut!
                Die Übersicht findest du hier: https://hexoria.net/Support

                Allgemeine Fragen sollten in den dafür vorgesehenen öffentlichen Kanälen gestellt werden.

                Wir bemühen uns die Tickets schnellstmöglich zu bearbeiten, jedoch arbeitet das gesamte Team freiwillig, und gerade unter der Woche kann die Bearbeitung der Tickets länger dauern.
                """.trimIndent()
            )
            setColor(COLOR_INFO)
            setTimestamp(Instant.now())
            setFooter("Support-System")
        }

        val row = ActionRow.of(
            Button.success("ticket:panel:open", "🎫 Ticket öffnen"),
        )

        event.channel.sendMessageEmbeds(panelEmbed)
            .setComponents(row)
            .queue()

        event.reply("✅ Ticket-Panel wurde gepostet.")
            .setEphemeral(true)
            .queue { hook -> hook.deleteOriginalAfter(coroutineScope) }
    }
}
