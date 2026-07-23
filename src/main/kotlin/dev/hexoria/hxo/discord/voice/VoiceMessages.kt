package dev.hexoria.hxo.discord.voice

import dev.hexoria.hxo.discord.util.COLOR_INFO
import dev.hexoria.hxo.discord.util.container
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel

const val VOICE_RENAME_BUTTON = "voice:rename"
const val VOICE_LIMIT_BUTTON = "voice:limit"
const val VOICE_LOCK_BUTTON = "voice:lock"
const val VOICE_UNLOCK_BUTTON = "voice:unlock"
const val VOICE_HIDE_BUTTON = "voice:hide"
const val VOICE_SHOW_BUTTON = "voice:show"
const val VOICE_KICK_BUTTON = "voice:kick"
const val VOICE_TRANSFER_BUTTON = "voice:transfer"
const val VOICE_CLAIM_BUTTON = "voice:claim"
const val VOICE_DELETE_BUTTON = "voice:delete"

const val VOICE_RENAME_MODAL = "voice:modal:rename"
const val VOICE_LIMIT_MODAL = "voice:modal:limit"
const val VOICE_KICK_SELECT = "voice:select:kick"
const val VOICE_TRANSFER_SELECT = "voice:select:transfer"

/** Die Willkommens-Nachricht im Info-Channel des Voice-Systems. */
fun voiceInfoContainer(creatorChannelId: Long): Container = container {
    accentColor = COLOR_INFO
    header("🔊 Voice Channel System")
    text(
        "Willkommen beim Voice Channel System. Erstellen Sie Ihren eigenen Sprachkanal, indem Sie " +
        creatorChannelMention(creatorChannelId) + " beitreten. " +
        "Der Sprachkanal wird dann automatisch erstellt."
    )
    divider(Separator.Spacing.LARGE)
    field(
        "Management",
        "Sobald Sie sich in Ihrem Sprachkanal befinden, wird dort eine Nachricht angezeigt, " +
        "die Ihnen bei der Verwaltung hilft.",
    )
}

private fun creatorChannelMention(creatorChannelId: Long): String =
    if (creatorChannelId == 0L) "**➕ Sprachkanal erstellen**" else "<#$creatorChannelId>"

/**
 * Das Manager-Panel, das im Text-Chat des erstellten Sprachkanals gepostet wird.
 * Die Buttons wirken immer auf den Kanal, in dem das Panel steht.
 */
fun voiceManagerContainer(channel: VoiceChannel, ownerId: Long?): Container {
    val everyone = channel.guild.publicRole
    val denied = channel.getPermissionOverride(everyone)?.denied.orEmpty()
    val locked = Permission.VOICE_CONNECT in denied
    val hidden = Permission.VIEW_CHANNEL in denied

    return container {
        accentColor = COLOR_INFO
        header("🎛️ Kanal-Verwaltung")
        text(
            if (ownerId != null) "<@$ownerId>, hier verwalten Sie Ihren Sprachkanal **${channel.name}**."
            else "Dieser Kanal hat aktuell keinen Besitzer. Mitglieder im Kanal können ihn beanspruchen."
        )
        divider(Separator.Spacing.LARGE)
        field(
            "Verfügbare Aktionen",
            """
            ✏️ **Umbenennen** – Namen des Kanals ändern
            👥 **Limit** – Maximale Anzahl an Mitgliedern festlegen
            ${if (locked) "🔓 **Entsperren** – Andere können wieder beitreten" else "🔒 **Sperren** – Niemand kann mehr beitreten"}
            ${if (hidden) "👁️ **Anzeigen** – Kanal wieder sichtbar machen" else "🙈 **Verstecken** – Kanal für andere unsichtbar machen"}
            👢 **Kicken** – Ein Mitglied aus dem Kanal entfernen
            👑 **Übertragen** – Verwaltung an ein Mitglied abgeben
            """.trimIndent(),
        )
        divider(Separator.Spacing.LARGE)
        buttons(
            Button.secondary(VOICE_RENAME_BUTTON, "✏️ Umbenennen"),
            Button.secondary(VOICE_LIMIT_BUTTON, "👥 Limit"),
            if (locked) Button.success(VOICE_UNLOCK_BUTTON, "🔓 Entsperren")
            else Button.secondary(VOICE_LOCK_BUTTON, "🔒 Sperren"),
            if (hidden) Button.success(VOICE_SHOW_BUTTON, "👁️ Anzeigen")
            else Button.secondary(VOICE_HIDE_BUTTON, "🙈 Verstecken"),
        )
        buttons(
            Button.secondary(VOICE_KICK_BUTTON, "👢 Kicken"),
            Button.secondary(VOICE_TRANSFER_BUTTON, "👑 Übertragen"),
            Button.secondary(VOICE_CLAIM_BUTTON, "🙋 Beanspruchen"),
            Button.danger(VOICE_DELETE_BUTTON, "🗑️ Löschen"),
        )
        footer("Nur der Besitzer des Kanals kann diese Aktionen ausführen.")
    }
}
