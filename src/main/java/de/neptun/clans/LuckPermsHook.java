package de.neptun.clans;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Liest die echten LuckPerms-Gruppen eines Spielers (OP zaehlt nicht als alle Gruppen). */
final class LuckPermsHook {
    private LuckPermsHook() {
    }

    static Set<String> groups(Player p) {
        Set<String> out = new HashSet<>();
        LuckPerms lp = LuckPermsProvider.get();
        User user = lp.getPlayerAdapter(Player.class).getUser(p);
        for (Group g : user.getInheritedGroups(QueryOptions.nonContextual())) {
            out.add(g.getName().toLowerCase(Locale.ROOT));
        }
        return out;
    }
}
