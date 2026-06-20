package dev.hexoria.hxo.discord.config

import kotlinx.serialization.Serializable
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
@Serializable
data class TicketConfig(
    val maxOpenTicketsPerUser: Int = 1,
    val transcriptEnabled: Boolean = false,
)
