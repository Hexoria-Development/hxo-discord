package dev.hexoria.hxo.discord.util

import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.modals.Modal

class ModalBuilder(private val id: String, private val title: String) {
    private val components = mutableListOf<Label>()

    fun textInput(block: TextInputBuilder.() -> Unit) {
        components.add(TextInputBuilder().apply(block).build())
    }

    fun build(): Modal {
        val builder = Modal.create(id, title)
        components.forEach { builder.addComponents(it) }
        return builder.build()
    }
}

class TextInputBuilder {
    var id: String = ""
    var label: String = ""
    var style: TextInputStyle = TextInputStyle.SHORT
    var placeholder: String = ""
    var lengthRange: IntRange? = null
    var required: Boolean = true

    fun build(): Label {
        val input = TextInput.create(id, style)
            .setPlaceholder(placeholder)
            .setRequired(required)
            .apply { lengthRange?.let { setMinLength(it.first); setMaxLength(it.last) } }
            .build()
        return Label.of(label, input)
    }
}

fun modal(id: String, title: String, block: ModalBuilder.() -> Unit): Modal =
    ModalBuilder(id, title).apply(block).build()
