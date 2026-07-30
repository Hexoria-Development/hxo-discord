package dev.hexoria.hxo.discord.voice

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
    section(null) {
        header("Voice Channel System")
        text(
            "Willkommen beim Voice Channel System.\n\n" +
            "Erstelle deinen eigenen Sprachkanal, indem du " + creatorChannelMention(creatorChannelId) +
            " beitrittst. Der Sprachkanal wird dann automatisch erstellt."
        )
    }
    divider(Separator.Spacing.LARGE)
    field(
        "Verwaltung",
        "Sobald du dich in deinem Sprachkanal befindest, erscheint dort eine Nachricht, die dir bei der Verwaltung hilft.",
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

    return container {
        section(null) {
            header("Sprachkanalverwaltung")
            text(
                if (ownerId != null) "Verwalte deinen eigenen Sprachkanal <@$ownerId>"
                else "Dieser Kanal hat aktuell keinen Besitzer. Mitglieder im Kanal können ihn beanspruchen."
            )
        }
        divider(Separator.Spacing.LARGE)
        buttons(
            Button.secondary(VOICE_RENAME_BUTTON, "✏️ Umbenennen"),
            if (locked) Button.success(VOICE_UNLOCK_BUTTON, "🔓 Öffentlicher Kanal")
            else Button.secondary(VOICE_LOCK_BUTTON, "🔒 Privater Kanal"),
            Button.secondary(VOICE_LIMIT_BUTTON, "👥 Benutzerlimit setzen"),
            Button.secondary(VOICE_KICK_BUTTON, "👢 Benutzer rauswerfen"),
            Button.danger(VOICE_DELETE_BUTTON, "🗑️ Löschen"),
        )
        if (ownerId == null) {
            buttons(Button.success(VOICE_CLAIM_BUTTON, "🙋 Beanspruchen"))
        }
    }
}
