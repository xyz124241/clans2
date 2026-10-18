package de.neptun.clans;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.projectiles.ProjectileSource;

public final class ClanListener implements Listener {
    private final ClansPlugin plugin;

    public ClanListener(ClansPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        Clan c = plugin.manager().get(p.getUniqueId());
        if (c == null) {
            return;
        }
        c.names.put(p.getUniqueId(), p.getName());
        if (c.owner.equals(p.getUniqueId())) {
            c.limit = plugin.manager().computeLimit(p);
        }
    }

    /** Setzt den Clan-Tag vor jede Chat-Nachricht (abschaltbar mit chat-tag: false in der config.yml). */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent e) {
        if (!plugin.getConfig().getBoolean("chat-tag", true)) {
            return;
        }
        Clan c = plugin.manager().get(e.getPlayer().getUniqueId());
        if (c == null) {
            return;
        }
        ChatRenderer prev = e.renderer();
        Component tag = ClanText.component(c.tag);
        e.renderer((source, name, msg, viewer) -> Component.empty()
                .append(tag)
                .append(Component.space())
                .append(prev.render(source, name, msg, viewer)));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player victim = e.getEntity();
        plugin.manager().addDeath(victim.getUniqueId());
        Player killer = victim.getKiller();
        if (killer != null && !killer.equals(victim)) {
            plugin.manager().addKill(killer.getUniqueId());
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        Player attacker = null;
        Entity d = e.getDamager();
        if (d instanceof Player pd) {
            attacker = pd;
        } else if (d instanceof Projectile proj) {
            ProjectileSource src = proj.getShooter();
            if (src instanceof Player ps) {
                attacker = ps;
            }
        }
        if (attacker == null || attacker.equals(victim)) {
            return;
        }
        Clan a = plugin.manager().get(attacker.getUniqueId());
        if (a != null && a == plugin.manager().get(victim.getUniqueId()) && !a.friendlyFire) {
            e.setCancelled(true);
        }
    }
}
