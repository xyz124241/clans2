package de.neptun.clans;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.view.AnvilView;

public final class GuiListener implements Listener {
    private final ClansPlugin plugin;

    public GuiListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean inAnvil(Player p) {
        return plugin.gui().anvilSessions.containsKey(p.getUniqueId());
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if (inAnvil(p) && e.getView() instanceof AnvilView) {
            e.setCancelled(true);
            if (e.getRawSlot() == 2) {
                plugin.gui().confirmTag(p, e.getView());
            }
            return;
        }
        Inventory top = e.getView().getTopInventory();
        InventoryHolder h = top.getHolder();
        if (h instanceof Gui.ClanMenu menu) {
            e.setCancelled(true);
            if (e.getClickedInventory() == top) {
                menu.click(p, e.getSlot(), e.getClick());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) {
            return;
        }
        if ((inAnvil(p) && e.getView() instanceof AnvilView)
                || e.getView().getTopInventory().getHolder() instanceof Gui.ClanMenu) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPrepare(PrepareAnvilEvent e) {
        if (!(e.getView().getPlayer() instanceof Player p) || !inAnvil(p)) {
            return;
        }
        AnvilView v = e.getView();
        e.setResult(plugin.gui().anvilResult(v.getRenameText()));
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player p)) {
            return;
        }
        if (e.getView() instanceof AnvilView) {
            // Eingabe-Items entfernen, damit nichts im Inventar landet
            e.getView().getTopInventory().clear();
            plugin.gui().anvilSessions.remove(p.getUniqueId());
            return;
        }
        if (e.getView().getTopInventory().getHolder() instanceof Gui.EnderHolder) {
            plugin.manager().save();
        }
    }
}
