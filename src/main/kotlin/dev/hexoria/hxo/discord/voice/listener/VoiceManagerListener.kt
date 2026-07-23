package dev.hexoria.hxo.discord.voice.listener

import dev.hexoria.hxo.discord.util.*
import dev.hexoria.hxo.discord.voice.*
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.selections.EntitySelectMenu
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.channel.ChannelType
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.EntitySelectInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.interactions.Interaction
import org.springframework.stereotype.Component

/**
 * Verarbeitet das Manager-Panel im Text-Chat eines temporären Sprachkanals.
 * Alle Aktionen wirken auf den Kanal, in dem das Panel steht.
 */
@Component
class VoiceManagerListener(
    private val tempVoiceService: TempVoiceService,
) : ListenerAdapter() {

    override fun onButtonInteraction(event: ButtonInteractionEvent) {
        when (event.componentId) {
            VOICE_RENAME_BUTTON -> withOwnership(event) { _, _ -> event.replyModal(renameModal()).queue() }
            VOICE_LIMIT_BUTTON -> withOwnership(event) { _, _ -> event.replyModal(limitModal()).queue() }
            VOICE_LOCK_BUTTON -> withOwnership(event) { channel, _ -> setLocked(event, channel, true) }
            VOICE_UNLOCK_BUTTON -> withOwnership(event) { channel, _ -> setLocked(event, channel, false) }
            VOICE_HIDE_BUTTON -> withOwnership(event) { channel, _ -> setHidden(event, channel, true) }
            VOICE_SHOW_BUTTON -> withOwnership(event) { channel, _ -> setHidden(event, channel, false) }
            VOICE_KICK_BUTTON -> withOwnership(event) { _, _ -> promptMemberSelect(event, VOICE_KICK_SELECT, "Wen möchten Sie aus dem Kanal entfernen?") }
            VOICE_TRANSFER_BUTTON -> withOwnership(event) { _, _ -> promptMemberSelect(event, VOICE_TRANSFER_SELECT, "An wen möchten Sie die Verwaltung übergeben?") }
            VOICE_DELETE_BUTTON -> withOwnership(event) { channel, _ -> deleteChannel(event, channel) }
            VOICE_CLAIM_BUTTON -> claimChannel(event)
        }
    }

    override fun onModalInteraction(event: ModalInteractionEvent) {
        when (event.modalId) {
            VOICE_RENAME_MODAL -> withOwnership(event) { channel, _ -> rename(event, channel) }
            VOICE_LIMIT_MODAL -> withOwnership(event) { channel, _ -> setLimit(event, channel) }
        }
    }

    override fun onEntitySelectInteraction(event: EntitySelectInteractionEvent) {
        when (event.componentId) {
            VOICE_KICK_SELECT -> withOwnership(event) { channel, _ -> kickMember(event, channel) }
            VOICE_TRANSFER_SELECT -> withOwnership(event) { channel, _ -> transferOwnership(event, channel) }
        }
    }

    // ── Aktionen ─────────────────────────────────────────────────────────────

    private fun rename(event: ModalInteractionEvent, channel: VoiceChannel) {
        val name = event.getValue("name")?.asString?.trim().orEmpty()
        if (name.isBlank()) {
            event.replyError("Kein Name", "Bitte geben Sie einen Namen an.")
            return
        }

        // Discord erlaubt nur zwei Umbenennungen pro zehn Minuten – daher der eigene Fehlerpfad.
        channel.manager.setName(name.take(100)).queue(
            { event.replySuccess("Umbenannt", "Der Kanal heißt jetzt **${name.take(100)}**.") },
            {
                event.replyError(
                    "Umbenennen fehlgeschlagen",
                    "Discord erlaubt nur zwei Umbenennungen alle zehn Minuten. Bitte versuchen Sie es später erneut.",
                )
            },
        )
    }

    private fun setLimit(event: ModalInteractionEvent, channel: VoiceChannel) {
        val limit = event.getValue("limit")?.asString?.trim()?.toIntOrNull()
        if (limit == null || limit !in 0..99) {
            event.replyError("Ungültiges Limit", "Bitte geben Sie eine Zahl zwischen 0 und 99 an (0 = unbegrenzt).")
            return
        }

        channel.manager.setUserLimit(limit).queue(
            {
                val text = if (limit == 0) "Das Limit wurde aufgehoben." else "Der Kanal ist nun auf **$limit** Mitglieder begrenzt."
                event.replySuccess("Limit gesetzt", text)
            },
            { event.replyError("Fehler", "Das Limit konnte nicht gesetzt werden.") },
        )
    }

    private fun setLocked(event: ButtonInteractionEvent, channel: VoiceChannel, locked: Boolean) {
        val everyone = channel.guild.publicRole
        val manager = channel.upsertPermissionOverride(everyone)
        if (locked) manager.deny(Permission.VOICE_CONNECT) else manager.clear(Permission.VOICE_CONNECT)

        manager.queue(
            {
                refreshPanel(channel)
                if (locked) event.replySuccess("Gesperrt", "Es kann niemand mehr beitreten.")
                else event.replySuccess("Entsperrt", "Der Kanal ist wieder offen.")
            },
            { event.replyError("Fehler", "Die Berechtigungen konnten nicht geändert werden.") },
        )
    }

    private fun setHidden(event: ButtonInteractionEvent, channel: VoiceChannel, hidden: Boolean) {
        val everyone = channel.guild.publicRole
        val manager = channel.upsertPermissionOverride(everyone)
        if (hidden) manager.deny(Permission.VIEW_CHANNEL) else manager.clear(Permission.VIEW_CHANNEL)

        manager.queue(
            {
                refreshPanel(channel)
                if (hidden) event.replySuccess("Versteckt", "Der Kanal ist für andere nicht mehr sichtbar.")
                else event.replySuccess("Sichtbar", "Der Kanal ist wieder sichtbar.")
            },
            { event.replyError("Fehler", "Die Berechtigungen konnten nicht geändert werden.") },
        )
    }

    private fun kickMember(event: EntitySelectInteractionEvent, channel: VoiceChannel) {
        val target = event.mentions.members.firstOrNull() ?: return
        if (target.idLong == event.user.idLong) {
            event.replyError("Nicht möglich", "Sie können sich nicht selbst entfernen.")
            return
        }
        if (channel.members.none { it.idLong == target.idLong }) {
            event.replyError("Nicht im Kanal", "${target.asMention} befindet sich nicht in Ihrem Kanal.")
            return
        }

        channel.guild.kickVoiceMember(target).queue(
            { event.replySuccess("Entfernt", "${target.asMention} wurde aus dem Kanal entfernt.") },
            { event.replyError("Fehler", "Das Mitglied konnte nicht entfernt werden.") },
        )
    }

    private fun transferOwnership(event: EntitySelectInteractionEvent, channel: VoiceChannel) {
        val target = event.mentions.members.firstOrNull() ?: return
        if (target.user.isBot) {
            event.replyError("Nicht möglich", "Bots können keinen Kanal verwalten.")
            return
        }
        if (channel.members.none { it.idLong == target.idLong }) {
            event.replyError("Nicht im Kanal", "${target.asMention} befindet sich nicht in Ihrem Kanal.")
            return
        }

        tempVoiceService.transferOwnership(channel.idLong, target.idLong)
        refreshPanel(channel)
        event.replySuccess("Übertragen", "${target.asMention} verwaltet diesen Kanal jetzt.")
    }

    private fun deleteChannel(event: ButtonInteractionEvent, channel: VoiceChannel) {
        tempVoiceService.forget(channel.idLong)
        channel.delete().queue(null) {
            event.replyError("Fehler", "Der Kanal konnte nicht gelöscht werden.")
        }
    }

    private fun claimChannel(event: ButtonInteractionEvent) {
        val channel = event.tempVoiceChannel() ?: return
        val member = event.member ?: return

        if (channel.members.none { it.idLong == member.idLong }) {
            event.replyError("Nicht im Kanal", "Sie müssen sich im Kanal befinden, um ihn zu beanspruchen.")
            return
        }

        if (!tempVoiceService.claim(channel, member)) {
            event.replyError("Nicht möglich", "Der Besitzer dieses Kanals ist noch anwesend.")
            return
        }

        refreshPanel(channel)
        event.replySuccess("Beansprucht", "${member.asMention} verwaltet diesen Kanal jetzt.")
    }

    // ── Hilfen ───────────────────────────────────────────────────────────────

    private fun renameModal() = modal(VOICE_RENAME_MODAL, "Kanal umbenennen") {
        textInput {
            id = "name"
            label = "Neuer Name"
            style = TextInputStyle.SHORT
            placeholder = "Wie soll Ihr Kanal heißen?"
            lengthRange = 1..100
            required = true
        }
    }

    private fun limitModal() = modal(VOICE_LIMIT_MODAL, "Mitglieder-Limit setzen") {
        textInput {
            id = "limit"
            label = "Maximale Mitglieder (0 = unbegrenzt)"
            style = TextInputStyle.SHORT
            placeholder = "z. B. 5"
            lengthRange = 1..2
            required = true
        }
    }

    private fun promptMemberSelect(event: ButtonInteractionEvent, menuId: String, prompt: String) {
        val menu = EntitySelectMenu.create(menuId, EntitySelectMenu.SelectTarget.USER)
            .setPlaceholder("Mitglied auswählen …")
            .build()

        event.replyContainers(container {
            accentColor = COLOR_INFO
            text(prompt)
            row(ActionRow.of(menu))
        }).setEphemeral(true).queue()
    }

    /**
     * Führt [action] nur aus, wenn die Interaktion aus einem temporären Sprachkanal kommt
     * und der Klickende dessen Besitzer ist.
     */
    private inline fun <T> withOwnership(
        event: T,
        action: (VoiceChannel, Member) -> Unit,
    ) where T : IReplyCallback, T : Interaction {
        val channel = event.tempVoiceChannel() ?: return
        val member = event.member ?: return

        val owner = tempVoiceService.ownerOf(channel.idLong)
        if (owner == null) {
            event.replyError(
                "Kein Besitzer",
                "Dieser Kanal hat keinen Besitzer mehr. Beanspruchen Sie ihn über **🙋 Beanspruchen**.",
            )
            return
        }
        if (owner != member.idLong) {
            event.replyError("Keine Berechtigung", "Nur <@$owner> kann diesen Kanal verwalten.")
            return
        }

        action(channel, member)
    }

    private fun Interaction.tempVoiceChannel(): VoiceChannel? {
        if (channelType != ChannelType.VOICE) return null
        val voiceChannel = channel as? VoiceChannel ?: return null
        return voiceChannel.takeIf { tempVoiceService.isTempChannel(it.idLong) }
    }

    /**
     * Zeichnet das Panel neu, damit Besitzer- und Zustandsanzeige stimmen.
     *
     * Läuft bewusst über die gemerkte Nachrichten-ID: Bei einer Auswahl aus dem ephemeren
     * Select-Menü ist `event.message` nicht das Panel, sondern das Menü selbst.
     */
    private fun refreshPanel(channel: VoiceChannel) {
        val panelId = tempVoiceService.panelMessageId(channel.idLong) ?: return
        channel.editMessageComponentsById(
            panelId,
            listOf(voiceManagerContainer(channel, tempVoiceService.ownerOf(channel.idLong))),
        ).useComponentsV2().queue(null) { }
    }

    private fun IReplyCallback.replySuccess(title: String, description: String) =
        replyContainers(successContainer(title, description)).setEphemeral(true).queue()

    private fun IReplyCallback.replyError(title: String, description: String) =
        replyContainers(errorContainer(title, description)).setEphemeral(true).queue()
}
