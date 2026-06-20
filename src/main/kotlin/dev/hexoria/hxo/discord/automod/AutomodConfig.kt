package dev.hexoria.hxo.discord.automod

import kotlinx.serialization.Serializable

@Serializable
data class AutomodConfig(
    val linkFilter: LinkFilterConfig = LinkFilterConfig(),
    val chatSpam: ChatSpamConfig = ChatSpamConfig(),
    val voiceSpam: VoiceSpamConfig = VoiceSpamConfig(),
)

@Serializable
data class LinkFilterConfig(
    val enabled: Boolean = false,
    val allowedDomains: List<String> = emptyList(),
    val ignoredChannels: List<Long> = emptyList(),
)

@Serializable
data class ChatSpamConfig(
    val enabled: Boolean = false,
    val maxMessages: Int = 5,
    val timeWindowSeconds: Int = 5,
    val timeoutSeconds: Long = 60,
    val ignoredChannels: List<Long> = emptyList(),
)

@Serializable
data class VoiceSpamConfig(
    val enabled: Boolean = false,
    val maxEvents: Int = 5,
    val timeWindowSeconds: Int = 5,
)