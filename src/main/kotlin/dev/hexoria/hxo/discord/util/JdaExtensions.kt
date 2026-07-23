package dev.hexoria.hxo.discord.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.interactions.InteractionHook
import java.awt.Color
import kotlin.time.Duration.Companion.seconds

val COLOR_SUCCESS = Color(0x57F287)
val COLOR_ERROR   = Color(0xED4245)
val COLOR_INFO    = Color(0x5865F2)
val COLOR_WARNING = Color(0xFEE75C)

fun InteractionHook.deleteOriginalAfter(scope: CoroutineScope) {
    scope.launch {
        delay(30.seconds)
        deleteOriginal().queue(null) { }
    }
}
