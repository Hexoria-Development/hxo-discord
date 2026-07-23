package dev.hexoria.hxo.discord.selfrole.command

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class SelfRolePanelCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "selfrole-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_SELFROLE_PANEL)) {
            event.replyContainers(errorContainer("Keine Berechtigung", "Nur Admins können diesen Befehl nutzen."))
                .setEphemeral(true).queue()
            return
        }

        val roles = botConfig.selfRoles
        if (roles.isEmpty()) {
            event.replyContainers(errorContainer("Keine Rollen", "Keine Self-Rollen in der config.yml konfiguriert."))
                .setEphemeral(true).queue()
            return
        }

        val lines = roles.joinToString("\n") { r ->
            "${r.emoji ?: "•"} **${r.label}**${r.description?.let { " — $it" } ?: ""}"
        }

        val buttons = roles.map { r ->
            val label = if (r.emoji != null) "${r.emoji} ${r.label}" else r.label
            Button.secondary("selfrole:${r.roleId}", label)
        }

        val panel = container {
            accentColor = COLOR_INFO
            header("🔔 Ping-Rollen")
            text("Klicke auf einen Button um eine Rolle zu erhalten oder zu entfernen.\n\n$lines")
            divider(Separator.Spacing.LARGE)
            buttons.chunked(5).forEach { row(ActionRow.of(it)) }
            footer("Klicke erneut um die Rolle zu entfernen.")
        }

        event.channel.sendContainers(panel).queue()
        event.replyContainers(successContainer("Panel gepostet", "Das Self-Role Panel wurde im Channel gepostet."))
            .setEphemeral(true).queue()
    }
}
