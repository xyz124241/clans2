package de.neptun.clans;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class Clan {
    public final String name;
    public String tag;
    public String legacyTag;
    public UUID owner;
    public final Map<UUID, Rank> members = new LinkedHashMap<>();
    public final Map<UUID, String> names = new HashMap<>();
    public final EnumMap<ClanPerm, Rank> perms = new EnumMap<>(ClanPerm.class);
    public boolean friendlyFire = false;
    public Location home;
    public int limit = 5;
    public Inventory ender;
    public byte[] enderData;

    public Clan(String name) {
        this.name = name;
        for (ClanPerm p : ClanPerm.values()) {
            perms.put(p, p.def);
        }
        setTag("<white>" + name);
    }

    public String key() {
        return name.toLowerCase(Locale.ROOT);
    }

    public void setTag(String raw) {
        this.tag = raw;
        this.legacyTag = ClanText.toLegacy(raw);
    }

    public Rank rank(UUID u) {
        return members.get(u);
    }

    public boolean can(UUID u, ClanPerm p) {
        Rank r = members.get(u);
        return r != null && r.atLeast(perms.get(p));
    }
}
