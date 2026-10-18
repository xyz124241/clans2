package de.neptun.clans;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.inventory.view.AnvilView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** All menus. Colors: only white/gray/blue - nothing green or yellow. */
public final class Gui {
    static final String ARROW = "<#9a9a9a>»";
    private final ClansPlugin plugin;
    /** Players currently typing a new tag in the anvil. */
    final Map<UUID, Clan> anvilSessions = new HashMap<>();

    public Gui(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    // ---------- Helpers ----------

    static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Msg.c(name));
        if (lore.length > 0) {
            meta.lore(Arrays.stream(lore).map(Msg::c).toList());
        }
        meta.addItemFlags(ItemFlag.values());
        it.setItemMeta(meta);
        return it;
    }

    static void fill(Inventory inv) {
        ItemStack pane = item(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 0; i < inv.getSize(); i++) {
            inv.setItem(i, pane);
        }
    }

    static String yesNo(boolean b) {
        return b ? "<white>Yes" : "<#7a7a7a>No";
    }

    static String title(String text) {
        return "<#9a9a9a>Clan " + ARROW + " <white>" + text;
    }

    ItemStack head(UUID id, String name, Rank r, String... extra) {
        ClanManager m = plugin.manager();
        List<String> lore = new ArrayList<>();
        lore.add("<gray>Rank: " + r.colored());
        lore.add("<gray>K/D: <white>" + ClanManager.fmt(m.kd(id)));
        lore.add("<gray>Kills: <white>" + m.kills(id) + " <gray>Deaths: <white>" + m.deaths(id));
        lore.addAll(Arrays.asList(extra));
        ItemStack head = item(Material.PLAYER_HEAD, r.color + "<bold>" + name, lore.toArray(new String[0]));
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(Bukkit.getOfflinePlayer(id));
        head.setItemMeta(sm);
        return head;
    }

    public void closeAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            InventoryHolder h = p.getOpenInventory().getTopInventory().getHolder();
            if (h instanceof ClanMenu || h instanceof EnderHolder || anvilSessions.containsKey(p.getUniqueId())) {
                p.closeInventory();
            }
        }
    }

    // ---------- Opening ----------

    public void openMain(Player p) {
        Clan c = plugin.manager().get(p.getUniqueId());
        if (c == null) {
            return;
        }
        p.openInventory(new MainMenu(this, c, p).getInventory());
    }

    public void openEnder(Player p, Clan c) {
        if (c.ender == null) {
            EnderHolder h = new EnderHolder();
            Inventory inv = Bukkit.createInventory(h, 27, Msg.c(title("Ender Chest")));
            if (c.enderData != null) {
                try {
                    inv.setContents(ItemStack.deserializeItemsFromBytes(c.enderData));
                } catch (Exception ex) {
                    plugin.getLogger().warning("Could not load the ender chest of " + c.name + ".");
                }
            }
            h.inv = inv;
            h.clan = c;
            c.ender = inv;
        }
        p.openInventory(c.ender);
    }

    public void openTagInput(Player p, Clan c) {
        // Close the old menu first, then register the session (closing would remove it otherwise)
        p.closeInventory();
        AnvilView view = MenuType.ANVIL.create(p, Msg.c(title("Name Tag")));
        view.getTopInventory().setFirstItem(item(Material.PAPER, ClanText.plain(c.tag)));
        anvilSessions.put(p.getUniqueId(), c);
        p.openInventory(view);
    }

    // ---------- Menu base ----------

    abstract static class ClanMenu implements InventoryHolder {
        final Gui gui;
        final Clan clan;
        Inventory inv;

        ClanMenu(Gui gui, Clan clan) {
            this.gui = gui;
            this.clan = clan;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }

        abstract void click(Player p, int slot, ClickType type);
    }

    static final class EnderHolder implements InventoryHolder {
        Inventory inv;
        Clan clan;

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    // ---------- Main menu ----------

    static final class MainMenu extends ClanMenu {
        MainMenu(Gui gui, Clan clan, Player p) {
            super(gui, clan);
            inv = Bukkit.createInventory(this, 27, Msg.c(title("<bold>" + clan.name)));
            build(p);
        }

        void build(Player p) {
            fill(inv);
            UUID u = p.getUniqueId();
            Rank r = clan.rank(u);
            ClanManager m = gui.plugin.manager();
            inv.setItem(4, item(Material.NETHER_STAR, "<white><bold>" + clan.name,
                    "<gray>Members: <white>" + clan.members.size() + "/" + clan.limit,
                    "<gray>Clan K/D: <white>" + ClanManager.fmt(m.clanKd(clan)),
                    "<gray>Your rank: " + r.colored(),
                    "<gray>Tag: <white>" + clan.tag));
            inv.setItem(10, item(Material.WHITE_BED, "<white><bold>Clan Home",
                    "<gray>Left click: <white>Teleport",
                    "<gray>Right click: <white>Set home",
                    " ",
                    "<gray>Teleport: " + yesNo(clan.can(u, ClanPerm.HOME)),
                    "<gray>Set: " + yesNo(clan.can(u, ClanPerm.SETHOME))));
            inv.setItem(12, item(Material.ENDER_CHEST, "<white><bold>Ender Chest",
                    "<gray>Access: " + yesNo(clan.can(u, ClanPerm.ENDERCHEST))));
            inv.setItem(14, item(Material.NAME_TAG, "<white><bold>Name Tag",
                    "<gray>Access: " + yesNo(clan.can(u, ClanPerm.NAMETAG))));
            inv.setItem(16, item(Material.PLAYER_HEAD, "<white><bold>Members",
                    "<gray>" + clan.members.size() + "/" + clan.limit));
            if (r == Rank.OWNER) {
                inv.setItem(20, item(Material.COMPARATOR, "<white><bold>Permissions"));
            }
            inv.setItem(24, item(Material.IRON_SWORD, "<white><bold>Top Clans",
                    "<gray>Best 5 by K/D"));
        }

        @Override
        void click(Player p, int slot, ClickType type) {
            UUID u = p.getUniqueId();
            switch (slot) {
                case 10 -> {
                    if (type.isRightClick()) {
                        if (!clan.can(u, ClanPerm.SETHOME)) {
                            Msg.send(p, "<#ff7b7b>No permission.");
                            return;
                        }
                        clan.home = p.getLocation();
                        gui.plugin.manager().save();
                        Msg.send(p, "Clan home set.");
                        build(p);
                    } else {
                        if (!clan.can(u, ClanPerm.HOME)) {
                            Msg.send(p, "<#ff7b7b>No permission.");
                            return;
                        }
                        if (clan.home == null) {
                            Msg.send(p, "<#ff7b7b>No clan home set.");
                            return;
                        }
                        p.closeInventory();
                        p.teleportAsync(clan.home);
                        Msg.send(p, "Teleported.");
                    }
                }
                case 12 -> {
                    if (!clan.can(u, ClanPerm.ENDERCHEST)) {
                        Msg.send(p, "<#ff7b7b>No permission.");
                        return;
                    }
                    gui.openEnder(p, clan);
                }
                case 14 -> {
                    if (!clan.can(u, ClanPerm.NAMETAG)) {
                        Msg.send(p, "<#ff7b7b>No permission.");
                        return;
                    }
                    gui.openTagInput(p, clan);
                }
                case 16 -> p.openInventory(new MembersMenu(gui, clan, p).getInventory());
                case 20 -> {
                    if (clan.rank(u) == Rank.OWNER) {
                        p.openInventory(new PermsMenu(gui, clan).getInventory());
                    }
                }
                case 24 -> p.openInventory(new TopMenu(gui, clan).getInventory());
                default -> {
                }
            }
        }
    }

    // ---------- Members ----------

    static boolean canManage(Clan clan, UUID viewer, UUID target) {
        Rank v = clan.rank(viewer);
        Rank t = clan.rank(target);
        if (v == null || t == null || viewer.equals(target) || t == Rank.OWNER) {
            return false;
        }
        if (v == Rank.OWNER) {
            return true;
        }
        return clan.can(viewer, ClanPerm.MANAGE) && v.ordinal() > t.ordinal();
    }

    static final class MembersMenu extends ClanMenu {
        private final Map<Integer, UUID> slots = new HashMap<>();

        MembersMenu(Gui gui, Clan clan, Player p) {
            super(gui, clan);
            inv = Bukkit.createInventory(this, 54, Msg.c(title("Members")));
            build(p);
        }

        void build(Player p) {
            fill(inv);
            slots.clear();
            List<UUID> sorted = new ArrayList<>(clan.members.keySet());
            sorted.sort((a, b) -> {
                int c = Integer.compare(clan.rank(b).ordinal(), clan.rank(a).ordinal());
                return c != 0 ? c : clan.names.getOrDefault(a, "").compareToIgnoreCase(clan.names.getOrDefault(b, ""));
            });
            int slot = 0;
            for (UUID id : sorted) {
                if (slot >= 45) {
                    break;
                }
                boolean manage = canManage(clan, p.getUniqueId(), id);
                inv.setItem(slot, gui.head(id, clan.names.getOrDefault(id, "?"), clan.rank(id),
                        manage ? "<#9a9a9a>Click to manage" : " "));
                slots.put(slot, id);
                slot++;
            }
            inv.setItem(49, item(Material.ARROW, "<white>Back"));
        }

        @Override
        void click(Player p, int slot, ClickType type) {
            if (slot == 49) {
                gui.openMain(p);
                return;
            }
            UUID target = slots.get(slot);
            if (target != null && canManage(clan, p.getUniqueId(), target)) {
                p.openInventory(new MemberMenu(gui, clan, p, target).getInventory());
            }
        }
    }

    static final class MemberMenu extends ClanMenu {
        private final UUID target;

        MemberMenu(Gui gui, Clan clan, Player p, UUID target) {
            super(gui, clan);
            this.target = target;
            inv = Bukkit.createInventory(this, 27, Msg.c(title(clan.names.getOrDefault(target, "?"))));
            build(p);
        }

        void build(Player p) {
            fill(inv);
            Rank r = clan.rank(target);
            inv.setItem(4, gui.head(target, clan.names.getOrDefault(target, "?"), r));
            boolean owner = clan.rank(p.getUniqueId()) == Rank.OWNER;
            if (owner) {
                inv.setItem(10, item(Material.LIME_DYE, "<white><bold>Promote",
                        r.ordinal() < Rank.ADMIN.ordinal() ? "<gray>" + r.colored() + " <gray>» " + r.next().colored()
                                : "<#7a7a7a>Highest rank"));
                inv.setItem(12, item(Material.GRAY_DYE, "<white><bold>Demote",
                        r.ordinal() > Rank.MEMBER.ordinal() ? "<gray>" + r.colored() + " <gray>» " + r.prev().colored()
                                : "<#7a7a7a>Lowest rank"));
            }
            inv.setItem(14, item(Material.BARRIER, "<white><bold>Kick"));
            inv.setItem(22, item(Material.ARROW, "<white>Back"));
        }

        @Override
        void click(Player p, int slot, ClickType type) {
            UUID me = p.getUniqueId();
            if (slot == 22) {
                p.openInventory(new MembersMenu(gui, clan, p).getInventory());
                return;
            }
            if (!canManage(clan, me, target) || clan.rank(target) == null) {
                p.openInventory(new MembersMenu(gui, clan, p).getInventory());
                return;
            }
            Rank r = clan.rank(target);
            boolean owner = clan.rank(me) == Rank.OWNER;
            switch (slot) {
                case 10 -> {
                    if (owner && r.ordinal() < Rank.ADMIN.ordinal()) {
                        clan.members.put(target, r.next());
                        gui.plugin.manager().save();
                        build(p);
                    }
                }
                case 12 -> {
                    if (owner && r.ordinal() > Rank.MEMBER.ordinal()) {
                        clan.members.put(target, r.prev());
                        gui.plugin.manager().save();
                        build(p);
                    }
                }
                case 14 -> {
                    String name = clan.names.getOrDefault(target, "?");
                    gui.plugin.manager().removeMember(clan, target);
                    Player t = Bukkit.getPlayer(target);
                    if (t != null) {
                        t.closeInventory();
                        Msg.send(t, "You were kicked from the clan.");
                    }
                    Msg.send(p, "<bold>" + name + "</bold> was kicked.");
                    p.openInventory(new MembersMenu(gui, clan, p).getInventory());
                }
                default -> {
                }
            }
        }
    }

    // ---------- Permissions ----------

    static final class PermsMenu extends ClanMenu {
        private static final int[] SLOTS = {10, 11, 12, 13, 14, 15};

        PermsMenu(Gui gui, Clan clan) {
            super(gui, clan);
            inv = Bukkit.createInventory(this, 27, Msg.c(title("Permissions")));
            build();
        }

        void build() {
            fill(inv);
            ClanPerm[] perms = ClanPerm.values();
            for (int i = 0; i < perms.length; i++) {
                ClanPerm p = perms[i];
                inv.setItem(SLOTS[i], item(p.icon, "<white><bold>" + p.label,
                        "<gray>From rank: " + clan.perms.get(p).colored(),
                        "<gray>Left: <white>higher <gray>Right: <white>lower"));
            }
            inv.setItem(16, item(Material.IRON_SWORD, "<white><bold>Friendly fire",
                    "<gray>Enabled: " + yesNo(clan.friendlyFire)));
            inv.setItem(22, item(Material.ARROW, "<white>Back"));
        }

        @Override
        void click(Player p, int slot, ClickType type) {
            if (clan.rank(p.getUniqueId()) != Rank.OWNER) {
                p.closeInventory();
                return;
            }
            if (slot == 22) {
                gui.openMain(p);
                return;
            }
            if (slot == 16) {
                clan.friendlyFire = !clan.friendlyFire;
                gui.plugin.manager().save();
                build();
                return;
            }
            ClanPerm[] perms = ClanPerm.values();
            for (int i = 0; i < perms.length; i++) {
                if (SLOTS[i] == slot) {
                    Rank r = clan.perms.get(perms[i]);
                    clan.perms.put(perms[i], type.isRightClick() ? r.prev() : r.next());
                    gui.plugin.manager().save();
                    build();
                    return;
                }
            }
        }
    }

    // ---------- Top clans ----------

    static final class TopMenu extends ClanMenu {
        private static final int[] SLOTS = {11, 12, 13, 14, 15};

        TopMenu(Gui gui, Clan clan) {
            super(gui, clan);
            inv = Bukkit.createInventory(this, 27, Msg.c(title("Top Clans")));
            build();
        }

        void build() {
            fill(inv);
            ClanManager m = gui.plugin.manager();
            List<Clan> list = new ArrayList<>();
            for (Clan c : m.all()) {
                list.add(c);
            }
            list.sort((a, b) -> Double.compare(m.clanKd(b), m.clanKd(a)));
            for (int i = 0; i < Math.min(5, list.size()); i++) {
                Clan c = list.get(i);
                inv.setItem(SLOTS[i], item(Material.SHIELD, "<white><bold>#" + (i + 1) + "</bold> " + c.tag,
                        "<gray>Clan K/D: <white>" + ClanManager.fmt(m.clanKd(c)),
                        "<gray>Members: <white>" + c.members.size(),
                        c == clan ? "<#9cc0ff>Your clan" : " "));
            }
            inv.setItem(22, item(Material.ARROW, "<white>Back"));
        }

        @Override
        void click(Player p, int slot, ClickType type) {
            if (slot == 22) {
                gui.openMain(p);
            }
        }
    }

    // ---------- Anvil ----------

    /** Preview item in the anvil result slot. */
    ItemStack anvilResult(String text) {
        if (text == null || text.isBlank()) {
            return item(Material.PAPER, "<#9a9a9a>Type a name");
        }
        String err = ClanText.validate(text);
        if (err != null) {
            return item(Material.PAPER, "<#ff7b7b>" + err);
        }
        ItemStack it = new ItemStack(Material.PAPER);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(ClanText.component(text));
        meta.lore(List.of(Msg.c("<#9a9a9a>Click to confirm")));
        it.setItemMeta(meta);
        return it;
    }

    void confirmTag(Player p, InventoryView view) {
        Clan c = anvilSessions.get(p.getUniqueId());
        if (c == null || !(view instanceof AnvilView av)) {
            return;
        }
        if (!c.can(p.getUniqueId(), ClanPerm.NAMETAG)) {
            p.closeInventory();
            return;
        }
        String text = av.getRenameText();
        String err = ClanText.validate(text);
        if (err != null) {
            Msg.send(p, "<#ff7b7b>" + err);
            return;
        }
        c.setTag(text);
        plugin.manager().save();
        p.closeInventory();
        Msg.send(p, "Name tag updated.");
        Bukkit.getScheduler().runTask(plugin, () -> openMain(p));
    }

    ClansPlugin plugin() {
        return plugin;
    }
}
