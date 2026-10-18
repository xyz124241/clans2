package de.neptun.clans;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class ClanCommand implements CommandExecutor, TabCompleter {
    private final ClansPlugin plugin;

    public ClanCommand(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur im Spiel.");
            return true;
        }
        ClanManager mgr = plugin.manager();
        Clan clan = mgr.get(p.getUniqueId());

        if (args.length == 0) {
            if (clan == null) {
                help(p);
            } else {
                plugin.gui().openMain(p);
            }
            return true;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "create" -> create(p, clan, args);
            case "invite" -> invite(p, clan, args);
            case "accept" -> accept(p, clan, args);
            case "leave" -> leave(p, clan);
            case "delete" -> delete(p, clan, args);
            default -> help(p);
        }
        return true;
    }

    private void help(Player p) {
        Msg.chat(p, "Commands:");
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan create <name> <dark_gray>- <gray>Create a clan (1-6 characters)"));
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan invite <player> <dark_gray>- <gray>Invite a player"));
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan accept <dark_gray>- <gray>Accept an invite"));
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan leave <dark_gray>- <gray>Leave your clan"));
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan delete <dark_gray>- <gray>Delete your clan (owner)"));
        p.sendMessage(Msg.MM.deserialize("<#bbbbbb> /clan <dark_gray>- <gray>Open the clan menu"));
    }

    private void create(Player p, Clan clan, String[] args) {
        if (clan != null) {
            Msg.send(p, "<#ff7b7b>You are already in a clan.");
            return;
        }
        if (args.length < 2) {
            Msg.send(p, "<#ff7b7b>Usage: /clan create <name>");
            return;
        }
        String name = args[1];
        if (!name.matches("[A-Za-z0-9]{1,6}")) {
            Msg.send(p, "<#ff7b7b>The name must be 1 to 6 letters or numbers.");
            return;
        }
        if (plugin.manager().byName(name) != null) {
            Msg.send(p, "<#ff7b7b>This clan already exists.");
            return;
        }
        plugin.manager().create(p, name);
        Msg.send(p, "Clan <bold>" + name + "</bold> created. Open the menu with <#bbbbbb>/clan<white>.");
    }

    private void invite(Player p, Clan clan, String[] args) {
        if (clan == null) {
            Msg.send(p, "<#ff7b7b>You are not in a clan.");
            return;
        }
        if (!clan.can(p.getUniqueId(), ClanPerm.INVITE)) {
            Msg.send(p, "<#ff7b7b>No permission.");
            return;
        }
        if (args.length < 2) {
            Msg.send(p, "<#ff7b7b>Usage: /clan invite <player>");
            return;
        }
        Player t = Bukkit.getPlayerExact(args[1]);
        if (t == null) {
            Msg.send(p, "<#ff7b7b>Player not found.");
            return;
        }
        if (t.equals(p) || plugin.manager().get(t.getUniqueId()) != null) {
            Msg.send(p, "<#ff7b7b>This player cannot be invited.");
            return;
        }
        plugin.manager().refreshLimit(clan);
        if (clan.members.size() >= clan.limit) {
            Msg.send(p, "<#ff7b7b>Your clan is full (" + clan.members.size() + "/" + clan.limit + ").");
            return;
        }
        plugin.manager().invite(t, clan);
        Msg.send(p, "Invite sent to <bold>" + t.getName() + "</bold>.");
        Component msg = Msg.MM.deserialize(Msg.PREFIX + "You were invited to clan <bold>" + clan.name
                + "</bold>. <#bbbbbb><underlined>[Accept]")
                .clickEvent(ClickEvent.runCommand("/clan accept " + clan.name));
        t.sendMessage(msg);
    }

    private void accept(Player p, Clan clan, String[] args) {
        if (clan != null) {
            Msg.send(p, "<#ff7b7b>You are already in a clan.");
            return;
        }
        List<String> pending = plugin.manager().pendingInvites(p.getUniqueId());
        if (pending.isEmpty()) {
            Msg.send(p, "<#ff7b7b>You have no open invite.");
            return;
        }
        String key = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : pending.get(pending.size() - 1);
        if (!pending.contains(key)) {
            Msg.send(p, "<#ff7b7b>No invite from this clan.");
            return;
        }
        Clan target = plugin.manager().byName(key);
        if (target == null) {
            Msg.send(p, "<#ff7b7b>This clan no longer exists.");
            return;
        }
        plugin.manager().refreshLimit(target);
        if (target.members.size() >= target.limit) {
            Msg.send(p, "<#ff7b7b>This clan is full.");
            return;
        }
        plugin.manager().addMember(target, p);
        plugin.manager().clearInvites(p.getUniqueId());
        Msg.send(p, "You joined clan <bold>" + target.name + "</bold>.");
        for (UUID u : target.members.keySet()) {
            Player m = Bukkit.getPlayer(u);
            if (m != null && !m.equals(p)) {
                Msg.send(m, "<bold>" + p.getName() + "</bold> joined the clan.");
            }
        }
    }

    private void leave(Player p, Clan clan) {
        if (clan == null) {
            Msg.send(p, "<#ff7b7b>You are not in a clan.");
            return;
        }
        if (clan.owner.equals(p.getUniqueId())) {
            Msg.send(p, "<#ff7b7b>Owners cannot leave. Use <#bbbbbb>/clan delete<#ff7b7b>.");
            return;
        }
        plugin.manager().removeMember(clan, p.getUniqueId());
        Msg.send(p, "You left the clan.");
        for (UUID u : clan.members.keySet()) {
            Player m = Bukkit.getPlayer(u);
            if (m != null) {
                Msg.send(m, "<bold>" + p.getName() + "</bold> left the clan.");
            }
        }
    }

    private void delete(Player p, Clan clan, String[] args) {
        if (clan == null) {
            Msg.send(p, "<#ff7b7b>You are not in a clan.");
            return;
        }
        if (!clan.owner.equals(p.getUniqueId())) {
            Msg.send(p, "<#ff7b7b>Only the owner can delete the clan.");
            return;
        }
        if (args.length < 2 || !args[1].equalsIgnoreCase("confirm")) {
            Msg.send(p, "Are you sure? Type <#bbbbbb>/clan delete confirm<white>.");
            return;
        }
        List<UUID> members = new ArrayList<>(clan.members.keySet());
        plugin.manager().delete(clan);
        for (UUID u : members) {
            Player m = Bukkit.getPlayer(u);
            if (m != null) {
                m.closeInventory();
                Msg.send(m, "Clan <bold>" + clan.name + "</bold> was deleted.");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!(sender instanceof Player p)) {
            return out;
        }
        if (args.length == 1) {
            for (String s : List.of("create", "invite", "accept", "leave", "delete")) {
                if (s.startsWith(args[0].toLowerCase(Locale.ROOT))) {
                    out.add(s);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("invite")) {
            for (Player o : Bukkit.getOnlinePlayers()) {
                if (o.getName().toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT))) {
                    out.add(o.getName());
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("accept")) {
            out.addAll(plugin.manager().pendingInvites(p.getUniqueId()));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            out.add("confirm");
        }
        return out;
    }
}
