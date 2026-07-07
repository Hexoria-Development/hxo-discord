package dev.hexoria.hxo.discord.autorole

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.util.componentLogger
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class AutoRoleListener : ListenerAdapter() {

    private val logger = componentLogger<AutoRoleListener>()

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val roleIds = botConfig.autoRoles
        if (roleIds.isEmpty()) return

        val guild = event.guild
        val member = event.member

        for (roleId in roleIds) {
            val role = guild.getRoleById(roleId) ?: run {
                logger.warn("AutoRole: Rolle $roleId nicht gefunden – übersprungen.")
                continue
            }
            guild.addRoleToMember(member, role).queue(null) {
                logger.warn("AutoRole: Rolle ${role.name} konnte nicht zugewiesen werden: ${it.message}")
            }
        }

        logger.info("AutoRole: ${roleIds.size} Rolle(n) an ${member.user.name} vergeben.")
    }
}
