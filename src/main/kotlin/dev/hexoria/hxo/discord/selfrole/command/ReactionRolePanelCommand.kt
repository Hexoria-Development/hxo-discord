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
            event.replyEmbeds(errorEmbed("Keine Berechtigung", "Nur Admins können diesen Befehl nutzen."))
                .setEphemeral(true).queue()
            return
        }

        val roles = botConfig.reactionRoles
        if (roles.isEmpty()) {
            event.replyEmbeds(errorEmbed("Keine Rollen", "Keine Reaction-Rollen in der config.yml konfiguriert."))
                .setEphemeral(true).queue()
            return
        }

        val lines = roles.joinToString("\n") { r -> "${r.emoji} = <@&${r.roleId}>" }

        val panelEmbed = embed {
            setTitle("Hol dir deine Rolle")
            setDescription(lines)
            setColor(COLOR_INFO)
            setFooter(REACTION_ROLE_PANEL_FOOTER)
        }

        event.channel.sendMessageEmbeds(panelEmbed).queue { message ->
            roles.forEach { r ->
                message.addReaction(Emoji.fromUnicode(r.emoji)).queue(null) {}
            }
        }

        event.replyEmbeds(successEmbed("Panel gepostet", "Das Reaction-Role Panel wurde im Channel gepostet."))
            .setEphemeral(true).queue()
    }
}
