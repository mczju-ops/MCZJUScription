package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.arena.ResolvedArenaLayout;
import com.github.mczju.mczjuscription.entity.MatchEntityProtection;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.inventory.Merchant;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 商店模式常驻村民：右键打开购卡面板。 */
public final class ShopVillagerService {

    private static final Map<InscriptionMatch, BukkitTask> LOOK_TASKS = new ConcurrentHashMap<>();

    private ShopVillagerService() {}

    public static void spawnShopVillagers(InscriptionMatch match) {
        if (match.deckMode() != DeckMode.SHOP) {
            return;
        }
        despawnAllShopVillagers(match);
        for (var human : match.humanParticipants()) {
            spawnForSide(match, human.side());
        }
        startLookTask(match);
    }

    public static void spawnForSide(InscriptionMatch match, MatchSide side) {
        Location at = resolveShopVillagerLocation(match, side);
        if (at == null || at.getWorld() == null) {
            if (side == match.actingSide()) {
                match.feedback().actionBarWarn("<yellow>未配置商店村民位置");
            }
            return;
        }

        Villager villager =
                at.getWorld()
                        .spawn(at, Villager.class, SpawnReason.CUSTOM, entity -> configureShopVillager(entity, side));
        configureShopVillager(villager, side);
        villager.setCustomNameVisible(true);
        villager.customName(Component.text("商店"));
        match.setShopVillagerEntityId(side, villager.getUniqueId());
        Player target = resolveLookTarget(match, side);
        if (target != null) {
            ArenaFacing.faceToward(villager, target.getEyeLocation());
        }
    }

    public static void despawnAllShopVillagers(InscriptionMatch match) {
        stopLookTask(match);
        for (MatchSide side : MatchSide.values()) {
            despawnSide(match, side);
        }
    }

    private static void despawnSide(InscriptionMatch match, MatchSide side) {
        UUID id = match.shopVillagerEntityId(side);
        if (id == null) {
            return;
        }
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
        match.setShopVillagerEntityId(side, null);
    }

    public static boolean isShopVillager(Entity entity, InscriptionMatch match) {
        if (entity == null || match.deckMode() != DeckMode.SHOP) {
            return false;
        }
        for (MatchSide side : MatchSide.values()) {
            UUID id = match.shopVillagerEntityId(side);
            if (id != null && entity.getUniqueId().equals(id)) {
                return true;
            }
        }
        return entity.getPersistentDataContainer().has(InscriptionKeys.SHOP_VILLAGER, PersistentDataType.BYTE);
    }

    public static MatchSide shopVillagerSide(Entity entity, InscriptionMatch match) {
        if (entity == null) {
            return null;
        }
        for (MatchSide side : MatchSide.values()) {
            UUID id = match.shopVillagerEntityId(side);
            if (id != null && entity.getUniqueId().equals(id)) {
                return side;
            }
        }
        String raw = entity.getPersistentDataContainer().get(
                InscriptionKeys.SHOP_VILLAGER_SIDE, PersistentDataType.STRING);
        if (raw == null) {
            return null;
        }
        try {
            return MatchSide.valueOf(raw);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static void tryOpenShop(Player player, InscriptionMatch match) {
        MatchSide side = match.sideFor(player);
        if (side == null) {
            return;
        }
        if (!match.turn().canDrawFromMainDeck()) {
            match.feedback().actionBarWarn("<yellow>当前无法打开商店");
            return;
        }
        match.drawFromMainDeck(side);
    }

    private static void configureShopVillager(Villager villager, MatchSide side) {
        MatchEntityProtection.apply(villager);
        if (villager instanceof Merchant merchant) {
            merchant.setRecipes(Collections.emptyList());
        }
        villager.setVillagerType(Villager.Type.PLAINS);
        villager.setProfession(Villager.Profession.NONE);
        villager.getPersistentDataContainer().set(InscriptionKeys.SHOP_VILLAGER, PersistentDataType.BYTE, (byte) 1);
        villager.getPersistentDataContainer().set(
                InscriptionKeys.SHOP_VILLAGER_SIDE, PersistentDataType.STRING, side.name());
    }

    private static Location resolveShopVillagerLocation(InscriptionMatch match, MatchSide side) {
        BattleArena arena = match.arena();
        if (arena != null && arena.layout() != null) {
            ResolvedArenaLayout.StagingSites staging = arena.layout().staging(side);
            if (staging != null && staging.shopVillager != null) {
                return staging.shopVillager.clone();
            }
        }
        return null;
    }

    private static void startLookTask(InscriptionMatch match) {
        stopLookTask(match);
        BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                MCZJUScriptionPlugin.getInstance(), () -> tickLookAtPlayers(match), 0L, 2L);
        LOOK_TASKS.put(match, task);
    }

    private static void stopLookTask(InscriptionMatch match) {
        BukkitTask task = LOOK_TASKS.remove(match);
        if (task != null) {
            task.cancel();
        }
    }

    private static void tickLookAtPlayers(InscriptionMatch match) {
        if (match.deckMode() != DeckMode.SHOP) {
            stopLookTask(match);
            return;
        }
        for (MatchSide side : MatchSide.values()) {
            UUID id = match.shopVillagerEntityId(side);
            if (id == null) {
                continue;
            }
            Entity entity = findEntity(id);
            if (!(entity instanceof Villager villager) || !villager.isValid()) {
                continue;
            }
            Player target = resolveLookTarget(match, side);
            if (target == null || !target.isOnline()) {
                continue;
            }
            ArenaFacing.faceToward(villager, target.getEyeLocation());
        }
    }

    private static Player resolveLookTarget(InscriptionMatch match, MatchSide side) {
        return match.participant(side).player()
                .map(ext -> ext.player())
                .filter(Player::isOnline)
                .orElse(null);
    }

    private static Entity findEntity(UUID id) {
        for (var world : Bukkit.getWorlds()) {
            Entity entity = world.getEntity(id);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
