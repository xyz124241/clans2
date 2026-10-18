package de.neptun.clans;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public final class Msg {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gradient:#2d6bff:#e5edff><bold>Clan</bold></gradient> <#bbbbbb>» <white>";

    private Msg() {
    }

    /** Rueckmeldungen erscheinen ueber der Hotbar (Action Bar), nicht im Chat. */
    public static void send(CommandSender to, String mm) {
        to.sendActionBar(MM.deserialize(PREFIX + mm));
    }

    /** Echte Chat-Nachricht (nur fuer Einladung und Hilfe). */
    public static void chat(CommandSender to, String mm) {
        to.sendMessage(MM.deserialize(PREFIX + mm));
    }

    /** Komponente ohne Kursiv (fuer Item-Namen). */
    public static Component c(String mm) {
        return MM.deserialize(mm).decoration(TextDecoration.ITALIC, false);
    }
}
