package dev.hiorcraft.nex.discord.selfrole.listener

import dev.hiorcraft.nex.discord.util.errorEmbed
import dev.hiorcraft.nex.discord.util.successEmbed
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
            event.replyEmbeds(errorEmbed("Fehler", "Diese Rolle existiert nicht mehr. Bitte einen Admin kontaktieren."))
                .setEphemeral(true).queue()
            return
        }

        if (role in member.roles) {
            guild.removeRoleFromMember(member, role).queue()
            event.replyEmbeds(successEmbed("Rolle entfernt", "**${role.name}** wurde entfernt."))
                .setEphemeral(true).queue()
        } else {
            guild.addRoleToMember(member, role).queue()
            event.replyEmbeds(successEmbed("Rolle hinzugefügt", "**${role.name}** wurde hinzugefügt."))
                .setEphemeral(true).queue()
        }
    }
}
