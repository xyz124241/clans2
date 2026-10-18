package de.neptun.clans;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ClanManager {
    private final ClansPlugin plugin;
    private final File file;
    private final Map<String, Clan> clans = new LinkedHashMap<>();
    private final Map<UUID, Clan> byPlayer = new HashMap<>();
    /** Einladungen: Spieler -> (Clan-Key -> Ablaufzeit in ms). */
    private final Map<UUID, Map<String, Long>> invites = new HashMap<>();
    /** Statistik: Spieler -> [Kills, Tode]. */
    private final Map<UUID, int[]> stats = new HashMap<>();

    public ClanManager(ClansPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "clans.yml");
    }

    // ---------- Zugriff ----------

    public Clan get(UUID player) {
        return byPlayer.get(player);
    }

    public Clan byName(String name) {
        return clans.get(name.toLowerCase(Locale.ROOT));
    }

    public Iterable<Clan> all() {
        return clans.values();
    }

    public Clan create(Player owner, String name) {
        Clan c = new Clan(name);
        c.owner = owner.getUniqueId();
        c.members.put(owner.getUniqueId(), Rank.OWNER);
        c.names.put(owner.getUniqueId(), owner.getName());
        c.limit = computeLimit(owner);
        clans.put(c.key(), c);
        byPlayer.put(owner.getUniqueId(), c);
        save();
        return c;
    }

    public void delete(Clan c) {
        for (UUID u : new ArrayList<>(c.members.keySet())) {
            byPlayer.remove(u);
        }
        clans.remove(c.key());
        for (Map<String, Long> m : invites.values()) {
            m.remove(c.key());
        }
        save();
    }

    public void addMember(Clan c, Player p) {
        c.members.put(p.getUniqueId(), Rank.MEMBER);
        c.names.put(p.getUniqueId(), p.getName());
        byPlayer.put(p.getUniqueId(), c);
        save();
    }

    public void removeMember(Clan c, UUID u) {
        c.members.remove(u);
        byPlayer.remove(u);
        save();
    }

    // ---------- K/D ----------

    private int[] st(UUID u) {
        return stats.computeIfAbsent(u, k -> new int[2]);
    }

    public int kills(UUID u) {
        return st(u)[0];
    }

    public int deaths(UUID u) {
        return st(u)[1];
    }

    public void addKill(UUID u) {
        st(u)[0]++;
    }

    public void addDeath(UUID u) {
        st(u)[1]++;
    }

    /** ratio = Kills / Tode (ohne Tode: Kills). average = (Kills + Tode) / 2. */
    public double kd(UUID u) {
        int k = kills(u);
        int d = deaths(u);
        if ("average".equalsIgnoreCase(plugin.getConfig().getString("kd-mode", "ratio"))) {
            return (k + d) / 2.0;
        }
        return d == 0 ? k : (double) k / d;
    }

    /** Clan-K/D: Summe der Spieler-K/Ds geteilt durch die Anzahl der Mitglieder. */
    public double clanKd(Clan c) {
        if (c.members.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (UUID u : c.members.keySet()) {
            sum += kd(u);
        }
        return sum / c.members.size();
    }

    public static String fmt(double d) {
        return String.format(Locale.ROOT, "%.2f", d);
    }

    // ---------- Limit ----------

    public int computeLimit(Player owner) {
        int best = plugin.getConfig().getInt("default-limit", 5);
        ConfigurationSection s = plugin.getConfig().getConfigurationSection("limits");
        if (s == null || Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return best;
        }
        try {
            java.util.Set<String> groups = LuckPermsHook.groups(owner);
            for (String group : s.getKeys(false)) {
                if (groups.contains(group.toLowerCase(Locale.ROOT))) {
                    best = Math.max(best, s.getInt(group));
                }
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("LuckPerms-Gruppen konnten nicht gelesen werden: " + t.getMessage());
        }
        return best;
    }

    /** Aktualisiert das Limit, wenn der Owner online ist. */
    public void refreshLimit(Clan c) {
        Player o = Bukkit.getPlayer(c.owner);
        if (o != null) {
            c.limit = computeLimit(o);
        }
    }

    // ---------- Einladungen ----------

    public void invite(Player target, Clan c) {
        long ms = plugin.getConfig().getInt("invite-seconds", 60) * 1000L;
        invites.computeIfAbsent(target.getUniqueId(), k -> new LinkedHashMap<>())
                .put(c.key(), System.currentTimeMillis() + ms);
    }

    /** Gueltige Einladungen (Clan-Keys), neueste zuletzt. */
    public List<String> pendingInvites(UUID target) {
        Map<String, Long> m = invites.get(target);
        List<String> out = new ArrayList<>();
        if (m == null) {
            return out;
        }
        long now = System.currentTimeMillis();
        m.entrySet().removeIf(e -> e.getValue() < now || !clans.containsKey(e.getKey()));
        out.addAll(m.keySet());
        return out;
    }

    public void clearInvites(UUID target) {
        invites.remove(target);
    }

    // ---------- Speichern / Laden ----------

    public void load() {
        clans.clear();
        byPlayer.clear();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection ss = y.getConfigurationSection("stats");
        if (ss != null) {
            for (String u : ss.getKeys(false)) {
                try {
                    stats.put(UUID.fromString(u), new int[]{ss.getInt(u + ".k"), ss.getInt(u + ".d")});
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        ConfigurationSection root = y.getConfigurationSection("clans");
        if (root == null) {
            return;
        }
        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) {
                continue;
            }
            try {
                Clan c = new Clan(s.getString("name", key));
                c.owner = UUID.fromString(s.getString("owner"));
                String tag = s.getString("tag");
                if (tag != null && ClanText.validate(tag) == null) {
                    c.setTag(tag);
                }
                c.limit = s.getInt("limit", 5);
                c.friendlyFire = s.getBoolean("friendly-fire", false);
                ConfigurationSection ms = s.getConfigurationSection("members");
                if (ms != null) {
                    for (String u : ms.getKeys(false)) {
                        UUID id = UUID.fromString(u);
                        c.members.put(id, Rank.valueOf(ms.getString(u + ".rank", "MEMBER")));
                        c.names.put(id, ms.getString(u + ".name", "?"));
                        byPlayer.put(id, c);
                    }
                }
                ConfigurationSection ps = s.getConfigurationSection("perms");
                if (ps != null) {
                    for (ClanPerm p : ClanPerm.values()) {
                        String r = ps.getString(p.name());
                        if (r != null) {
                            c.perms.put(p, Rank.valueOf(r));
                        }
                    }
                }
                ConfigurationSection hs = s.getConfigurationSection("home");
                if (hs != null) {
                    World w = Bukkit.getWorld(hs.getString("world", ""));
                    if (w != null) {
                        c.home = new Location(w, hs.getDouble("x"), hs.getDouble("y"), hs.getDouble("z"),
                                (float) hs.getDouble("yaw"), (float) hs.getDouble("pitch"));
                    }
                }
                String ender = s.getString("ender");
                if (ender != null && !ender.isEmpty()) {
                    c.enderData = Base64.getDecoder().decode(ender);
                }
                clans.put(c.key(), c);
            } catch (Exception ex) {
                plugin.getLogger().warning("Clan '" + key + "' konnte nicht geladen werden: " + ex.getMessage());
            }
        }
    }

    public void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Map.Entry<UUID, int[]> e : stats.entrySet()) {
            y.set("stats." + e.getKey() + ".k", e.getValue()[0]);
            y.set("stats." + e.getKey() + ".d", e.getValue()[1]);
        }
        for (Clan c : clans.values()) {
            String p = "clans." + c.key();
            y.set(p + ".name", c.name);
            y.set(p + ".tag", c.tag);
            y.set(p + ".owner", c.owner.toString());
            y.set(p + ".limit", c.limit);
            y.set(p + ".friendly-fire", c.friendlyFire);
            for (Map.Entry<UUID, Rank> e : c.members.entrySet()) {
                y.set(p + ".members." + e.getKey() + ".rank", e.getValue().name());
                y.set(p + ".members." + e.getKey() + ".name", c.names.getOrDefault(e.getKey(), "?"));
            }
            for (ClanPerm perm : ClanPerm.values()) {
                y.set(p + ".perms." + perm.name(), c.perms.get(perm).name());
            }
            if (c.home != null && c.home.getWorld() != null) {
                y.set(p + ".home.world", c.home.getWorld().getName());
                y.set(p + ".home.x", c.home.getX());
                y.set(p + ".home.y", c.home.getY());
                y.set(p + ".home.z", c.home.getZ());
                y.set(p + ".home.yaw", c.home.getYaw());
                y.set(p + ".home.pitch", c.home.getPitch());
            }
            byte[] data = c.enderData;
            if (c.ender != null) {
                ItemStack[] contents = c.ender.getContents();
                data = ItemStack.serializeItemsAsBytes(contents);
                c.enderData = data;
            }
            if (data != null) {
                y.set(p + ".ender", Base64.getEncoder().encodeToString(data));
            }
        }
        try {
            plugin.getDataFolder().mkdirs();
            y.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("clans.yml konnte nicht gespeichert werden: " + e.getMessage());
        }
    }
}
