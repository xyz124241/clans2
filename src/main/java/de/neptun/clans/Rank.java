package de.neptun.clans;

public enum Rank {
    MEMBER("Member", "<#d9d9d9>"),
    MOD("Mod", "<#9cc0ff>"),
    ADMIN("Admin", "<#5c8dff>"),
    OWNER("Owner", "<#ff6b6b>");

    public final String display;
    public final String color;

    Rank(String display, String color) {
        this.display = display;
        this.color = color;
    }

    public boolean atLeast(Rank other) {
        return ordinal() >= other.ordinal();
    }

    public Rank next() {
        return values()[Math.min(ordinal() + 1, values().length - 1)];
    }

    public Rank prev() {
        return values()[Math.max(ordinal() - 1, 0)];
    }

    /** MiniMessage-String mit Farbe. */
    public String colored() {
        return color + display + "<reset>";
    }
}
