package dev.hexoria.hxo.discord.ticket

import dev.hexoria.hxo.discord.permission.DiscordPermission
import dev.hexoria.hxo.discord.util.modal
import net.dv8tion.jda.api.components.buttons.Button
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.modals.Modal

/** Ein Feld im Ticket-Container: Überschrift und Inhalt. */
data class TicketField(val label: String, val value: String)

private fun MutableList<TicketField>.field(label: String, value: String?) {
    if (!value.isNullOrBlank()) add(TicketField(label, value))
}

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
    val closeReasons: List<TicketCloseReason>,
    val viewPermission: DiscordPermission,
) {
    SUPPORT(
        id = "support",
        displayName = "Support",
        description = "Allgemeines Support-Ticket für dein Anliegen.",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_SUPPORT_VIEW,
    ),
    REPORT(
        id = "report",
        displayName = "Report",
        description = "Melde einen Spieler oder einen Verstoß.",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_REPORT_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Report wurde erstellt.\n" +
            "Ein Teamer prüft den gemeldeten Vorfall so schnell wie möglich."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Gemeldeter Spieler", data["reported_name"])
            field("Discord des gemeldeten Spielers", data["reported_user"])
            field("Was ist passiert?", data["description"])
        }
    },
    BUG(
        id = "bug",
        displayName = "Bug Report",
        description = "Melde einen Fehler im System.",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("confirmed", "Bestätigt", "Der Bug wurde bestätigt und wird behoben"),
            TicketCloseReason.of("fixed", "Behoben", "Der Bug wurde erfolgreich behoben"),
            TicketCloseReason.of("not_reproducible", "Nicht reproduzierbar", "Der Bug konnte nicht reproduziert werden"),
            TicketCloseReason.of("external", "Externer Fehler", "Der Bug liegt außerhalb unseres Systems"),
        ),
        viewPermission = DiscordPermission.TICKET_BUG_VIEW,
    ) {
        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Bugreport wurde erstellt.\n" +
            "Das Team sieht sich den Fehler so schnell wie möglich an."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Beschreibung des Fehlers", data["description"])
        }
    },
    UNBAN(
        id = "unban",
        displayName = "Entbannungsantrag",
        description = "Stelle einen Antrag auf Entbannung.",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("unbanned", "Entbannt", "Der Spieler wurde entbannt"),
            TicketCloseReason.of("denied", "Abgelehnt", "Der Antrag wurde abgelehnt"),
            TicketCloseReason.of("shortened", "Gekürzt", "Die Strafe wurde gekürzt"),
        ),
        viewPermission = DiscordPermission.TICKET_UNBAN_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Entbannungsantrag wurde erstellt.\n" +
            "Ein Teamer prüft deinen Antrag so schnell wie möglich."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Punish-ID", data["punish_id"])
            field("Minecraft-Name", data["minecraft_name"])
            field("Begründung", data["description"])
        }
    },
    BEWERBUNG(
        id = "bewerbung",
        displayName = "Team Bewerbung",
        description = "Bewirb dich als Teammitglied.",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("accepted", "Angenommen", "Die Bewerbung wurde angenommen"),
            TicketCloseReason.of("rejected", "Abgelehnt", "Die Bewerbung wurde abgelehnt"),
        ),
        viewPermission = DiscordPermission.TICKET_BEWERBUNG_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Deine Bewerbung wurde eingereicht.\n" +
            "Die Teamleitung sieht sich deine Bewerbung an und meldet sich bei dir."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Gewünschte Rolle", data["role"])
            field("Alter", data["age"])
            field("Vorerfahrung", data["experience"])
            field("Motivation", data["description"])
        }
    },
    DISCORD_SUPPORT(
        id = "discord_support",
        displayName = "Discord Support",
        description = "Fragen zu Discord oder Nutzer melden.",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_DISCORD_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Discord Support Ticket wurde erstellt.\n" +
            "Ein Teamer wird sich so schnell wie möglich um dein Anliegen kümmern."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Betroffener Nutzer", data["affected_user"])
            field("Dein Anliegen", data["description"])
        }
    },
    EVENT_SUPPORT(
        id = "event_support",
        displayName = "Event Support",
        description = "Probleme oder Fragen rund um Events.",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_SUPPORT_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Event Support Ticket wurde erstellt.\n" +
            "Das Event-Team wird sich so schnell wie möglich um dein Anliegen kümmern."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Event", data["event_name"])
            field("Dein Anliegen", data["description"])
        }
    },
    CONTENT_SUPPORT(
        id = "content_support",
        displayName = "Content Support",
        description = "Support für Content Creator.",
        closeReasons = defaultCloseReasons,
        viewPermission = DiscordPermission.TICKET_CONTENT_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
            textInput {
                id = "content_type"
                label = "Art des Contents"
                style = TextInputStyle.SHORT
                placeholder = "z. B. Video, Stream, Social Media ..."
                lengthRange = 1..100
                required = true
            }
            textInput {
                id = "description"
                label = "Dein Anliegen"
                style = TextInputStyle.PARAGRAPH
                placeholder = "Beschreibe dein Anliegen ..."
                lengthRange = 20..1000
                required = true
            }
        }

        override fun extractFormData(event: ModalInteractionEvent) = buildMap {
            put("description", event.getValue("description")?.asString ?: "")
            event.getValue("content_type")?.asString?.let { put("content_type", it) }
        }

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Dein Content Support Ticket wurde erstellt.\n" +
            "Das Management wird sich so schnell wie möglich um dein Anliegen kümmern."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Art des Contents", data["content_type"])
            field("Dein Anliegen", data["description"])
        }
    },
    TEAM_REPORT(
        id = "team_report",
        displayName = "Team Report",
        description = "Fehlverhalten eines Teammitglieds melden.",
        closeReasons = defaultCloseReasons + listOf(
            TicketCloseReason.of("action_taken", "Maßnahme ergriffen", "Es wurden entsprechende Maßnahmen eingeleitet"),
            TicketCloseReason.of("unfounded", "Unbegründet", "Der Bericht war nicht ausreichend begründet"),
        ),
        viewPermission = DiscordPermission.TICKET_TEAM_VIEW,
    ) {
        override fun createModal() = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

        override fun welcomeText(authorId: Long) =
            "Willkommen <@$authorId>! Deine Meldung wurde erstellt.\n" +
            "Nur die Teamleitung kann dieses Ticket einsehen."

        override fun displayFields(data: Map<String, String>) = buildList {
            field("Betroffenes Teammitglied", data["team_member"])
            field("Was ist passiert?", data["description"])
            field("Beweise / Links", data["evidence"])
        }
    };

    open fun createModal(): Modal = modal("ticket:modal:$id", "$displayName – Ticket erstellen") {
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

    /** Begrüßungstext, der im Ticket-Container über den Feldern steht. */
    open fun welcomeText(authorId: Long): String =
        "Willkommen <@$authorId>! Dein Ticket wurde erstellt.\n" +
        "Ein Teamer wird sich so schnell wie möglich um dein Anliegen kümmern."

    /** Felder, die im Ticket-Container angezeigt werden. */
    open fun displayFields(data: Map<String, String>): List<TicketField> = buildList {
        field("Dein Anliegen", data["description"])
    }

    fun toButton(): Button = Button.secondary("ticket:open:$id", displayName)

    companion object {
        fun fromId(id: String): TicketType? = entries.firstOrNull { it.id == id }
    }
}
