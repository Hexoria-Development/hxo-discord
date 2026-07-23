package dev.hexoria.hxo.discord.selfrole.listener

import dev.hexoria.hxo.discord.util.errorContainer
import dev.hexoria.hxo.discord.util.replyContainers
import dev.hexoria.hxo.discord.util.successContainer
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class SelfRoleButtonListener : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        if (!event.componentId.startsWith("selfrole:")) return

        val roleId = event.componentId.removePrefix("selfrole:").toLongOrNull() ?: return
        val guild = event.guild ?: return
        val member = event.member ?: return

        val role = guild.getRoleById(roleId) ?: run {
            event.replyContainers(errorContainer("Fehler", "Diese Rolle existiert nicht mehr. Bitte einen Admin kontaktieren."))
                .setEphemeral(true).queue()
            return
        }

        if (role in member.roles) {
            guild.removeRoleFromMember(member, role).queue()
            event.replyContainers(successContainer("Rolle entfernt", "**${role.name}** wurde entfernt."))
                .setEphemeral(true).queue()
        } else {
            guild.addRoleToMember(member, role).queue()
            event.replyContainers(successContainer("Rolle hinzugefügt", "**${role.name}** wurde hinzugefügt."))
                .setEphemeral(true).queue()
        }
    }
}
