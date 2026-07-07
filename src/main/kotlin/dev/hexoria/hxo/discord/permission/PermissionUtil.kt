package dev.hexoria.hxo.discord.permission

import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.User

private val guildPermissionConfig: Map<Long, Map<Long, Set<DiscordPermission>>> = mapOf(

    // Hexoria Network
    1411323559225069620 to mapOf(

        //  Admin
        1411324020523143279 to setOf(*DiscordPermission.entries.toTypedArray()),

        // Developer
        1506411737379770519 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_ADD_SILENT,
            DiscordPermission.COMMAND_TICKET_REMOVE,
            DiscordPermission.COMMAND_TICKET_BUTTONS,

            DiscordPermission.TICKET_TYPE_BYPASS,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_BUG_VIEW,
            DiscordPermission.TICKET_REPORT_VIEW,
            DiscordPermission.TICKET_UNBAN_VIEW,
            DiscordPermission.TICKET_SUPPORT_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Developer A
        1523126616153395341 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_ADD_SILENT,
            DiscordPermission.COMMAND_TICKET_REMOVE,
            DiscordPermission.COMMAND_TICKET_BUTTONS,

            DiscordPermission.TICKET_TYPE_BYPASS,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_BUG_VIEW,
            DiscordPermission.TICKET_REPORT_VIEW,
            DiscordPermission.TICKET_UNBAN_VIEW,
            DiscordPermission.TICKET_SUPPORT_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        //Management
        1450063758457049108 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_BEWERBUNG_VIEW,
            DiscordPermission.TICKET_TEAM_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,
            DiscordPermission.TICKET_CONTENT_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Community Management
        1463490632495661222 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_BEWERBUNG_VIEW,
            DiscordPermission.TICKET_TEAM_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,
            DiscordPermission.TICKET_CONTENT_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Moderator
        1411324116761313351 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_REPORT_VIEW,
            DiscordPermission.TICKET_UNBAN_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,
            DiscordPermission.TICKET_SUPPORT_VIEW,
            DiscordPermission.TICKET_BEWERBUNG_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Moderator A
        1523132759428562974 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_REPORT_VIEW,
            DiscordPermission.TICKET_UNBAN_VIEW,
            DiscordPermission.TICKET_DISCORD_VIEW,
            DiscordPermission.TICKET_SUPPORT_VIEW,
            DiscordPermission.TICKET_BEWERBUNG_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Supporter
        1450063758457049108 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_SUPPORT_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Supporter A
        1523132634614595594 to setOf(
            DiscordPermission.COMMAND_TICKET_ADD,
            DiscordPermission.COMMAND_TICKET_REMOVE,

            DiscordPermission.TICKET_CLOSE,
            DiscordPermission.TICKET_CLAIM,

            DiscordPermission.TICKET_SUPPORT_VIEW,

            DiscordPermission.COMMAND_FAQ,
            DiscordPermission.AUTOMOD_BYPASS,
        ),

        // Creator
        1450064251010945024 to setOf(
            DiscordPermission.TICKET_CONTENT_CREATE,
            DiscordPermission.AUTOMOD_BYPASS,
        ),
    )
)

fun User.hasPermission(guildId: Long, permission: DiscordPermission): Boolean {
    val guild = jda.getGuildById(guildId) ?: return false
    val member = guild.getMember(this) ?: return false

    return member.hasPermission(permission)
}

fun Member?.hasPermission(permission: DiscordPermission): Boolean {
    if (this == null) {
        return false
    }

    val guildPerms = guildPermissionConfig[guild.idLong] ?: return false
    val memberRoleIds = roles.map { it.idLong }

    return memberRoleIds.any { roleId ->
        guildPerms[roleId]?.contains(permission) == true
    }
}

fun DiscordPermission.getRolesWithPermission(guildId: Long) =
    guildPermissionConfig[guildId]?.filterValues { this in it }?.keys ?: emptySet()
