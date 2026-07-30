package dev.hexoria.hxo.discord.selfrole.command

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.permission.hasPermission
import dev.hexoria.hxo.discord.selfrole.REACTION_ROLE_PANEL_FOOTER
import dev.hexoria.hxo.discord.util.*
import net.dv8tion.jda.api.entities.emoji.Emoji
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class ReactionRolePanelCommand : ListenerAdapter() {

    override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
        if (event.name != "reactionrole-panel") return

        if (!event.member.hasPermission(DiscordPermission.COMMAND_REACTIONROLE_PANEL)) {
            event.replyContainers(errorContainer("Keine Berechtigung", "Nur Admins können diesen Befehl nutzen."))
                .setEphemeral(true).queue()
            return
        }

        val roles = botConfig.reactionRoles
        if (roles.isEmpty()) {
            event.replyContainers(errorContainer("Keine Rollen", "Keine Reaction-Rollen in der config.yml konfiguriert."))
                .setEphemeral(true).queue()
            return
        }

        val lines = roles.joinToString("\n") { r -> "${r.emoji} = <@&${r.roleId}>" }

        val panel = container {
            section(null) {
                header("Hol dir deine Rolle")
                text(lines)
            }
            footer(REACTION_ROLE_PANEL_FOOTER)
        }

        event.channel.sendSilentContainers(panel).queue { message ->
            roles.forEach { r ->
                message.addReaction(Emoji.fromUnicode(r.emoji)).queue(null) {}
            }
        }

        event.replyContainers(successContainer("Panel gepostet", "Das Reaction-Role Panel wurde im Channel gepostet."))
            .setEphemeral(true).queue()
    }
}
