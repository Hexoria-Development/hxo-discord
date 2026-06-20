package dev.hexoria.hxo.discord.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.interactions.InteractionHook
import java.awt.Color
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

fun embed(block: EmbedBuilder.() -> Unit): MessageEmbed =
    EmbedBuilder().apply(block).build()

val COLOR_SUCCESS = Color(0x57F287)
val COLOR_ERROR   = Color(0xED4245)
val COLOR_INFO    = Color(0x5865F2)
val COLOR_WARNING = Color(0xFEE75C)

fun successEmbed(title: String, description: String) = embed {
    setTitle("✅ $title")
    setDescription(description)
    setColor(COLOR_SUCCESS)
    setTimestamp(Instant.now())
}

fun errorEmbed(title: String, description: String) = embed {
    setTitle("❌ $title")
    setDescription(description)
    setColor(COLOR_ERROR)
    setTimestamp(Instant.now())
}

fun infoEmbed(title: String, description: String) = embed {
    setTitle("ℹ️ $title")
    setDescription(description)
    setColor(COLOR_INFO)
    setTimestamp(Instant.now())
}

fun InteractionHook.deleteOriginalAfter(scope: CoroutineScope) {
    scope.launch {
        delay(30.seconds)
        deleteOriginal().queue(null) { }
    }
}
