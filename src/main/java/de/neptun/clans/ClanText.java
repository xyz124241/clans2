package de.neptun.clans;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

/** Clan-Tags: eingeschraenktes MiniMessage (nur Farben, Verlauf, Regenbogen, fett/kursiv...). */
public final class ClanText {
    private static final MiniMessage RESTRICTED = MiniMessage.builder()
            .tags(TagResolver.builder()
                    .resolver(StandardTags.color())
                    .resolver(StandardTags.decorations())
                    .resolver(StandardTags.gradient())
                    .resolver(StandardTags.rainbow())
                    .build())
            .build();

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character('§')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private ClanText() {
    }

    /** @return Fehlertext oder null, wenn gueltig. */
    public static String validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return "The name cannot be empty.";
        }
        Component c;
        try {
            c = RESTRICTED.deserialize(raw);
        } catch (Exception e) {
            return "Invalid format.";
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(c);
        if (plain.length() < 1 || plain.length() > 6) {
            return "The visible name must be 1 to 6 characters.";
        }
        if (!plain.matches("[\\p{L}\\p{N}]+")) {
            return "Only letters and numbers are allowed.";
        }
        return null;
    }

    public static Component component(String raw) {
        return RESTRICTED.deserialize(raw).decoration(TextDecoration.ITALIC, false);
    }

    public static String plain(String raw) {
        return PlainTextComponentSerializer.plainText().serialize(RESTRICTED.deserialize(raw));
    }

    public static String toLegacy(String raw) {
        return LEGACY.serialize(RESTRICTED.deserialize(raw));
    }
}
