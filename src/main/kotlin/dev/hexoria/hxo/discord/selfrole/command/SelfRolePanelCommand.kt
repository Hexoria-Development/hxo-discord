package dev.hexoria.hxo.discord.selfrole.command

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class SelfRolePanelCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "selfrole-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_SELFROLE_PANEL)) {
            event.replyEmbeds(errorEmbed("Keine Berechtigung", "Nur Admins können diesen Befehl nutzen."))
                .setEphemeral(true).queue()
            return
        }

        val roles = botConfig.selfRoles
        if (roles.isEmpty()) {
            event.replyEmbeds(errorEmbed("Keine Rollen", "Keine Self-Rollen in der config.yml konfiguriert."))
                .setEphemeral(true).queue()
            return
        }

        val lines = roles.joinToString("\n") { r ->
            "${r.emoji ?: "•"} **${r.label}**${r.description?.let { " — $it" } ?: ""}"
        }

        val panelEmbed = embed {
            setTitle("🔔 Ping-Rollen")
            setDescription("Klicke auf einen Button um eine Rolle zu erhalten oder zu entfernen.\n\n$lines")
            setColor(COLOR_INFO)
            setFooter("Klicke erneut um die Rolle zu entfernen.")
        }

        val buttons = roles.map { r ->
            val label = if (r.emoji != null) "${r.emoji} ${r.label}" else r.label
            Button.secondary("selfrole:${r.roleId}", label)
        }

        val rows = buttons.chunked(5).map { ActionRow.of(it) }

        event.channel.sendMessageEmbeds(panelEmbed).setComponents(rows).queue()
        event.replyEmbeds(successEmbed("Panel gepostet", "Das Self-Role Panel wurde im Channel gepostet."))
            .setEphemeral(true).queue()
    }
}
