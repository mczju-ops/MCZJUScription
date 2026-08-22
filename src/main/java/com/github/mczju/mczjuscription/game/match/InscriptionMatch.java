package com.github.mczju.mczjuscription.game.match;

import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.arena.ArenaManager;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGame;
import com.github.mczju.mczjuscription.game.InscriptionGameRoom;
import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardDefinition;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.CostType;
import com.github.mczju.mczjuscription.game.session.DeckMode;
import com.github.mczju.mczjuscription.game.session.MatchMode;
import com.github.mczju.mczjuscription.game.session.MatchSetup;
import com.github.mczju.mczjuscription.game.session.OpponentController;
import com.github.mczju.mczjuscription.game.session.ParticipantState;
import com.github.mczju.mczjuscription.game.session.TurnOwnership;
import com.github.mczju.mczjuscription.game.sigil.BreedingTracker;
import com.github.mczju.mczjuscription.game.sigil.SigilContext;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilRegistry;
import com.github.mczju.mczjuscription.game.sigil.SigilTrigger;
import com.github.mczju.mczjuscription.game.deck.OpeningHandDealer;
import com.github.mczju.mczjuscription.shop.ParticipantShopState;
import com.github.mczju.mczjuscription.shop.ShopConfig;
import com.github.mczju.mczjuscription.shop.ShopConfigStorage;
import java.util.EnumMap;
import com.github.mczju.mczjuscription.game.combat.BeamCombatSession;
import com.github.mczju.mczjuscription.game.combat.BeamTargeting;
import com.github.mczju.mczjuscription.game.combat.CombatModifiers;
import com.github.mczju.mczjuscription.game.turn.TurnController;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.ui.MatchFeedback;
import com.github.mczju.mczjuscription.ui.MatchHotbar;
import com.github.mczju.mczjuscription.ui.ScalesBossBar;
import com.github.mczju.mczjuscription.util.InscriptionKeys;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import com.github.mczjuops.mczjugamecore.MCZJUGameCore;
import com.github.mczjuops.mczjugamecore.player.PlayerExt;
import com.github.mczjuops.mczjugamecore.utils.sender.Sender;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InscriptionMatch {

    private final AbstractInscriptionGame game;
    private final MatchSetup setup;
    private final Map<MatchSide, ParticipantState> participants;
    private final OpponentController opponentController;
    private final TurnOwnership turnOwnership;
    private final Sender sender;

    private final LifeSystem life = new LifeSystem();
    private final Scales scales = new Scales();
    private final ScalesBossBar scalesBossBar = new ScalesBossBar(this);
    private final BattleBoard board = new BattleBoard();
    private final TurnController turn;
    private final MatchFeedback feedback;
    private final BreedingTracker breedingTracker = new BreedingTracker();

    private BattleArena arena;
    private MatchSide activeCombatSide = MatchSide.PLAYER;
    private boolean matchOver;
    private MatchSide matchWinner;
    private boolean combatAnimating;
    /** 天平在本轮战斗中已决出小局（±5）时置位，用于截断连击等多段攻击。 */
    private boolean roundResolvedDuringCombat;
    private BeamCombatSession beamSession;
    private UUID wanderingTraderEntityId;
    private final EnumMap<MatchSide, UUID> shopVillagerEntityIds = new EnumMap<>(MatchSide.class);
    private final EnumMap<MatchSide, ParticipantShopState> shopStates = new EnumMap<>(MatchSide.class);

    public InscriptionMatch(AbstractInscriptionGame game, MatchSetup setup) {
        this.game = game;
        this.setup = setup;
        this.participants = setup.createParticipantStates();
        this.opponentController = setup.opponentController();
        this.turnOwnership = new TurnOwnership(setup.variant().matchMode());
        this.sender = game.sender();
        this.feedback = new MatchFeedback(this);
        this.turn = new TurnController(this);
        initializeDecks();
    }

    public MatchFeedback feedback() {
        return feedback;
    }

    public BreedingTracker breedingTracker() {
        return breedingTracker;
    }

    /** 商店购卡：入手牌并同步，不结束抽牌阶段。 */
    public void completeShopPurchase(MatchSide side) {
        if (side != actingSide()) return;
        syncHud();
    }

    public ParticipantShopState shopState(MatchSide side) {
        return shopStates.computeIfAbsent(side, s -> new ParticipantShopState());
    }

    public ShopConfig shopConfig() {
        return ShopConfigStorage.get();
    }

    public void refreshShopOffers() {
        if (setup.deckMode() != DeckMode.SHOP) {
            return;
        }
        ShopConfig config = shopConfig();
        for (ParticipantState human : humanParticipants()) {
            shopState(human.side()).refreshFromConfig(config);
        }
    }

    /** 花费腐肉解锁第 4 个刷新格（本局永久，仅解锁方生效）。 */
    public boolean tryUnlockExtraRotatingSlot(MatchSide side) {
        if (setup.deckMode() != DeckMode.SHOP) {
            return false;
        }
        ParticipantShopState shop = shopState(side);
        if (shop.isExtraRotatingSlotUnlocked()) {
            feedback.actionBarWarn("<yellow>额外刷新格已解锁。");
            return false;
        }
        int cost = shopConfig().extraSlotUnlockBlood();
        if (!currency(side).trySpendBlood(cost)) {
            feedback.actionBarWarn("<red>腐肉不足 ×%d".formatted(cost));
            return false;
        }
        shop.unlockExtraRotatingSlot();
        feedback.actionBarInfo("<green>已解锁第 4 个刷新格（本局有效）");
        syncHud();
        return true;
    }

    public void start() {
        OpeningHandDealer.deal(this);
        refreshShopOffers();
        if (setup.deckMode() == DeckMode.SHOP && !usesUnifiedShopSlot()) {
            com.github.mczju.mczjuscription.shop.ShopVillagerService.spawnShopVillagers(this);
        }
        turn.enterPhase(
                setup.deckMode() == DeckMode.SHOP
                        ? com.github.mczju.mczjuscription.game.turn.TurnPhase.PLAY
                        : com.github.mczju.mczjuscription.game.turn.TurnPhase.DRAW);
        if (opponentController != null) {
            opponentController.onMatchStart(this);
        }
        planOpponentPreview();
        syncHud();
        if (arena == null) {
            feedback.actionBarWarn("<yellow>场地未就绪");
        }
        scalesBossBar.start();
    }

    private void initializeDecks() {
        for (ParticipantState state : participants.values()) {
            if (state.deckSource() != null) {
                state.deckSource().initializeDeck(state, this);
            }
            if (setup.deckMode() == DeckMode.SHOP && state.isHuman()) {
                state.currency().addBones(3);
            }
        }
    }

    public com.github.mczjuops.mczjugamecore.utils.sender.Sender sender() {
        return sender;
    }

    public MatchMode mode() {
        return setup.variant().matchMode();
    }

    public com.github.mczju.mczjuscription.game.session.DeckMode deckMode() {
        return setup.deckMode();
    }

    public MatchSetup setup() {
        return setup;
    }

    public void bindArena(BattleArena arena) {
        this.arena = arena;
    }

    public BattleArena arena() {
        return arena;
    }

    public AbstractInscriptionGame game() {
        return game;
    }

    /** 对局场地配置（优先插件分配的 play 房，而非 MGC 绑定的 main）。 */
    public InscriptionGameRoom matchRoom() {
        if (game instanceof InscriptionGame inscription) {
            InscriptionGameRoom dedicated = inscription.matchRoom();
            if (dedicated != null) {
                return dedicated;
            }
        }
        if (game.getGameRoom() instanceof InscriptionGameRoom room) {
            return room;
        }
        return null;
    }

    public ParticipantState participant(MatchSide side) {
        return participants.get(side);
    }

    public Currency currency(MatchSide side) {
        return participant(side).currency();
    }

    public List<ParticipantState> humanParticipants() {
        List<ParticipantState> humans = new ArrayList<>();
        for (ParticipantState state : participants.values()) {
            if (state.isHuman()) humans.add(state);
        }
        return humans;
    }

    /** @deprecated 优先使用 {@link #sideFor(Player)} 与 {@link #participant(MatchSide)} */
    public PlayerExt primaryHuman() {
        return humanParticipants().stream()
                .findFirst()
                .flatMap(ParticipantState::player)
                .orElseThrow();
    }

    public MatchSide sideFor(Player player) {
        for (ParticipantState state : participants.values()) {
            if (state.player().map(p -> p.player().getUniqueId().equals(player.getUniqueId())).orElse(false)) {
                return state.side();
            }
        }
        return null;
    }

    public MatchSide actingSide() {
        return turnOwnership.actingSideForInput();
    }

    public TurnOwnership turnOwnership() {
        return turnOwnership;
    }

    public LifeSystem life() {
        return life;
    }

    public Scales scales() {
        return scales;
    }

    public BattleBoard board() {
        return board;
    }

    public TurnController turn() {
        return turn;
    }

    public MatchSide activeCombatSide() {
        return activeCombatSide;
    }

    public void setActiveCombatSide(MatchSide side) {
        this.activeCombatSide = side;
    }

    public boolean isMatchOver() {
        return matchOver;
    }

    public boolean roundResolvedDuringCombat() {
        return roundResolvedDuringCombat;
    }

    /** 整局结束，或本敲钟战斗内小局已分出胜负。 */
    public boolean shouldStopCombatSequence() {
        return matchOver || roundResolvedDuringCombat;
    }

    public void clearRoundResolvedDuringCombat() {
        roundResolvedDuringCombat = false;
    }

    public boolean isCombatAnimating() {
        return combatAnimating;
    }

    public void setCombatAnimating(boolean combatAnimating) {
        this.combatAnimating = combatAnimating;
    }

    public BeamCombatSession beamSession() {
        return beamSession;
    }

    public void setBeamSession(BeamCombatSession beamSession) {
        this.beamSession = beamSession;
    }

    public UUID wanderingTraderEntityId() {
        return wanderingTraderEntityId;
    }

    public void setWanderingTraderEntityId(UUID wanderingTraderEntityId) {
        this.wanderingTraderEntityId = wanderingTraderEntityId;
    }

    public UUID shopVillagerEntityId(MatchSide side) {
        return shopVillagerEntityIds.get(side);
    }

    public void setShopVillagerEntityId(MatchSide side, UUID entityId) {
        if (entityId == null) {
            shopVillagerEntityIds.remove(side);
        } else {
            shopVillagerEntityIds.put(side, entityId);
        }
    }

    public MatchSide matchWinner() {
        return matchWinner;
    }

    public void planOpponentPreview() {
        if (opponentController != null) {
            opponentController.planPreview(this);
        }
        if (arena != null) {
            arena.refreshPreviewStrip(this);
        }
    }

    /** 顶栏 2×11：下一只将进入后场的造物模板 id。 */
    public List<String> enemyStripPreviewTemplates() {
        if (opponentController == null) {
            return List.of();
        }
        return opponentController.stripPreviewTemplates(this);
    }

    /** 后场→站场每推进一步：roll 并入队，顶栏→后场，队首→顶栏。 */
    public void notifyEnemyBackfieldWaveAdvanced() {
        if (opponentController != null) {
            opponentController.onBackfieldWaveAdvanced(this);
        }
        if (arena != null) {
            arena.refreshPreviewStrip(this);
        }
    }

    public void drawFromMainDeck(MatchSide side) {
        if (side != actingSide()) {
            feedback.actionBarWarn("<yellow>不是你的回合");
            return;
        }
        if (!turn.canDrawFromMainDeck()) {
            return;
        }
        var source = participant(side).deckSource();
        if (source == null) {
            return;
        }
        source.performMainDrawAction(this, side);
    }

    /** 主牌组抽取或商店购牌成功后调用，结束抽牌子阶段。 */
    public void completeMainDraw(MatchSide side) {
        if (side != actingSide()) return;
        turn.onDrawFromMainDeck();
        syncHud();
    }

    public boolean drawFromRabbitPile(MatchSide side) {
        if (setup.deckMode() == DeckMode.SHOP) {
            feedback.actionBarWarn("<yellow>商店模式请在商店槽位购卡");
            return false;
        }
        if (side != actingSide()) {
            feedback.actionBarWarn("<yellow>不是你的回合");
            return false;
        }
        if (turn.hasDrawnFromRabbitPileThisTurn()) {
            feedback.actionBarWarn("<yellow>本回合已领过免费兔子");
            return false;
        }
        if (!turn.canDrawFromRabbitPile()) {
            feedback.actionBarWarn("<yellow>当前无法领取兔子");
            return false;
        }
        grantCardToHand(side, "mob_rabbit");
        turn.onDrawFromRabbitPile();
        return true;
    }

    private boolean usesUnifiedShopSlot() {
        if (arena == null || arena.layout() == null) {
            return false;
        }
        var staging = arena.layout().staging(MatchSide.PLAYER);
        return staging != null && staging.shopSlot != null;
    }

    public void grantCardToHand(MatchSide side, String templateId) {
        grantCardToHandSilent(side, templateId);
        syncHud();
    }

    public void grantCardToHandSilent(MatchSide side, String templateId) {
        CardCatalog.require(templateId);
        ParticipantState state = participant(side);
        state.hand().add(templateId);
        state.player().ifPresent(ext ->
                InscriptionItems.giveCardToHand(ext.player(), templateId, deckMode())
        );
        com.github.mczju.mczjuscription.game.sigil.SurpriseEntry.tryAutoSummon(this, side, templateId);
    }

    public boolean playCardToSlot(String templateId, int slotIndex, MatchSide actingSide) {
        return playCardToSlot(templateId, slotIndex, actingSide, null);
    }

    /**
     * @param droppedItem 丢牌召唤时场上的掉落物；已从背包脱出，成功时移除该实体而非再扫背包
     */
    public boolean playCardToSlot(
            String templateId, int slotIndex, MatchSide actingSide, org.bukkit.entity.Item droppedItem) {
        if (actingSide != actingSide()) {
            feedback.actionBarWarn("<yellow>不是你的回合");
            return false;
        }
        if (turn.phase() != com.github.mczju.mczjuscription.game.turn.TurnPhase.PLAY) {
            return false;
        }
        if (arena == null) {
            feedback.actionBarWarn("<yellow>场地未就绪");
            return false;
        }
        if ("mob_fish_dried".equals(templateId)) {
            feedback.actionBarWarn("<yellow>鱼干是代币，不能当作卡牌上场");
            return false;
        }

        SlotOwner slotOwner = BoardSides.toSlotOwner(actingSide);
        BoardSlot[] row = board.row(slotOwner);
        if (!BoardRules.canPlaceAt(row, slotIndex)) {
            return false;
        }
        BoardSlot slot = board.slot(slotOwner, slotIndex);

        ParticipantState state = participant(actingSide);
        if (!state.hand().contains(templateId)) {
            return false;
        }

        java.util.UUID designerId =
                state.player().map(p -> p.player().getUniqueId()).orElse(null);
        CardTemplate def = CardCatalog.resolve(templateId, designerId);
        if (def == null) {
            feedback.actionBarWarn("<red>卡牌模板不存在，请在设计器重新获取");
            return false;
        }
        if (!payCost(actingSide, def.toDefinition())) return false;

        state.hand().remove(templateId);
        if (droppedItem != null && droppedItem.isValid()) {
            droppedItem.remove();
        } else {
            state.player().ifPresent(ext -> removeCardItemFromInventory(ext.player(), templateId));
        }

        BoardCreature creature = new BoardCreature(templateId, actingSide);
        creature.bind(slot);
        spawnCreatureEntity(creature, slotOwner, slotIndex);
        SigilRegistry.fire(SigilTrigger.ON_PLAY, new SigilContext(this, SigilTrigger.ON_PLAY, creature, null, 0));
        syncHud();
        return true;
    }

    /** 食尸鬼等：战斗中断免费落子，不检查回合阶段与费用。 */
    public boolean forcePlayFromHand(MatchSide side, String templateId, int slotIndex) {
        if (arena == null) return false;
        SlotOwner slotOwner = BoardSides.toSlotOwner(side);
        BoardSlot[] row = board.row(slotOwner);
        if (!BoardRules.canPlaceAt(row, slotIndex)) return false;

        ParticipantState state = participant(side);
        if (!state.hand().contains(templateId)) return false;

        state.hand().remove(templateId);
        state.player().ifPresent(ext -> removeCardItemFromInventory(ext.player(), templateId));

        BoardCreature creature = new BoardCreature(templateId, side);
        creature.bind(board.slot(slotOwner, slotIndex));
        spawnCreatureEntity(creature, slotOwner, slotIndex);
        SigilRegistry.fire(SigilTrigger.ON_PLAY, new SigilContext(this, SigilTrigger.ON_PLAY, creature, null, 0));
        syncHud();
        return true;
    }

    public void spawnCreatureEntity(BoardCreature creature, SlotOwner owner, int index) {
        if (arena == null) return;
        Location loc = arena.slotLocation(owner, index);
        if (loc == null) return;
        Location spawnAt = ArenaFacing.withYawToward(loc, ArenaFacing.facingTarget(arena, owner, index));
        BoardVfx.playSpawn(spawnAt);
        CreatureEntityService.spawn(creature, spawnAt);
        refreshCreatureLabels();
    }

    public void refreshCreatureLabels() {
        board.refreshAllLabels();
    }

    /** 敌方预览区造物前移落场，并播放移动粒子。 */
    public void advanceEnemyPreviewWithEffects() {
        if (arena == null) {
            board.advanceEnemyPreview();
            return;
        }
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            BoardSlot preview = board.enemyPreviewSlot(i);
            if (preview.isEmpty()) continue;
            if (!BoardRules.canAdvancePreviewToCombat(board.row(SlotOwner.ENEMY), i)) continue;

            Location from = arena.slotLocation(SlotOwner.ENEMY_PREVIEW, i);
            Location to = arena.slotLocation(SlotOwner.ENEMY, i);
            if (from != null && to != null) {
                BoardVfx.playMove(from, to, 8);
            }

            BoardCreature creature = preview.creature();
            CreatureEntityService.despawn(creature);
            preview.clear();
            creature.bind(board.enemySlot(i));
        }
        refreshCreatureLabels();
    }

    public BoardCreature findCreatureByEntity(UUID entityId) {
        String instanceId = null;
        Entity entity = null;
        for (var world : org.bukkit.Bukkit.getWorlds()) {
            entity = world.getEntity(entityId);
            if (entity != null) break;
        }
        if (entity != null) {
            instanceId = entity.getPersistentDataContainer().get(InscriptionKeys.CREATURE_INSTANCE, PersistentDataType.STRING);
        }
        if (instanceId == null) return null;

        for (SlotOwner owner : List.of(SlotOwner.PLAYER, SlotOwner.ENEMY, SlotOwner.ENEMY_PREVIEW)) {
            for (BoardSlot slot : board.row(owner)) {
                if (slot.isEmpty()) continue;
                BoardCreature creature = slot.creature();
                if (creature != null && creature.instanceId().toString().equals(instanceId)) {
                    return creature;
                }
            }
        }
        return null;
    }

    public BoardCreature findCreatureByInstanceId(UUID instanceId) {
        if (instanceId == null) return null;
        for (SlotOwner owner : List.of(SlotOwner.PLAYER, SlotOwner.ENEMY, SlotOwner.ENEMY_PREVIEW)) {
            for (BoardSlot slot : board.row(owner)) {
                if (slot.isEmpty()) continue;
                BoardCreature creature = slot.creature();
                if (creature != null && creature.instanceId().equals(instanceId)) {
                    return creature;
                }
            }
        }
        return null;
    }

    private boolean payCost(MatchSide side, CardDefinition def) {
        Currency currency = currency(side);
        return switch (def.costType()) {
            case FREE -> true;
            case BLOOD -> {
                if (!currency.trySpendBlood(def.cost())) {
                    feedback.actionBarWarn("<red>腐肉不足 ×%d".formatted(def.cost()));
                    yield false;
                }
                yield true;
            }
            case BONES -> {
                if (!currency.trySpendBones(def.cost())) {
                    feedback.actionBarWarn("<red>骨币不足 ×%d".formatted(def.cost()));
                    yield false;
                }
                yield true;
            }
            case FISH -> {
                if (!currency.trySpendFish(def.cost())) {
                    feedback.actionBarWarn("<red>鱼干不足 ×%d".formatted(def.cost()));
                    yield false;
                }
                yield true;
            }
        };
    }

    private void removeCardItemFromInventory(Player bukkit, String templateId) {
        String itemId = InscriptionItems.cardItemId(templateId);
        for (int i = 0; i < bukkit.getInventory().getSize(); i++) {
            if (MatchHotbar.isLockedSlot(i, deckMode())) continue;
            ItemStack stack = bukkit.getInventory().getItem(i);
            if (MCZJUGameCore.getItemManager().is(stack, itemId)) {
                int amount = stack.getAmount();
                if (amount <= 1) {
                    bukkit.getInventory().setItem(i, null);
                } else {
                    stack.setAmount(amount - 1);
                }
                return;
            }
        }
    }

    public void sacrifice(BoardCreature victim, MatchSide actingSide) {
        if (victim.owner() != actingSide) return;
        if (actingSide != actingSide()) {
            feedback.actionBarWarn("<yellow>不是你的回合");
            return;
        }
        if (turn.phase() != com.github.mczju.mczjuscription.game.turn.TurnPhase.PLAY) {
            return;
        }

        SigilRegistry.fire(SigilTrigger.ON_SACRIFICE, new SigilContext(this, SigilTrigger.ON_SACRIFICE, victim, null, 0));

        applySacrificeReward(actingSide, victim);
        playSacrificeFx(victim);

        if (victim.hasSigil(SigilId.ETERNAL_LIFE)) {
            syncHud();
            return;
        }

        removeCreatureFromBoard(victim);
        syncHud();
    }

    private void applySacrificeReward(MatchSide actingSide, BoardCreature victim) {
        currency(actingSide).addBlood(1);
        currency(actingSide).addBones(1);
        if (victim.hasSigil(SigilId.QUALITY_SACRIFICE)) {
            currency(actingSide).addBlood(2);
        }
    }

    private void playSacrificeFx(BoardCreature victim) {
        Location at = BoardVfx.locationOf(this, victim);
        if (at == null && victim.slot() != null && arena != null) {
            at = arena.slotLocation(victim.slot().owner(), victim.slot().index());
        }
        if (at != null) {
            BoardVfx.playSacrificeDrops(at);
        }
    }

    /** 对场上造物造成伤害；生命归零时按 killer 方结算死亡（护盾、硬壳等同战斗伤害）。 */
    public void damageCreature(BoardCreature creature, int amount, MatchSide killerSide) {
        if (creature == null || amount <= 0 || creature.isDead()) {
            return;
        }
        BoardSlot slot = creature.slot();
        if (slot == null || slot.isEmpty() || slot.creature() != creature) {
            return;
        }
        if (creature.absorbFirstHitWithShield()) {
            return;
        }
        int dmg = CombatModifiers.capIncomingDamage(creature, amount);
        creature.damage(dmg);
        if (creature.isDead()) {
            killCreature(creature, killerSide, false);
        }
    }

    public void killCreature(BoardCreature creature, MatchSide killer, boolean fromHammer) {
        BoardSlot slotBefore = creature.slot();
        SigilRegistry.fire(SigilTrigger.ON_DEATH, new SigilContext(this, SigilTrigger.ON_DEATH, creature, null, 0));

        // 亡语/成长等在槽位上替换为新造物后，不得再清槽，否则会只留下漂浮文字
        if (slotBefore != null) {
            BoardCreature onSlot = slotBefore.creature();
            if (onSlot != null && onSlot != creature) {
                boolean skipBone = creature.hasSigil(SigilId.BONE_ROYALTY);
                if (!skipBone) {
                    grantBones(creature.owner(), 1);
                }
                syncHud();
                return;
            }
        }

        boolean skipBone = creature.hasSigil(SigilId.BONE_ROYALTY);
        if (!skipBone) {
            grantBones(creature.owner(), 1);
        }
        removeCreatureFromBoard(creature);
        syncHud();
    }

    public void grantBones(MatchSide side, int amount) {
        currency(side).addBones(amount);
    }

    public void grantFish(MatchSide side, int amount) {
        currency(side).addFish(amount);
    }

    public void replaceWith(BoardSlot slot, String templateId, MatchSide owner) {
        if (arena != null) {
            Location loc = arena.slotLocation(slot.owner(), slot.index());
            if (loc != null) {
                BoardVfx.playTransform(loc);
            }
        }
        if (!slot.isEmpty() && slot.creature() != null) {
            CreatureEntityService.despawn(slot.creature());
        }
        slot.clear();
        BoardCreature replacement = new BoardCreature(templateId, owner);
        replacement.bind(slot);
        if (arena != null) {
            spawnCreatureEntity(replacement, slot.owner(), slot.index());
        }
    }

    private void removeCreatureFromBoard(BoardCreature creature) {
        Location loc = BoardVfx.locationOf(this, creature);
        if (loc != null) {
            BoardVfx.playDeath(loc);
        }
        CreatureEntityService.despawn(creature);
        BoardSlot slot = creature.slot();
        if (slot != null && slot.creature() == creature) {
            slot.clear();
        }
        refreshCreatureLabels();
    }

    public void despawnAllOnBoard() {
        for (SlotOwner owner : List.of(SlotOwner.PLAYER, SlotOwner.ENEMY, SlotOwner.ENEMY_PREVIEW)) {
            for (BoardSlot slot : board.row(owner)) {
                if (!slot.isEmpty() && slot.creature() != null) {
                    CreatureEntityService.despawn(slot.creature());
                    slot.clear();
                }
            }
        }
        CreatureEntityService.purgeOrphanBoardEntities();
    }

    public void checkRoundEnd() {
        MatchSide roundWinner = scales.checkRoundWinner();
        if (roundWinner == null) return;

        roundResolvedDuringCombat = true;
        BeamTargeting.cancel(this);

        MatchSide loser = roundWinner.opposite();
        life.extinguish(loser);
        feedback.announceRoundWon(describeSide(loser));

        scales.reset();
        despawnAllOnBoard();
        if (opponentController != null) {
            opponentController.onRoundReset(this);
        }

        if (life.isDefeated(loser)) {
            finishMatch(roundWinner);
        } else {
            syncHud();
        }
    }

    private String describeSide(MatchSide side) {
        if (mode() == MatchMode.SOLO_PVE) {
            return side == MatchSide.PLAYER ? "你" : "敌人";
        }
        return side == MatchSide.PLAYER ? "先手" : "后手";
    }

    private void finishMatch(MatchSide winner) {
        BeamTargeting.cancel(this);
        matchOver = true;
        matchWinner = winner;
        combatAnimating = false;
        scalesBossBar.stop();
        despawnAllOnBoard();
        game.onMatchFinished(winner);
    }

    public void forfeit(MatchSide quitter) {
        if (matchOver) return;
        finishMatch(quitter.opposite());
        feedback.actionBarInfo("<gray>%s 退出对局".formatted(describeSide(quitter)));
    }

    public void cleanup() {
        BeamTargeting.cancel(this);
        combatAnimating = false;
        scalesBossBar.stop();
        for (ParticipantState human : humanParticipants()) {
            human.hand().clear();
            human.mainDeck().clear();
            human.player().ifPresent(ext -> InscriptionItems.stripPlayerInventory(ext.player()));
        }
        despawnAllOnBoard();
        com.github.mczju.mczjuscription.roguelike.WanderingTraderService.despawnTrader(this);
        com.github.mczju.mczjuscription.shop.ShopVillagerService.despawnAllShopVillagers(this);
        breedingTracker.clear();
        ArenaManager.cleanupAll();
        arena = null;
    }

    public void syncHud() {
        if (matchOver) return;
        if (arena != null) {
            arena.refreshUiDisplays(this);
        }
        for (ParticipantState human : humanParticipants()) {
            human.player().ifPresent(ext -> MatchHotbar.placeTools(ext.player(), deckMode()));
        }
    }

}
