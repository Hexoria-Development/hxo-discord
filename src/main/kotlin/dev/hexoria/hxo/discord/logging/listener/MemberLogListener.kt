package dev.hexoria.hxo.discord.logging.listener

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.logging.LoggingRepository
import dev.hexoria.hxo.discord.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.guild.GuildBanEvent
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleAddEvent
import net.dv8tion.jda.api.events.guild.member.GuildMemberRoleRemoveEvent
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateGlobalNameEvent
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component

@Component
class MemberLogListener(
    private val jda: JDA,
    private val loggingRepository: LoggingRepository,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    private fun memberLogChannel() = jda.getTextChannelById(botConfig.channels.memberLogChannelId)
    private fun roleLogChannel() = jda.getTextChannelById(botConfig.channels.roleLogChannelId)

    // ── Member Join/Leave ────────────────────────────────────────────────────

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "JOIN", null)
            channel.sendSilentContainers(container {
                accentColor = COLOR_SUCCESS
                section(event.user.effectiveAvatarUrl) {
                    header("✅ Mitglied beigetreten")
                    text("${event.user.asMention}\n`${event.user.name}`")
                }
                divider()
                field("ID", "`${event.user.idLong}`")
                footer("Server: ${event.guild.name}", now)
            }).queue()
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            val roles = event.member?.roles
                ?.filter { it.name != "@everyone" }
                ?.joinToString(" ") { it.asMention }
                ?.takeIf { it.isNotBlank() } ?: "*keine*"
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "LEAVE", roles)
            channel.sendSilentContainers(container {
                accentColor = COLOR_ERROR
                section(event.user.effectiveAvatarUrl) {
                    header("👋 Mitglied verlassen")
                    text("${event.user.asMention}\n`${event.user.name}`")
                }
                divider()
                field("ID", "`${event.user.idLong}`")
                field("Rollen", roles.take(1024))
                footer("Server: ${event.guild.name}", now)
            }).queue()
        }
    }

    // ── Ban/Unban ────────────────────────────────────────────────────────────

    override fun onGuildBan(event: GuildBanEvent) {
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "BAN", null)
            channel.sendSilentContainers(container {
                accentColor = COLOR_ERROR
                section(event.user.effectiveAvatarUrl) {
                    header("🔨 Mitglied gebannt")
                    text("${event.user.asMention} `${event.user.name}`")
                }
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    override fun onGuildUnban(event: GuildUnbanEvent) {
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "UNBAN", null)
            channel.sendSilentContainers(container {
                accentColor = COLOR_INFO
                section(event.user.effectiveAvatarUrl) {
                    header("🔓 Mitglied entbannt")
                    text("${event.user.asMention} `${event.user.name}`")
                }
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    // ── Name / Nickname ──────────────────────────────────────────────────────

    override fun onGuildMemberUpdateNickname(event: GuildMemberUpdateNicknameEvent) {
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            val detail = "${event.oldNickname ?: event.member.user.name} → ${event.newNickname ?: event.member.user.name}"
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "NICKNAME", detail)
            channel.sendSilentContainers(container {
                accentColor = COLOR_WARNING
                section(event.user.effectiveAvatarUrl) {
                    header("✏️ Nickname geändert")
                    text(event.user.asMention)
                }
                divider()
                field("Vorher", event.oldNickname ?: "*keiner*")
                field("Nachher", event.newNickname ?: "*keiner*")
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    override fun onUserUpdateName(event: UserUpdateNameEvent) {
        val guild = jda.getGuildById(botConfig.guildId) ?: return
        if (guild.getMember(event.user) == null) return
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(guild.idLong, event.user.idLong, event.newValue, "USERNAME", "${event.oldValue} → ${event.newValue}")
            channel.sendSilentContainers(container {
                accentColor = COLOR_WARNING
                section(event.user.effectiveAvatarUrl) {
                    header("🔤 Username geändert")
                    text(event.user.asMention)
                }
                divider()
                field("Vorher", "`${event.oldValue}`")
                field("Nachher", "`${event.newValue}`")
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    override fun onUserUpdateGlobalName(event: UserUpdateGlobalNameEvent) {
        val guild = jda.getGuildById(botConfig.guildId) ?: return
        if (guild.getMember(event.user) == null) return
        val channel = memberLogChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(guild.idLong, event.user.idLong, event.user.name, "DISPLAY_NAME", "${event.oldValue ?: "*keiner*"} → ${event.newValue ?: "*keiner*"}")
            channel.sendSilentContainers(container {
                accentColor = COLOR_WARNING
                section(event.user.effectiveAvatarUrl) {
                    header("🏷️ Anzeigename geändert")
                    text(event.user.asMention)
                }
                divider()
                field("Vorher", event.oldValue ?: "*keiner*")
                field("Nachher", event.newValue ?: "*keiner*")
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    // ── Rollen ───────────────────────────────────────────────────────────────

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        val channel = roleLogChannel() ?: return
        coroutineScope.launch {
            val roles = event.roles.joinToString(", ") { it.name }
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "ROLE_ADD", roles)
            channel.sendSilentContainers(container {
                accentColor = COLOR_SUCCESS
                section(event.user.effectiveAvatarUrl) {
                    header("➕ Rolle hinzugefügt")
                    text("${event.user.asMention}\n`${event.user.name}`")
                }
                divider()
                field("Rollen", event.roles.joinToString(" ") { it.asMention })
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val channel = roleLogChannel() ?: return
        coroutineScope.launch {
            val roles = event.roles.joinToString(", ") { it.name }
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "ROLE_REMOVE", roles)
            channel.sendSilentContainers(container {
                accentColor = COLOR_ERROR
                section(event.user.effectiveAvatarUrl) {
                    header("➖ Rolle entfernt")
                    text("${event.user.asMention}\n`${event.user.name}`")
                }
                divider()
                field("Rollen", event.roles.joinToString(" ") { it.asMention })
                footer("User-ID: ${event.user.idLong}", now)
            }).queue()
        }
    }
}
