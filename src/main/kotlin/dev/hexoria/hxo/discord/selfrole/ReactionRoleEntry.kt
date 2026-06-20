package dev.hexoria.hxo.discord.selfrole

import kotlinx.serialization.Serializable

const val REACTION_ROLE_PANEL_FOOTER = "Reaction-Role Panel"

@Serializable
data class ReactionRoleEntry(
    val roleId: Long,
    val emoji: String,
    val label: String,
)