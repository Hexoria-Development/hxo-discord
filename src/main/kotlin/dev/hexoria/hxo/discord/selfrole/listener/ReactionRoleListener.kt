package dev.hexoria.hxo.discord.selfrole.listener

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.selfrole.REACTION_ROLE_PANEL_FOOTER
import dev.hexoria.hxo.discord.util.componentLogger
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class ReactionRoleListener : ListenerAdapter() {

    private val logger = componentLogger<ReactionRoleListener>()

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        handleReaction(event) { guild, member, role ->
            guild.addRoleToMember(member, role).queue(
                { logger.info("ReactionRole: '${role.name}' an ${member.user.name} vergeben.") },
                { err -> logger.error("ReactionRole: Fehler beim Vergeben von '${role.name}' an ${member.user.name}: ${err.message}") }
            )
        }
    }

    override fun onMessageReactionRemove(event: MessageReactionRemoveEvent) {
        handleReaction(event) { guild, member, role ->
            guild.removeRoleFromMember(member, role).queue(
                { logger.info("ReactionRole: '${role.name}' von ${member.user.name} entfernt.") },
                { err -> logger.error("ReactionRole: Fehler beim Entfernen von '${role.name}' bei ${member.user.name}: ${err.message}") }
            )
        }
    }

    private fun handleReaction(
        event: GenericMessageReactionEvent,
        action: (Guild, Member, Role) -> Unit,
    ) {
        if (!event.isFromGuild) return
        if (event.userIdLong == event.jda.selfUser.idLong) return

        val entry = botConfig.reactionRoles.find { it.emoji == event.emoji.name } ?: return
        val guild = event.guild

        event.retrieveMessage().queue { message ->
            if (message.author.idLong != event.jda.selfUser.idLong) return@queue
            if (!message.isReactionRolePanel()) return@queue

            val role = guild.getRoleById(entry.roleId) ?: run {
                logger.warn("ReactionRole: Rolle mit ID ${entry.roleId} nicht gefunden – übersprungen.")
                return@queue
            }

            guild.retrieveMemberById(event.userIdLong).queue { member ->
                action(guild, member, role)
            }
        }
    }

    /**
     * Erkennt das Panel an seiner Fußzeile – im Container als `-#`-Text,
     * in vor der Components-V2-Umstellung geposteten Panels als Embed-Footer.
     */
    private fun Message.isReactionRolePanel(): Boolean {
        val inContainer = componentTree
            .findAll(TextDisplay::class.java)
            .any { it.content.contains(REACTION_ROLE_PANEL_FOOTER) }
        return inContainer || embeds.firstOrNull()?.footer?.text == REACTION_ROLE_PANEL_FOOTER
    }
}
