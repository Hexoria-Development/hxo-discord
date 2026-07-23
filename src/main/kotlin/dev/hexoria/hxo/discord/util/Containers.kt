package dev.hexoria.hxo.discord.util

import net.dv8tion.jda.api.components.actionrow.ActionRow
import net.dv8tion.jda.api.components.actionrow.ActionRowChildComponent
import net.dv8tion.jda.api.components.container.Container
import net.dv8tion.jda.api.components.container.ContainerChildComponent
import net.dv8tion.jda.api.components.mediagallery.MediaGallery
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem
import net.dv8tion.jda.api.components.section.Section
import net.dv8tion.jda.api.components.section.SectionContentComponent
import net.dv8tion.jda.api.components.separator.Separator
import net.dv8tion.jda.api.components.textdisplay.TextDisplay
import net.dv8tion.jda.api.components.thumbnail.Thumbnail
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.interactions.InteractionHook
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback
import net.dv8tion.jda.api.utils.FileUpload
import java.awt.Color
import java.time.Instant

/** Inhalt einer [Section] – Text neben dem Thumbnail. */
class SectionBuilder {
    internal val contents = mutableListOf<SectionContentComponent>()

    fun text(content: String) {
        contents += TextDisplay.of(content)
    }

    fun header(title: String) = text("## $title")
}

/**
 * Baut einen Components-V2-[Container]. Ersetzt die früheren Embeds:
 * Überschriften sind Markdown (`##`), Felder werden durch [divider] getrennt,
 * Fußnoten laufen über `-#`.
 */
class ContainerBuilder {
    private val children = mutableListOf<ContainerChildComponent>()

    var accentColor: Color? = null

    fun text(content: String) {
        children += TextDisplay.of(content)
    }

    fun header(title: String) = text("## $title")

    fun subHeader(title: String) = text("### $title")

    /** Fußzeile. Mehrere Teile werden mit `•` getrennt. */
    fun footer(vararg notes: String) = text("-# " + notes.joinToString(" • "))

    fun timestamp(instant: Instant = Instant.now()) = footer(discordTime(instant))

    /** Aktueller Zeitpunkt als Discord-Timestamp – z. B. als letzter Teil einer [footer]-Zeile. */
    val now: String get() = discordTime(Instant.now())

    /** Ein Feld mit fetter Überschrift. Leere Werte werden weggelassen. */
    fun field(label: String, value: String?) {
        if (!value.isNullOrBlank()) text("**$label**\n$value")
    }

    fun divider(spacing: Separator.Spacing = Separator.Spacing.SMALL) {
        children += Separator.createDivider(spacing)
    }

    fun spacer(spacing: Separator.Spacing = Separator.Spacing.SMALL) {
        children += Separator.createInvisible(spacing)
    }

    fun buttons(vararg components: ActionRowChildComponent) {
        children += ActionRow.of(components.toList())
    }

    fun row(row: ActionRow) {
        children += row
    }

    fun image(url: String) {
        children += MediaGallery.of(MediaGalleryItem.fromUrl(url))
    }

    fun image(file: FileUpload) {
        children += MediaGallery.of(MediaGalleryItem.fromFile(file))
    }

    /** Kopfbereich mit Bild rechts daneben. Ohne Bild-URL bleiben nur die Texte übrig. */
    fun section(thumbnailUrl: String?, block: SectionBuilder.() -> Unit) {
        val builder = SectionBuilder().apply(block)
        if (thumbnailUrl == null) {
            children += builder.contents.filterIsInstance<ContainerChildComponent>()
        } else {
            children += Section.of(Thumbnail.fromUrl(thumbnailUrl), builder.contents)
        }
    }

    fun build(): Container {
        val container = Container.of(children)
        return accentColor?.let { container.withAccentColor(it) } ?: container
    }
}

fun container(block: ContainerBuilder.() -> Unit): Container =
    ContainerBuilder().apply(block).build()

/** Zeitpunkt im Discord-Format (`<t:…:f>`), das die Zeitzone des Betrachters berücksichtigt. */
fun discordTime(instant: Instant = Instant.now()): String = "<t:${instant.epochSecond}:f>"

fun successContainer(title: String, description: String) = container {
    accentColor = COLOR_SUCCESS
    header("✅ $title")
    text(description)
}

fun errorContainer(title: String, description: String) = container {
    accentColor = COLOR_ERROR
    header("❌ $title")
    text(description)
}

fun infoContainer(title: String, description: String) = container {
    accentColor = COLOR_INFO
    header("ℹ️ $title")
    text(description)
}

fun warningContainer(title: String, description: String) = container {
    accentColor = COLOR_WARNING
    header("⚠️ $title")
    text(description)
}

fun IReplyCallback.replyContainers(vararg containers: Container) =
    replyComponents(containers.toList()).useComponentsV2()

fun InteractionHook.editContainers(vararg containers: Container) =
    editOriginalComponents(containers.toList()).useComponentsV2()

fun InteractionHook.sendContainers(vararg containers: Container) =
    sendMessageComponents(containers.toList()).useComponentsV2()

fun MessageChannel.sendContainers(vararg containers: Container) =
    sendMessageComponents(containers.toList()).useComponentsV2()

/**
 * Wie [sendContainers], löst aber keine Mentions auf.
 *
 * Anders als in Embeds pingen `@user`- und `@role`-Erwähnungen in Text-Komponenten wirklich –
 * für Log-Channels und Panels ist das unerwünscht.
 */
fun MessageChannel.sendSilentContainers(vararg containers: Container) =
    sendContainers(*containers).setAllowedMentions(emptyList())

fun Message.replyContainers(vararg containers: Container) =
    replyComponents(containers.toList()).useComponentsV2()
