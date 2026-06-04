package dev.hiorcraft.nex.discord.counting

import dev.hiorcraft.nex.discord.config.botConfig
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.stereotype.Component

enum class CountResult { SUCCESS, WRONG_NUMBER, SAME_USER }

@Component
class CountingService(
    private val countingRepository: CountingRepository,
    private val coroutineScope: CoroutineScope,
) {
    private val guildId = botConfig.guildId

    @Volatile private var _currentCount: Long = 0
    val currentCount: Long get() = _currentCount

    @Volatile private var _lastUserId: Long = 0L
    val lastUserId: Long get() = _lastUserId

    @PostConstruct
    fun init() {
        val state = countingRepository.load(guildId) ?: return
        _currentCount = state.first
        _lastUserId = state.second
    }

    @Synchronized
    fun tryIncrement(userId: Long, number: Long): CountResult {
        if (userId == _lastUserId) return CountResult.SAME_USER
        if (number != _currentCount + 1) return CountResult.WRONG_NUMBER
        _currentCount++
        _lastUserId = userId
        persist()
        return CountResult.SUCCESS
    }

    @Synchronized
    fun reset() {
        _currentCount = 0
        _lastUserId = 0L
        persist()
    }

    private fun persist() {
        val count = currentCount
        val userId = lastUserId
        coroutineScope.launch(Dispatchers.IO) {
            countingRepository.save(guildId, count, userId)
        }
    }
}
