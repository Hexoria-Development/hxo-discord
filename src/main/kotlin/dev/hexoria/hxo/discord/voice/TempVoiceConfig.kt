package dev.hexoria.hxo.discord.voice

import kotlinx.serialization.Serializable

@Serializable
data class TempVoiceConfig(
    val enabled: Boolean = false,
    /** Voice-Channel, dessen Beitritt einen eigenen Kanal erstellt ("➕ Sprachkanal erstellen"). */
    val creatorChannelId: Long = 0L,
    /** Kategorie, in der die temporären Kanäle angelegt werden. Leer = Kategorie des Creator-Channels. */
    val categoryId: Long = 0L,
    /** Text-Channel, in dem die Willkommens-Nachricht des Systems steht. */
    val infoChannelId: Long = 0L,
    /** Name des erstellten Kanals. `%user%` wird durch den Anzeigenamen ersetzt. */
    val channelNameTemplate: String = "🔊 %user%",
    /** Nutzerlimit neuer Kanäle (0 = unbegrenzt). */
    val defaultUserLimit: Int = 0,
)
