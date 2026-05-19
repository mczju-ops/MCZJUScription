package com.github.mczju.mczjuscription.game.match;

import com.github.mczju.mczjuscription.arena.ArenaFacing;
import com.github.mczju.mczjuscription.arena.ArenaManager;
import com.github.mczju.mczjuscription.arena.BattleArena;
import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.AbstractInscriptionGame;
import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardRules;
import com.github.mczju.mczjuscription.game.board.BoardSides;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardDefinition;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.card.CardRegistry;
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
import com.github.mczju.mczjuscription.game.turn.TurnController;
import com.github.mczju.mczjuscription.item.InscriptionItems;
import com.github.mczju.mczjuscription.ui.MatchFeedback;
import com.github.mczju.mczjuscription.ui.MatchHotbar;
import com.github.mczju.mczjuscription.ui.ResourceHotbar;
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

    public void start() {
        OpeningHandDealer.deal(this);
        turn.enterPhase(com.github.mczju.mczjuscription.game.turn.TurnPhase.DRAW);
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

    public boolean isCombatAnimating() {
        return combatAnimating;
    }

    public void setCombatAnimating(boolean combatAnimating) {
        this.combatAnimating = combatAnimating;
    }

    public MatchSide matchWinner() {
        return matchWinner;
    }

    public void planOpponentPreview() {
        if (opponentController != null) {
            opponentController.planPreview(this);
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

    public void drawFromRabbitPile(MatchSide side) {
        if (side != actingSide()) {
            feedback.actionBarWarn("<yellow>不是你的回合");
            return;
        }
        if (!turn.canDrawFromRabbitPile()) {
            return;
        }
        grantCardToHand(side, CardId.RABBIT);
        turn.onDrawFromRabbitPile();
    }

    public void grantCardToHand(MatchSide side, CardId cardId) {
        grantCardToHandSilent(side, cardId);
        syncHud();
    }

    public void grantCardToHandSilent(MatchSide side, CardId cardId) {
        ParticipantState state = participant(side);
        state.hand().add(cardId);
        state.player().ifPresent(ext ->
                ext.player().getInventory().addItem(InscriptionItems.card(cardId).getItem())
        );
    }

    public boolean playCardToSlot(CardId cardId, int slotIndex, MatchSide actingSide) {
        return playCardToSlot(cardId, slotIndex, actingSide, null);
    }

    /**
     * @param droppedItem 丢牌召唤时场上的掉落物；已从背包脱出，成功时移除该实体而非再扫背包
     */
    public boolean playCardToSlot(CardId cardId, int slotIndex, MatchSide actingSide, org.bukkit.entity.Item droppedItem) {
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

        SlotOwner slotOwner = BoardSides.toSlotOwner(actingSide);
        BoardSlot[] row = board.row(slotOwner);
        if (!BoardRules.canPlaceAt(row, slotIndex)) {
            return false;
        }
        BoardSlot slot = board.slot(slotOwner, slotIndex);

        ParticipantState state = participant(actingSide);
        if (!state.hand().contains(cardId)) {
            return false;
        }

        CardDefinition def = CardRegistry.get(cardId);
        if (!payCost(actingSide, def)) return false;

        state.hand().remove(cardId);
        if (droppedItem != null && droppedItem.isValid()) {
            droppedItem.remove();
        } else {
            state.player().ifPresent(ext -> removeCardItemFromInventory(ext.player(), cardId));
        }

        BoardCreature creature = new BoardCreature(cardId, actingSide);
        creature.bind(slot);
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
                BoardVfx.playMove(from, to);
            }

            BoardCreature creature = preview.creature();
            CreatureEntityService.despawn(creature);
            preview.clear();
            creature.bind(board.enemySlot(i));
        }
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
        };
    }

    private void removeCardItemFromInventory(Player bukkit, CardId cardId) {
        String itemId = InscriptionItems.cardItemId(cardId);
        for (int i = 0; i < bukkit.getInventory().getSize(); i++) {
            if (MatchHotbar.isLockedSlot(i)) continue;
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

        int value = victim.definition().sacrificeValue();
        if (victim.hasSigil(SigilId.QUALITY_SACRIFICE)) {
            value = 3;
        }
        currency(actingSide).addBlood(value);
        SigilRegistry.fire(SigilTrigger.ON_SACRIFICE, new SigilContext(this, SigilTrigger.ON_SACRIFICE, victim, null, 0));

        if (victim.hasSigil(SigilId.ETERNAL_LIFE)) {
            grantCardToHand(actingSide, victim.cardId());
        }

        removeCreatureFromBoard(victim);
        syncHud();
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

    public void replaceWith(BoardSlot slot, CardId cardId, MatchSide owner) {
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
        BoardCreature replacement = new BoardCreature(cardId, owner);
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
        CreatureEntityService.purgeAllMatchCreatures();
    }

    public void checkRoundEnd() {
        MatchSide roundWinner = scales.checkRoundWinner();
        if (roundWinner == null) return;

        MatchSide loser = roundWinner.opposite();
        life.extinguish(loser);
        feedback.announceRoundWon(describeSide(loser));

        scales.reset();
        despawnAllOnBoard();

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
        combatAnimating = false;
        scalesBossBar.stop();
        for (ParticipantState human : humanParticipants()) {
            human.hand().clear();
            human.mainDeck().clear();
            human.player().ifPresent(ext -> InscriptionItems.stripPlayerInventory(ext.player()));
        }
        despawnAllOnBoard();
        breedingTracker.clear();
        ArenaManager.cleanupAll();
        arena = null;
    }

    public void syncHud() {
        if (matchOver) return;
        for (ParticipantState human : humanParticipants()) {
            human.player().ifPresent(ext -> {
                MatchHotbar.placeTools(ext.player(), deckMode());
                ResourceHotbar.sync(ext.player(), human.currency());
            });
        }
    }

}
