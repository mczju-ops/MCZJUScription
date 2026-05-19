package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.lobby.menu.BlessingPlaceholderMenu;
import com.github.mczju.mczjuscription.lobby.menu.CursePlaceholderMenu;
import com.github.mczju.mczjuscription.lobby.menu.DuelModePickMenu;
import com.github.mczju.mczjuscription.lobby.menu.LeaderboardMenu;
import com.github.mczju.mczjuscription.lobby.menu.SoloModePickMenu;
import com.github.mczju.mczjuscription.menu.DeckBuilderMenu;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;

/** 大厅区域交互（仅 {@link InscriptionHubGame} 内生效）。 */
public final class HubZoneListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
            return;
        }
        PlayerExt ext = new PlayerExt(event.getPlayer());
        if (!ext.isInGame(InscriptionHubGame.class)) {
            return;
        }
        if (!(ext.getGame() instanceof InscriptionHubGame hub)) {
            return;
        }
        InscriptionHubRoom room = hub.hubRoom();
        if (room == null) {
            return;
        }

        Location click =
                event.getClickedBlock() != null
                        ? event.getClickedBlock().getLocation()
                        : event.getPlayer().getLocation();

        event.setCancelled(true);

        if (isDeckLectern(room, event.getClickedBlock(), click)) {
            new DeckBuilderMenu(event.getPlayer(), new Object[0]).open();
            return;
        }
        if (HubLocationUtil.near(room.blessingAt, click)) {
            new BlessingPlaceholderMenu(event.getPlayer()).open();
            return;
        }
        if (HubLocationUtil.near(room.curseSelectAt, click)) {
            new CursePlaceholderMenu(event.getPlayer()).open();
            return;
        }
        if (HubLocationUtil.near(room.leaderboardAt, click)) {
            new LeaderboardMenu(event.getPlayer()).open();
            return;
        }

        int soloIndex = seatIndex(HubLocationUtil.soloSeats(room), click);
        if (soloIndex >= 0) {
            if (HubSeatService.occupySolo(ext, room, soloIndex)) {
                new SoloModePickMenu(event.getPlayer()).open();
            }
            return;
        }

        int duelIndex = seatIndex(HubLocationUtil.duelSeats(room), click);
        if (duelIndex >= 0) {
            if (HubSeatService.occupyDuel(ext, room, duelIndex)) {
                new DuelModePickMenu(event.getPlayer()).open();
            }
        }
    }

    @EventHandler
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking()) {
            return;
        }
        PlayerExt ext = new PlayerExt(event.getPlayer());
        if (!ext.isInGame(InscriptionHubGame.class)) {
            return;
        }
        if (HubSession.seatOf(ext.getUniqueId()) == null) {
            return;
        }
        HubSeatService.dismount(event.getPlayer());
        HubSeatService.release(ext.getUniqueId());
        ext.sender().info("<gray>已离开座位。");
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        HubSeatService.release(event.getPlayer().getUniqueId());
    }

    private static boolean isDeckLectern(InscriptionHubRoom room, Block block, Location click) {
        if (room.deckLecternAt == null) {
            return false;
        }
        if (block != null && block.getType() == Material.LECTERN) {
            if (HubLocationUtil.sameBlock(room.deckLecternAt, block.getLocation())) {
                return true;
            }
        }
        return HubLocationUtil.near(room.deckLecternAt, click);
    }

    private static int seatIndex(List<Location> seats, Location click) {
        for (int i = 0; i < seats.size(); i++) {
            if (HubLocationUtil.near(seats.get(i), click)) {
                return i;
            }
        }
        return -1;
    }
}
