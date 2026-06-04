package dev.hiorcraft.nex.discord.selfrole

import kotlinx.serialization.Serializable

@Serializable
data class SelfRoleEntry(
    val roleId: Long,
    val label: String,
    val emoji: String? = null,
    val description: String? = null,
)
