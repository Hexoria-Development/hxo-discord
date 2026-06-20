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
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class MemberLogListener(
    private val jda: JDA,
    private val loggingRepository: LoggingRepository,
    private val coroutineScope: CoroutineScope,
) : ListenerAdapter() {

    private fun logChannel() = jda.getTextChannelById(botConfig.channels.memberLogChannelId)

    override fun onGuildMemberJoin(event: GuildMemberJoinEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "JOIN", null)
            channel.sendMessageEmbeds(embed {
                setTitle("✅ Mitglied Beigetreten")
                setColor(COLOR_SUCCESS)
                addField("User", "${event.user.asMention}\n`${event.user.name}`", true)
                addField("ID", "`${event.user.idLong}`", true)
                setThumbnail(event.user.effectiveAvatarUrl)
                setFooter("Server: ${event.guild.name}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildMemberRemove(event: GuildMemberRemoveEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            val roles = event.member?.roles
                ?.filter { it.name != "@everyone" }
                ?.joinToString(" ") { it.asMention }
                ?.takeIf { it.isNotBlank() } ?: "*keine*"
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "LEAVE", roles)
            channel.sendMessageEmbeds(embed {
                setTitle("👋 Mitglied Verlassen")
                setColor(COLOR_ERROR)
                addField("User", "${event.user.asMention}\n`${event.user.name}`", true)
                addField("ID", "`${event.user.idLong}`", true)
                addField("Rollen", roles.take(1024), false)
                setThumbnail(event.user.effectiveAvatarUrl)
                setFooter("Server: ${event.guild.name}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildBan(event: GuildBanEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "BAN", null)
            channel.sendMessageEmbeds(embed {
                setTitle("🔨 Mitglied gebannt")
                setColor(COLOR_ERROR)
                setDescription("<@${event.user.idLong}> **${event.user.name}**")
                setThumbnail(event.user.effectiveAvatarUrl)
                setFooter("User-ID: ${event.user.idLong}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildUnban(event: GuildUnbanEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "UNBAN", null)
            channel.sendMessageEmbeds(embed {
                setTitle("🔓 Mitglied entbannt")
                setColor(COLOR_INFO)
                setDescription("<@${event.user.idLong}> **${event.user.name}**")
                setThumbnail(event.user.effectiveAvatarUrl)
                setFooter("User-ID: ${event.user.idLong}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildMemberUpdateNickname(event: GuildMemberUpdateNicknameEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            val detail = "${event.oldNickname ?: event.member.user.name} → ${event.newNickname ?: event.member.user.name}"
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "NICKNAME", detail)
            channel.sendMessageEmbeds(embed {
                setTitle("✏️ Nickname geändert")
                setColor(COLOR_WARNING)
                setDescription("<@${event.user.idLong}>")
                addField("Vorher", event.oldNickname ?: "*keiner*", true)
                addField("Nachher", event.newNickname ?: "*keiner*", true)
                setFooter("User-ID: ${event.user.idLong}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildMemberRoleAdd(event: GuildMemberRoleAddEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            val roles = event.roles.joinToString(", ") { it.name }
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "ROLE_ADD", roles)
            channel.sendMessageEmbeds(embed {
                setTitle("➕ Rolle hinzugefügt")
                setColor(COLOR_SUCCESS)
                setDescription("<@${event.user.idLong}>")
                addField("Rollen", event.roles.joinToString(" ") { it.asMention }, false)
                setFooter("User-ID: ${event.user.idLong}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }

    override fun onGuildMemberRoleRemove(event: GuildMemberRoleRemoveEvent) {
        val channel = logChannel() ?: return
        coroutineScope.launch {
            val roles = event.roles.joinToString(", ") { it.name }
            loggingRepository.logMember(event.guild.idLong, event.user.idLong, event.user.name, "ROLE_REMOVE", roles)
            channel.sendMessageEmbeds(embed {
                setTitle("➖ Rolle entfernt")
                setColor(COLOR_ERROR)
                setDescription("<@${event.user.idLong}>")
                addField("Rollen", event.roles.joinToString(" ") { it.asMention }, false)
                setFooter("User-ID: ${event.user.idLong}")
                setTimestamp(Instant.now())
            }).queue()
        }
    }
}
