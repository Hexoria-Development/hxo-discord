package dev.hiorcraft.nex.discord.automod

import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class AutomodDeleteTracker {
    private val cache = Caffeine.newBuilder()
        .maximumSize(5_000)
        .expireAfterWrite(30, TimeUnit.SECONDS)
        .build<Long, Boolean>()

    fun mark(messageId: Long) = cache.put(messageId, true)

    fun consume(messageId: Long): Boolean {
        val marked = cache.getIfPresent(messageId) == true
        if (marked) cache.invalidate(messageId)
        return marked
    }
}