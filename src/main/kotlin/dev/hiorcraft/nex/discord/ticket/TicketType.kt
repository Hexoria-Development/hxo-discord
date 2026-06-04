package dev.hiorcraft.nex.discord.ticket

import dev.hiorcraft.nex.discord.permission.DiscordPermission
import dev.hiorcraft.nex.discord.ticket.modal.modal
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.modals.Modal

private val defaultCloseReasons = listOf(
    TicketCloseReason.of("no_reason", "Kein Grund", "Es wurde kein Grund angegeben"),
    TicketCloseReason.of("resolved", "Problem gelöst", "Das Problem wurde erfolgreich gelöst"),
    TicketCloseReason.of("wrong_type", "Falscher Typ", "Das Ticket wurde im falschen Typ erstellt"),
    TicketCloseReason.of("spam", "Spam / Missbrauch", "Das Ticket wurde als Spam oder Missbrauch markiert"),
    TicketCloseReason.of("troll", "Trolling", "Das Ticket wurde zum Trollen genutzt"),
    TicketCloseReason.of("inactivity", "Inaktivität", "Das Ticket wurde wegen Inaktivität geschlossen"),
)

enum class TicketType(
    val id: String,
    val displayName: String,
    val description: String,
    val emoji: String,
    val closeReasons: List<TicketCloseReason>,
    val viewPermission: DiscordPermission,
) {
    SUPPORT(
        id = "support",
        displayName = "Support",
        description = "Allgemeines Support-Ticket für dein Anliegen.",
        emoji = "🎫",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_SUPPORT_VIEW,
    ),
    REPORT(
        id = "report",
        displayName = "Report",
        description = "Melde einen Spieler oder einen Verstoß.",
        emoji = "🚨",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_REPORT_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "reported_name"
                label = "Name des gemeldeten Spielers"
                style = TextInputStyle.SHORT
                placeholder = "Minecraft-Name des gemeldeten Spielers …"
                lengthRange = 2..64
                required = true
            }
            textInput {
                id = "reported_user"
                label = "Discord des gemeldeten Spielers"
                style = TextInputStyle.SHORT
                placeholder = "Discord-Tag oder Mention …"
                lengthRange = 2..100
                required = false
            }
            textInput {
                id = "description"
                label = "Was ist passiert?"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Beschreibe den Vorfall so genau wie möglich …"
                lengthRange = 20..1000
                required = true
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("reported_name")?.asString?.let { put("reported_name", it) }
            event.getValue("reported_user")?.asString?.takeIf { it.isNotBlank() }?.let { put("reported_user", it) }
        }
    },
    BUG(
        id = "bug",
        displayName = "Bug Report",
        description = "Melde einen Fehler im System.",
        emoji = "🐛",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("confirmed", "Bestätigt", "Der Bug wurde bestätigt und wird behoben"),
            TicketCloseReason.of("fixed", "Behoben", "Der Bug wurde erfolgreich behoben"),
            TicketCloseReason.of("not_reproducible", "Nicht reproduzierbar", "Der Bug konnte nicht reproduziert werden"),
            TicketCloseReason.of("external", "Externer Fehler", "Der Bug liegt außerhalb unseres Systems"),
        ),
        viewPermission = DiscordPermission.TICKET_BUG_VIEW,
    ),
    UNBAN(
        id = "unban",
        displayName = "Entbannungsantrag",
        description = "Stelle einen Antrag auf Entbannung.",
        emoji = "⚖️",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("unbanned", "Entbannt", "Der Spieler wurde entbannt"),
            TicketCloseReason.of("denied", "Abgelehnt", "Der Antrag wurde abgelehnt"),
            TicketCloseReason.of("shortened", "Gekürzt", "Die Strafe wurde gekürzt"),
        ),
        viewPermission = DiscordPermission.TICKET_UNBAN_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "punish_id"
                label = "Punish-ID"
                style = TextInputStyle.SHORT
                placeholder = "Die ID deiner Bestrafung (z. B. #1234) …"
                lengthRange = 1..32
                required = true
            }
            textInput {
                id = "minecraft_name"
                label = "Minecraft-Name"
                style = TextInputStyle.SHORT
                placeholder = "Dein aktueller Minecraft-Name …"
                lengthRange = 2..32
                required = true
            }
            textInput {
                id = "description"
                label = "Warum möchtest du entbannt werden?"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Erkläre, warum du entbannt werden möchtest …"
                lengthRange = 50 ..1000
                required = true
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("punish_id")?.asString?.let { put("punish_id", it) }
            event.getValue("minecraft_name")?.asString?.let { put("minecraft_name", it) }
        }
    },
    BEWERBUNG(
        id = "bewerbung",
        displayName = "Team Bewerbung",
        description = "Bewirb dich als Teammitglied.",
        emoji = "📋",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("accepted", "Angenommen", "Die Bewerbung wurde angenommen"),
            TicketCloseReason.of("rejected", "Abgelehnt", "Die Bewerbung wurde abgelehnt"),
        ),
        viewPermission = DiscordPermission.TICKET_BEWERBUNG_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "role"
                label = "Für welche Rolle bewirbst du dich?"
                style = TextInputStyle.SHORT
                placeholder = "z. B. Supporter, Moderator ..."
                lengthRange = 1..100
                required = true
            }
            textInput {
                id = "age"
                label = "Dein Alter"
                style = TextInputStyle.SHORT
                placeholder = "Wie alt bist du?"
                lengthRange = 1..20
                required = true
            }
            textInput {
                id = "experience"
                label = "Vorerfahrung"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Hast du bereits Erfahrung in einem Team gesammelt?"
                lengthRange = 20..500
                required = true
            }
            textInput {
                id = "motivation"
                label = "Warum möchtest du Teil des Teams werden?"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Begründe deine Bewerbung ..."
                lengthRange = 20..1000
                required = true
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("motivation")?.asString ?: "")
            event.getValue("role")?.asString?.let { put("role", it) }
            event.getValue("age")?.asString?.let { put("age", it) }
            event.getValue("experience")?.asString?.let { put("experience", it) }
        }
    },
    DISCORD_SUPPORT(
        id = "discord_support",
        displayName = "Discord Support",
        description = "Fragen zu Discord oder Nutzer melden.",
        emoji = "💬",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_DISCORD_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "description"
                label = "Dein Anliegen"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Frage zu Discord oder was möchtest du melden?"
                lengthRange = 20..1000
                required = true
            }
            textInput {
                id = "affected_user"
                label = "Betroffener Nutzer (optional)"
                style = TextInputStyle.SHORT
                placeholder = "Discord-Name des Nutzers, falls zutreffend"
                lengthRange = 1..100
                required = false
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("affected_user")?.asString?.takeIf { it.isNotBlank() }
                ?.let { put("affected_user", it) }
        }
    },
    EVENT_SUPPORT(
        id = "event_support",
        displayName = "Event Support",
        description = "Probleme oder Fragen rund um Events.",
        emoji = "🎉",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_SUPPORT_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "event_name"
                label = "Event Name"
                style = TextInputStyle.SHORT
                placeholder = "Um welches Event handelt es sich?"
                lengthRange = 1..100
                required = true
            }
            textInput {
                id = "description"
                label = "Dein Problem / Deine Frage"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Beschreibe dein Anliegen zum Event ..."
                lengthRange = 20..1000
                required = true
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("event_name")?.asString?.let { put("event_name", it) }
        }
    },
    TEAM_REPORT(
        id = "team_report",
        displayName = "Team Report",
        description = "Fehlverhalten eines Teammitglieds melden.",
        emoji = "🛡️",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("action_taken", "Maßnahme ergriffen", "Es wurden entsprechende Maßnahmen eingeleitet"),
            TicketCloseReason.of("unfounded", "Unbegründet", "Der Bericht war nicht ausreichend begründet"),
        ),
        viewPermission = DiscordPermission.TICKET_TEAM_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
            textInput {
                id = "team_member"
                label = "Betroffenes Teammitglied"
                style = TextInputStyle.SHORT
                placeholder = "Name des Teammitglieds"
                lengthRange = 1..100
                required = true
            }
            textInput {
                id = "description"
                label = "Was ist passiert?"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Beschreibe den Vorfall so genau wie möglich ..."
                lengthRange = 20..1000
                required = true
            }
            textInput {
                id = "evidence"
                label = "Beweise / Links (optional)"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Screenshots, Videos oder andere Beweise"
                lengthRange = 1..500
                required = false
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("team_member")?.asString?.let { put("team_member", it) }
            event.getValue("evidence")?.asString?.takeIf { it.isNotBlank() }
                ?.let { put("evidence", it) }
        }
    };

    open fun createModal(): Modal = modal("ticket:modal:$id", "$emoji $displayName – Ticket erstellen") {
        textInput {
            id = "description"
            label = "Dein Anliegen"
            style = TextInputStyle.PARAGRAPH
            placeholder = "Beschreibe dein Anliegen so genau wie möglich …"
            lengthRange = 20..1000
            required = true
        }
    }

    open fun extractFormData(event: ModalInteractionEvent): Map<String, String> =
        mapOf("description" to (event.getValue("description")?.asString ?: ""))

    fun toButton(): Button = Button.secondary("ticket:open:$id", "$emoji $displayName")

    companion object {
        fun fromId(id: String): TicketType? = entries.firstOrNull { it.id == id }
    }
}
