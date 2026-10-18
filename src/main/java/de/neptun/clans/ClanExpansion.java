package de.neptun.clans;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * %clan_tag%     Clan-Tag farbig, mit Leerzeichen dahinter (leer ohne Clan)
 * %clan_name%    Klarname
 * %clan_rank%    Rang im Clan
 * %clan_members% Mitglieder/Limit
 */
public final class ClanExpansion extends PlaceholderExpansion {
    private final ClansPlugin plugin;

    public ClanExpansion(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clan";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Neptun";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        Clan c = plugin.manager().get(player.getUniqueId());
        if (c == null) {
            return "";
        }
        return switch (params.toLowerCase()) {
            case "tag" -> c.legacyTag + "§r ";
            case "name" -> c.name;
            case "rank" -> c.rank(player.getUniqueId()).display;
            case "members" -> c.members.size() + "/" + c.limit;
            default -> null;
        };
    }
}
