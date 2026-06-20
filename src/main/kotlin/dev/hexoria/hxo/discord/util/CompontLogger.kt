package dev.hexoria.hxo.discord.util

import net.kyori.adventure.text.logger.slf4j.ComponentLogger

inline fun <reified T : Any> componentLogger(): ComponentLogger =
    ComponentLogger.logger(T::class.java)
