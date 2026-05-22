package com.github.mczju.mczjuscription;

import com.github.mczju.mczjuscription.bootstrap.InscriptionShutdownService;
import com.github.mczju.mczjuscription.command.InscriptionCommand;
import com.github.mczju.mczjuscription.data.InscriptionPlayerData;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.InscriptionGames;
import com.github.mczju.mczjuscription.game.InscriptionGameCoreBridge;
import com.github.mczju.mczjuscription.game.InscriptionRoomSetup;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.listener.CardDesignerMenuListener;
import com.github.mczju.mczjuscription.listener.DeckBuilderMenuListener;
import com.github.mczju.mczjuscription.listener.ArenaSlotInteractListener;
import com.github.mczju.mczjuscription.listener.CardDropPlacementListener;
import com.github.mczju.mczjuscription.listener.ShopAdminListener;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import com.github.mczju.mczjuscription.listener.LeaveConfirmListener;
import com.github.mczju.mczjuscription.lobby.HubZoneListener;
import com.github.mczju.mczjuscription.lobby.InscriptionLeaderboards;
import com.github.mczju.mczjuscription.listener.MatchEntityProtectionListener;
import com.github.mczju.mczjuscription.listener.MatchListener;
import com.github.mczju.mczjuscription.menu.DeckBuilderMenu;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.menu.MenuFacade;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class MCZJUScriptionPlugin extends JavaPlugin {

    private static MCZJUScriptionPlugin instance;

    @Override
    public void onEnable() {
        instance = this;
        InscriptionKeys.init(this);
        CardCatalog.init(this);
        ShopConfigStorage.init(this);
        InscriptionItems.registerAll();
        MenuFacade.registerMenu("inscription_deck", DeckBuilderMenu.class);
        getLogger().info("构牌菜单已注册 (inscription_deck)，版本 " + getDescription().getVersion());
        InscriptionLeaderboards.registerAll();
        InscriptionGameCoreBridge.init(this);
        InscriptionGames.registerAll();
        InscriptionRoomSetup.validateAndLog(this);
        MCZJUGameCore.getPlayerDataManager().registerPlayerData(AbstractInscriptionGame.DATA_ID, InscriptionPlayerData.class);
        getServer().getPluginManager().registerEvents(new MatchListener(), this);
        getServer().getPluginManager().registerEvents(new ArenaSlotInteractListener(), this);
        getServer().getPluginManager().registerEvents(new MatchEntityProtectionListener(), this);
        getServer().getPluginManager().registerEvents(new CardDropPlacementListener(), this);
        getServer().getPluginManager().registerEvents(new LeaveConfirmListener(), this);
        getServer().getPluginManager().registerEvents(new HubZoneListener(), this);
        getServer().getPluginManager().registerEvents(new DeckBuilderMenuListener(), this);
        getServer().getPluginManager().registerEvents(new CardDesignerMenuListener(), this);
        getServer().getPluginManager().registerEvents(new ShopAdminListener(), this);

        PluginCommand isc = getCommand("isc");
        if (isc != null) {
            InscriptionCommand executor = new InscriptionCommand();
            isc.setExecutor(executor);
            isc.setTabCompleter(executor);
        }
    }

    @Override
    public void onDisable() {
        if (MCZJUGameCore.getInstance() != null) {
            InscriptionShutdownService.shutdown(getLogger());
        }
        instance = null;
    }

    public static MCZJUScriptionPlugin getInstance() {
        return instance;
    }
}
