package dev.hexoria.hxo.discord.ticket.deadline

import java.time.LocalDateTime
import java.util.*

data class ReplyDeadline(
    val id: Long,
    val ticketId: UUID,
    val threadId: Long,
    val targetUserId: Long,
    val targetUserName: String,
    val setById: Long,
    val setByName: String,
    val deadline: LocalDateTime,
)
