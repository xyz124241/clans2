package de.neptun.clans;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ClansPlugin extends JavaPlugin {
    private ClanManager manager;
    private Gui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        manager = new ClanManager(this);
        manager.load();
        gui = new Gui(this);

        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClanListener(this), this);

        PluginCommand cmd = getCommand("clan");
        if (cmd != null) {
            ClanCommand handler = new ClanCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ClanExpansion(this).register();
            getLogger().info("PlaceholderAPI gefunden: %clan_tag% ist aktiv.");
        } else {
            getLogger().warning("PlaceholderAPI fehlt - die Anzeige in TAB funktioniert nicht.");
        }

        // alle 5 Minuten speichern
        Bukkit.getScheduler().runTaskTimer(this, () -> manager.save(), 20L * 300, 20L * 300);
    }

    @Override
    public void onDisable() {
        if (gui != null) {
            gui.closeAll();
        }
        if (manager != null) {
            manager.save();
        }
    }

    public ClanManager manager() {
        return manager;
    }

    public Gui gui() {
        return gui;
    }
}
