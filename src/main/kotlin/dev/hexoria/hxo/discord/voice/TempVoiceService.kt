package dev.hexoria.hxo.discord.voice

import dev.hexoria.hxo.discord.config.botConfig
import dev.hexoria.hxo.discord.util.componentLogger
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.concrete.Category
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Verwaltet die temporären Sprachkanäle.
 *
 * Die Besitzer werden nur im Speicher gehalten – die Kanäle leben ohnehin nur so lange, wie jemand
 * drin ist. Nach einem Neustart hat ein noch bestehender Kanal deshalb keinen Besitzer mehr und
 * kann von einem Mitglied im Kanal über den Manager beansprucht werden.
 */
@Service
class TempVoiceService(private val jda: JDA) {
    private val logger = componentLogger<TempVoiceService>()

    private val owners = ConcurrentHashMap<Long, Long>()

    /** Kanal-ID → ID der Manager-Nachricht, damit sie nach Änderungen aktualisiert werden kann. */
    private val panels = ConcurrentHashMap<Long, Long>()

    val config get() = botConfig.tempVoice

    fun isTempChannel(channelId: Long): Boolean =
        owners.containsKey(channelId) || channelId.isInTempCategory()

    fun ownerOf(channelId: Long): Long? = owners[channelId]

    fun isOwner(channelId: Long, userId: Long): Boolean = owners[channelId] == userId

    /**
     * Übernimmt einen Kanal, dessen Besitzer nicht mehr anwesend ist – etwa nach einem Neustart
     * des Bots oder wenn der ursprüngliche Besitzer den Kanal verlassen hat.
     */
    fun claim(channel: VoiceChannel, member: Member): Boolean {
        val currentOwner = owners[channel.idLong]
        val ownerPresent = currentOwner != null && channel.members.any { it.idLong == currentOwner }
        if (ownerPresent) return false

        owners[channel.idLong] = member.idLong
        logger.info("Temp-Voice: ${member.user.name} hat Kanal ${channel.name} beansprucht.")
        return true
    }

    fun transferOwnership(channelId: Long, newOwnerId: Long) {
        owners[channelId] = newOwnerId
    }

    /** Legt den Kanal für [member] an und verschiebt das Mitglied hinein. */
    fun createFor(member: Member): VoiceChannel? {
        val guild = member.guild
        val category = resolveCategory(guild) ?: run {
            logger.error("Temp-Voice: Keine Kategorie für neue Sprachkanäle gefunden.")
            return null
        }

        val name = config.channelNameTemplate
            .replace("%user%", member.effectiveName)
            .take(100)

        val channel = runCatching {
            category.createVoiceChannel(name)
                .apply { if (config.defaultUserLimit > 0) setUserlimit(config.defaultUserLimit) }
                .addMemberPermissionOverride(
                    member.idLong,
                    listOf(Permission.VIEW_CHANNEL, Permission.VOICE_CONNECT),
                    emptyList(),
                )
                .complete()
        }.getOrElse {
            logger.error("Temp-Voice: Kanal für ${member.user.name} konnte nicht erstellt werden.", it)
            return null
        }

        val moved = runCatching { guild.moveVoiceMember(member, channel).complete() }.isSuccess
        if (!moved) {
            logger.warn("Temp-Voice: ${member.user.name} konnte nicht verschoben werden – Kanal wird verworfen.")
            channel.delete().queue(null) { }
            return null
        }

        owners[channel.idLong] = member.idLong
        logger.info("Temp-Voice: Kanal '${channel.name}' für ${member.user.name} erstellt.")
        return channel
    }

    /** Löscht den Kanal, sobald niemand mehr drin ist. */
    fun deleteIfEmpty(channel: VoiceChannel) {
        if (channel.members.isNotEmpty()) return
        if (!isTempChannel(channel.idLong)) return

        forget(channel.idLong)
        channel.delete().queue(
            { logger.info("Temp-Voice: Leerer Kanal '${channel.name}' gelöscht.") },
            { logger.warn("Temp-Voice: Kanal '${channel.name}' konnte nicht gelöscht werden: ${it.message}") },
        )
    }

    fun rememberPanel(channelId: Long, messageId: Long) {
        panels[channelId] = messageId
    }

    fun panelMessageId(channelId: Long): Long? = panels[channelId]

    fun forget(channelId: Long) {
        owners.remove(channelId)
        panels.remove(channelId)
    }

    private fun resolveCategory(guild: Guild): Category? {
        if (config.categoryId != 0L) return guild.getCategoryById(config.categoryId)
        return guild.getVoiceChannelById(config.creatorChannelId)?.parentCategory
    }

    private fun Long.isInTempCategory(): Boolean {
        if (config.categoryId == 0L) return false
        val channel = jda.getVoiceChannelById(this) ?: return false
        return channel.parentCategory?.idLong == config.categoryId && this != config.creatorChannelId
    }

    /**
     * Räumt Kanäle auf, die einen Neustart überlebt haben: alles in der Temp-Kategorie, was leer ist.
     */
    @EventListener(ApplicationReadyEvent::class)
    fun cleanupOrphaned() {
        if (!config.enabled) return

        if (config.categoryId == 0L) {
            logger.warn(
                "Temp-Voice: Ohne 'categoryId' können Kanäle aus einer früheren Sitzung nicht erkannt werden – " +
                "sie bleiben nach einem Neustart bestehen und lassen sich nicht mehr verwalten."
            )
            return
        }

        val category = jda.getCategoryById(config.categoryId) ?: run {
            logger.warn("Temp-Voice: Kategorie ${config.categoryId} nicht gefunden – kein Aufräumen möglich.")
            return
        }

        val orphaned = category.voiceChannels
            .filter { it.idLong != config.creatorChannelId && it.members.isEmpty() }

        orphaned.forEach { it.delete().queue(null) { } }
        if (orphaned.isNotEmpty()) {
            logger.info("Temp-Voice: ${orphaned.size} verwaiste Sprachkanäle beim Start entfernt.")
        }
    }
}
