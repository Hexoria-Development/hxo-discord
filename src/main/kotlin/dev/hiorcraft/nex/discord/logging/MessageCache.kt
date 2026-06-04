package dev.hiorcraft.nex.discord.logging

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

data class CachedMessage(
    val authorId: Long,
    val authorName: String,
    val authorAvatar: String?,
    val content: String,
    val channelId: Long,
    val channelName: String,
    val isBot: Boolean,
)

@Component
class MessageCache {
    val cache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterWrite(6, TimeUnit.HOURS)
        .build<Long, CachedMessage>()

    fun put(messageId: Long, msg: CachedMessage) = cache.put(messageId, msg)
    fun get(messageId: Long): CachedMessage? = cache.getIfPresent(messageId)
    fun invalidate(messageId: Long) = cache.invalidate(messageId)
}
