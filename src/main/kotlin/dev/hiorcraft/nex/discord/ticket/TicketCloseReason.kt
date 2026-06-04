package dev.hiorcraft.nex.discord.ticket

data class TicketCloseReason(
    val id: String,
    val displayName: String,
    val description: String,
) {
    companion object {
        fun of(id: String, displayName: String, description: String) =
            TicketCloseReason(id, displayName, description)
    }
}
