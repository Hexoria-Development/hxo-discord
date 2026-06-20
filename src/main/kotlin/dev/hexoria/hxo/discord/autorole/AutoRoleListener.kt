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

        roleIds.forEach { roleId ->
            val role = guild.getRoleById(roleId)
            if (role == null) {
                logger.warn("AutoRole: Rolle mit ID $roleId nicht gefunden – übersprungen.")
                return@forEach
            }
            guild.addRoleToMember(member, role).queue(
                { logger.info("AutoRole: Rolle '${role.name}' an ${member.user.name} vergeben.") },
                { err -> logger.error("AutoRole: Fehler beim Vergeben von '${role.name}' an ${member.user.name}: ${err.message}") }
            )
        }
    }
}
