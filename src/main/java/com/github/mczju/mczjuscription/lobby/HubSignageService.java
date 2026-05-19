package com.github.mczju.mczjuscription.lobby;

import com.github.mczjuops.mczjugamecore.utils.TextParser;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;

/** 按房间配置生成路标 TextDisplay。 */
public final class HubSignageService {

    private static final Map<String, List<UUID>> SIGNS = new ConcurrentHashMap<>();

    private HubSignageService() {}

    public static void ensureSpawned(InscriptionHubRoom room) {
        if (room == null) {
            return;
        }
        String key = roomKey(room);
        List<UUID> existing = SIGNS.get(key);
        if (existing != null && !existing.isEmpty() && allAlive(existing)) {
            return;
        }
        despawn(room);
        List<UUID> spawned = new ArrayList<>();
        for (HubLocationUtil.HubSign sign : HubLocationUtil.signs(room)) {
            TextDisplay display = spawnSign(sign.at(), sign.text());
            if (display != null) {
                spawned.add(display.getUniqueId());
            }
        }
        if (!spawned.isEmpty()) {
            SIGNS.put(key, spawned);
        }
    }

    public static void despawn(InscriptionHubRoom room) {
        if (room == null) {
            return;
        }
        String key = roomKey(room);
        List<UUID> ids = SIGNS.remove(key);
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Entity entity = findEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private static TextDisplay spawnSign(Location at, String miniMessage) {
        if (at.getWorld() == null) {
            return null;
        }
        Component text = TextParser.parse(miniMessage.replace("\\n", "\n"));
        return at.getWorld()
                .spawn(
                        at,
                        TextDisplay.class,
                        display -> {
                            display.text(text);
                            display.setBillboard(Display.Billboard.CENTER);
                            display.setSeeThrough(true);
                            display.setDefaultBackground(false);
                            display.setShadowed(true);
                            display.setInvulnerable(true);
                            display.setPersistent(false);
                            display.setLineWidth(200);
                        });
    }

    private static boolean allAlive(List<UUID> ids) {
        for (UUID id : ids) {
            if (findEntity(id) == null) {
                return false;
            }
        }
        return true;
    }

    private static Entity findEntity(UUID id) {
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }

    private static String roomKey(InscriptionHubRoom room) {
        return room.getGameId() + "/" + room.getRoomName();
    }
}
