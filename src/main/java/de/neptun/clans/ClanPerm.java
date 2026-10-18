package de.neptun.clans;

import org.bukkit.Material;

/** Einstellbare Rechte: ab welchem Rang darf man das? */
public enum ClanPerm {
    INVITE("Invite players", Rank.MOD, Material.WRITABLE_BOOK),
    MANAGE("Kick players", Rank.ADMIN, Material.IRON_BOOTS),
    ENDERCHEST("Open ender chest", Rank.MEMBER, Material.ENDER_CHEST),
    SETHOME("Set clan home", Rank.ADMIN, Material.WHITE_BED),
    HOME("Teleport to clan home", Rank.MEMBER, Material.ENDER_PEARL),
    NAMETAG("Change name tag", Rank.OWNER, Material.NAME_TAG);

    public final String label;
    public final Rank def;
    public final Material icon;

    ClanPerm(String label, Rank def, Material icon) {
        this.label = label;
        this.def = def;
        this.icon = icon;
    }
}
