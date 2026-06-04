package dev.hiorcraft.nex.discord.faq

enum class Faq(
    val id: String,
    val question: String,
    val answer: String,
    val attachmentPath: String? = null
) {
    BANNED(
        id = "banned",
        question = "Ich wurde gebannt – was nun?",
        answer = """
            Du wurdest von unserem Server gebannt? Kein Grund zur Panik – du kannst einen **Entbannungsantrag** stellen.

            **So gehst du vor:**
            1. Öffne ein Ticket über den Button im Support-Channel
            2. Wähle die Kategorie **⚖️ Unbann**
            3. Gib deine **Punish-ID**, deinen **Minecraft-Namen** und eine ehrliche **Begründung** an

            Bitte beachte: Unvollständige oder unehrliche Anträge werden abgelehnt.
            Wiederholte Verstöße können zu einem permanenten Bann führen.
        """.trimIndent()
    ),
    HOW_TO_OPEN_TICKET(
        id = "how-to-open-ticket",
        question = "Wie öffne ich ein Ticket?",
        answer = """
            Du brauchst Hilfe oder möchtest etwas melden? So öffnest du ein Ticket:

            1. Geh in den **Support-Channel** auf unserem Discord
            2. Klicke auf den Button **🎫 Ticket öffnen**
            3. Wähle die passende Kategorie aus – eine Übersicht aller Ticket-Typen findest du auf unserer Webseite:
               **https://hexoria.net/Support/**

            Ein Teammitglied wird sich so schnell wie möglich um dein Anliegen kümmern.
            Bitte öffne **kein Duplikat-Ticket**, wenn du bereits eines offen hast.
        """.trimIndent()
    ),
    RULEBOOK(
        id = "rulebook",
        question = "Wo finde ich das Regelwerk?",
        answer = """
            Das vollständige Regelwerk findest du in unserem Discord im Channel **#📜regelwerk**.

            **Die wichtigsten Grundregeln auf einen Blick:**
            • Respektvoller Umgang mit allen Spielern
            • Kein Cheaten, Exploiten oder Glitchen
            • Kein Griefing außerhalb erlaubter Zonen
            • Keine Beleidigungen, Diskriminierung oder Spam
            • Werbung für andere Server ist verboten

            Bei Verstößen drohen Verwarnungen, temporäre oder permanente Sanktionen.
            Unwissenheit schützt nicht vor Strafe – bitte lies das Regelwerk durch!
        """.trimIndent()
    ),
    CONNECTION_ISSUES(
        id = "problem-connection",
        question = "Ich kann mich nicht verbinden – was tun?",
        answer = """
            Du hast Probleme, dich mit dem Server zu verbinden? Hier sind die häufigsten Ursachen:

            **2. Server in Wartung**
            Prüfe den Channel **#📢ankündigungen** – vielleicht ist der Server gerade offline.

            **3. Verbindungsprobleme auf deiner Seite**
            • Starte Minecraft und deinen Launcher neu
            • Prüfe deine Internetverbindung
            • Deaktiviere temporär deine Firewall / VPN

            **4. Falsche Server-IP**
            Die korrekte IP lautet: **play.hexoria.net**

            Hilft nichts davon? Öffne ein **🎫 Support-Ticket** und teile deinen Log (F3 + C oder Logdatei).
        """.trimIndent()
    ),
    MAINTENANCE(
        id = "maintenance",
        question = "Der Server ist in Wartung – wann geht es weiter?",
        answer = """
            Unser Server wird regelmäßig gewartet, um die beste Spielerfahrung zu gewährleisten.

            **Wo findest du aktuelle Infos?**
            • Channel **#📢ankündigungen** – offizielle Updates vom Team

            Während der Wartung kannst du dich leider nicht verbinden.
            Wir versuchen, Ausfallzeiten so kurz wie möglich zu halten.

            Bitte öffne kein Ticket wegen Wartungsarbeiten – unsere Infos zum Status findest du immer in den genannten Channels.
        """.trimIndent()
    ),
    HOW_TO_SHARE_LOG(
        id = "how-to-share-log",
        question = "Wie teile ich meinen Log / Crash-Report?",
        answer = """
            Wenn du einen Bug meldest oder Verbindungsprobleme hast, ist dein Log sehr hilfreich. So findest du ihn:

            **Minecraft Log:**
            1. Öffne deinen Launcher-Ordner (`.minecraft` oder dein Modpack-Verzeichnis)
            2. Gehe in den Ordner **`logs`**
            3. Öffne die Datei **`latest.log`** mit einem Texteditor
            4. Kopiere den Inhalt und füge ihn auf **https://mclo.gs** ein
            5. Teile den generierten Link im Ticket

            **Crash-Report:**
            1. Gehe in den Ordner **`crash-reports`**
            2. Öffne die neueste `.txt`-Datei
            3. Teile den Inhalt ebenfalls über mclo.gs

            Bitte teile **keine** rohen Log-Texte direkt im Chat – das macht es unübersichtlich.
        """.trimIndent()
    ),
    HOW_TO_JOIN(
        id = "how-to-join",
        question = "Wie trete ich dem Server bei?",
        answer = """
            Willkommen! So wirst du Teil unserer Community:
            
            **Schritt 1 – Regelwerk lesen**
            Bitte lies das Regelwerk auf der Webseite: https://hexoria.net/Regeln
            
            **Schritt 2 – Server beitreten**
            Verbinde dich mit der IP: **play.hexoria.net**

            Bei Fragen stehen wir dir gerne per **🎫 Support-Ticket** zur Verfügung. Viel Spaß!
        """.trimIndent()
    ),
    ASK(
        id = "ask",
        question = "Wie stelle ich eine Frage richtig?",
        answer = """
            Damit wir dir schnell und gezielt helfen können, hilf uns mit ein paar Infos:

            **Bei technischen Problemen:**
            • Beschreibe das Problem so genau wie möglich
            • Wann tritt es auf? Was hast du vorher gemacht?
            • Teile deinen Log (siehe FAQ: "Wie teile ich meinen Log?")
            • Nenne deine Minecraft- und Modpack-Version

            **Bei Regelwerk-Fragen:**
            • Zitiere den betreffenden Regelwerk-Punkt wenn möglich
            • Erkläre den Kontext der Situation

            **Allgemeine Tipps:**
            • Öffne ein **Ticket** statt im allgemeinen Chat zu fragen – so bekommst du schneller Hilfe
            • Doppelposting / mehrere Tickets für das gleiche Thema verlangsamt uns
            • Sei geduldig – unser Team ist ehrenamtlich tätig
        """.trimIndent()
    )
}