package com.github.mczju.mczjuscription.command;

import com.github.mczju.mczjuscription.InscriptionBranding;
import com.github.mczju.mczjuscription.arena.ArenaBootstrap;
import com.github.mczju.mczjuscription.data.CardDesignerSession;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameCoreBridge;
import com.github.mczju.mczjuscription.game.InscriptionGameAccess;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.session.PlayVariant;
import com.github.mczju.mczjuscription.lobby.HubMatchLauncher;
import com.github.mczju.mczjuscription.menu.CardDesignerMenu;
import com.github.mczju.mczjuscription.menu.DeckBuilderMenu;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class InscriptionCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("仅玩家可执行。");
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage("用法: /isc <hub|start|arena|deck|carddesign|cardname>");
            return true;
        }
        if ("carddesign".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("inscription.admin")) {
                player.sendMessage("你没有卡牌设计权限。");
                return true;
            }
            new CardDesignerMenu(player, new Object[0]).open();
            return true;
        }
        if ("cardname".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("inscription.admin")) {
                player.sendMessage("你没有卡牌设计权限。");
                return true;
            }
            if (args.length < 2) {
                player.sendMessage("用法: /isc cardname <名称>");
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
            CardDesignerSession.of(player.getUniqueId()).setDisplayName(name);
            player.sendMessage("§a卡牌名称已设为: §f" + name);
            return true;
        }
        if ("hub".equalsIgnoreCase(args[0])) {
            InscriptionGameCoreBridge.joinHub(new PlayerExt(player));
            return true;
        }
        if ("start".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("inscription.play")) {
                player.sendMessage("你没有对局操作权限。");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("用法: /isc start <solo|duel> <shop|free>");
                return true;
            }
            PlayVariant variant = HubMatchLauncher.parseVariant(args[1], args[2]);
            if (variant == null) {
                player.sendMessage("无效模式。示例: /isc start solo shop");
                return true;
            }
            PlayerExt ext = new PlayerExt(player);
            if (variant.isSolo()) {
                HubMatchLauncher.startSolo(ext, variant);
            } else {
                if (!ext.isPartyLeader()) {
                    player.sendMessage("双人模式需要队长执行，且队伍为 2 人。");
                    return true;
                }
                if (ext.getParty() == null) {
                    player.sendMessage("请先组队（2 人）。");
                    return true;
                }
                HubMatchLauncher.startDuel(ext.getParty(), variant);
            }
            return true;
        }
        if ("deck".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("inscription.deck")) {
                player.sendMessage("你没有构牌编辑权限。");
                return true;
            }
            new DeckBuilderMenu(player, new Object[0]).open();
            return true;
        }
        if ("arena".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("inscription.play")) {
                player.sendMessage("你没有对局操作权限。");
                return true;
            }
            AbstractInscriptionGame game = InscriptionGameAccess.resolveGame(player);
            if (game == null) {
                player.sendMessage("请先开始" + InscriptionBranding.TITLE + "对局。");
                return true;
            }
            InscriptionMatch match = game.match();
            if (match == null) {
                player.sendMessage("对局尚未就绪。");
                return true;
            }
            InscriptionGameRoom room = match.matchRoom();
            if (room != null && room.hasConfiguredArena()) {
                ArenaBootstrap.bindForMatch(match, room, match.humanParticipants(), game.sender());
            } else {
                ArenaBootstrap.bindAtPlayer(match, player, game.sender());
            }
            return true;
        }
        sender.sendMessage("未知子命令。可用: hub, start, arena, deck, carddesign, cardname");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
            @NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            List<String> options = List.of("hub", "start", "arena", "deck", "carddesign", "cardname");
            String prefix = args[0].toLowerCase(Locale.ROOT);
            return options.stream().filter(s -> s.startsWith(prefix)).toList();
        }
        if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            return filter(List.of("solo", "duel"), args[1]);
        }
        if (args.length == 3 && "start".equalsIgnoreCase(args[0])) {
            return filter(List.of("shop", "free"), args[2]);
        }
        return List.of();
    }

    private static List<String> filter(List<String> options, String prefix) {
        String p = prefix.toLowerCase(Locale.ROOT);
        return options.stream().filter(s -> s.startsWith(p)).toList();
    }
}
