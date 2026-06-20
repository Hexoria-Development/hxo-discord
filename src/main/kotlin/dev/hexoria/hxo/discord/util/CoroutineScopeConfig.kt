package dev.hexoria.hxo.discord.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.Executors

@Configuration
class CoroutineScopeConfig {

    @Bean
    fun discordCoroutineScope(): CoroutineScope =
        CoroutineScope(
            SupervisorJob() + Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()
        )
}
