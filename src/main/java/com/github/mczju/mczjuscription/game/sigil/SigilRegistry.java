package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardShift;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SigilRegistry {

  private static final Map<SigilId, SigilHandler> HANDLERS = new EnumMap<>(SigilId.class);

  static {
    register(SigilId.SPIKY_ARMOR, ctx -> {
      if (ctx.target() != null && ctx.damage() > 0) {
        ctx.match().damageCreature(ctx.target(), 1, ctx.source().owner());
      }
    });

    register(SigilId.BONE_ROYALTY, ctx -> ctx.match().grantBones(ctx.source().owner(), 4));

    register(
        SigilId.COPY_ON_DEATH,
        ctx -> ctx.match().grantCardToHandSilent(ctx.source().owner(), ctx.source().templateId()));

    register(SigilId.BREEDING, ctx -> ctx.match().grantCardToHandSilent(ctx.source().owner(), ctx.source().templateId()));

    register(SigilId.FLEDGLING, ctx -> EvolutionResolver.mature(ctx.match(), ctx.source()));

    register(SigilId.AFFLICTION, ctx -> EvolutionResolver.mature(ctx.match(), ctx.source()));

    register(SigilId.EVOKE_VEX, ctx -> BoardTokens.spawnOnAdjacentEmpty(ctx.match(), ctx.source(), "mob_vex"));

    register(SigilId.ENDER_SHIFT, ctx -> BoardShift.moveRandomEmpty(ctx.match(), ctx.source(), BoardShift.ShiftStyle.ENDER_TELEPORT));

    register(SigilId.GUST_SHIFT, ctx -> BoardShift.moveRandomEmpty(ctx.match(), ctx.source(), BoardShift.ShiftStyle.BREEZE_JUMP));

    register(
        SigilId.INK,
        ctx -> {
          if (ctx.target() != null && ctx.damage() > 0) {
            ctx.target().markSkipNextAttack();
          }
        });

    register(SigilId.SNIFF_STEAL, ctx -> resolveSniffSteal(ctx.match(), ctx.source()));

    register(SigilId.FISH_BAIT, ctx -> ctx.match().grantFish(ctx.source().owner(), 1));

    register(SigilId.DEMON_OFFER, ctx -> ctx.match().grantBones(ctx.source().owner(), 3));

    register(
        SigilId.FERMENT,
        ctx -> ctx.match().currency(ctx.source().owner()).addBlood(2));

    register(SigilId.TRADE, ctx -> ctx.match().grantBones(ctx.source().owner(), 1));

    register(SigilId.WATER_STORE, ctx -> ctx.source().heal(1));

    register(SigilId.WANDER, ctx -> WanderHandler.wander(ctx.match(), ctx.source()));

    register(SigilId.SELF_DESTRUCT, ctx -> SelfDestructHandler.explode(ctx.match(), ctx.source()));

    register(SigilId.SPLIT_SPAWN, ctx -> SplitSpawnDeath.spawn(ctx.match(), ctx.source()));

    register(
        SigilId.STEAL_BONE,
        ctx -> {
          if (ctx.target() != null && ctx.damage() > 0) {
            ctx.match().grantBones(ctx.source().owner(), 1);
          }
        });

    register(
        SigilId.WEB_WEAK,
        ctx -> {
          if (ctx.damage() > 0) {
            ctx.source().modifyPower(-1);
          }
        });

    register(
        SigilId.SLOW,
        ctx -> {
          if (ctx.damage() > 0) {
            ctx.source().modifyPower(-1);
          }
        });

    register(
        SigilId.ABUNDANCE,
        ctx -> {
          if (ctx.damage() > 0) {
            MatchSide owner = ctx.source().owner();
            ctx.match().grantBones(owner, 1);
            ctx.match().currency(owner).addBlood(1);
          }
        });
  }

  private SigilRegistry() {}

  public static void register(SigilId id, SigilHandler handler) {
    HANDLERS.put(id, handler);
  }

  public static void fire(SigilTrigger trigger, SigilContext context) {
    BoardCreature source = context.source();
    for (SigilId sigil : source.activeSigils()) {
      if (triggerOf(sigil) == trigger && HANDLERS.containsKey(sigil)) {
        HANDLERS.get(sigil).apply(context);
      }
    }
  }

  public static void fireOnBoard(SigilTrigger trigger, InscriptionMatch match, BoardCreature primary) {
    List<BoardCreature> all = new ArrayList<>();
    all.addAll(match.board().occupiedSlots(MatchSide.PLAYER).stream().map(s -> s.creature()).toList());
    all.addAll(match.board().occupiedSlots(MatchSide.ENEMY).stream().map(s -> s.creature()).toList());
    for (BoardCreature creature : all) {
      if (creature.activeSigils().isEmpty()) continue;
      SigilContext ctx = new SigilContext(match, trigger, creature, primary, 0);
      for (SigilId sigil : creature.activeSigils()) {
        if (triggerOf(sigil) == trigger && HANDLERS.containsKey(sigil)) {
          HANDLERS.get(sigil).apply(ctx);
        }
      }
    }
  }

  /** 嗅探：从对方场上随机偷一个印记替换本卡上的嗅探。 */
  public static void resolveSniffSteal(InscriptionMatch match, BoardCreature sniffer) {
    if (!sniffer.hasSigil(SigilId.SNIFF_STEAL)) return;
    MatchSide enemy = sniffer.owner().opposite();
    List<SigilId> pool = new ArrayList<>();
    for (var slot : match.board().occupiedSlots(enemy)) {
      BoardCreature c = slot.creature();
      if (c == null || c == sniffer) continue;
      for (SigilId s : c.activeSigils()) {
        if (s != SigilId.SNIFF_STEAL) pool.add(s);
      }
    }
    if (pool.isEmpty()) return;
    SigilId stolen = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    sniffer.addBonusSigil(stolen);
    sniffer.consumeSniffSigil();
  }

  public static SigilTrigger triggerOf(SigilId id) {
    return switch (id) {
      case EVOKE_VEX, SURPRISE_ENTRY -> SigilTrigger.ON_PLAY;
      case SPIKY_ARMOR, INK, WEB_WEAK, SLOW, ABUNDANCE -> SigilTrigger.ON_ATTACKED;
      case STEAL_BONE -> SigilTrigger.ON_COMBAT_ATTACK;
      case BONE_ROYALTY, COPY_ON_DEATH, SELF_DESTRUCT, SPLIT_SPAWN -> SigilTrigger.ON_DEATH;
      case BREEDING, FERMENT, TRADE, WATER_STORE, FLEDGLING, AFFLICTION, WANDER, SNIFF_STEAL ->
          SigilTrigger.ON_TURN_END;
      case QUALITY_SACRIFICE, DEMON_OFFER, ETERNAL_LIFE, FISH_BAIT -> SigilTrigger.ON_SACRIFICE;
      case ENDER_SHIFT -> SigilTrigger.ON_ATTACKED;
      case GUST_SHIFT -> SigilTrigger.ON_COMBAT_ATTACK;
      case STINKY, STINKY_FAR, RIDING, TAUNT_AURA, SCORCH, HISS -> SigilTrigger.AURA;
      case AIR_STRIKE,
          WATER_STRIKE,
          HIGH_JUMP,
          SPLIT_STRIKE,
          TRI_STRIKE,
          ALL_STRIKE,
          BEAM,
          DOUBLE_STRIKE,
          VENOM_KILL,
          HARD_SHELL,
          FIRST_SHIELD,
          INTIMIDATE,
          SONAR,
          GUARD_DOG,
          RUSH_PUSH ->
          SigilTrigger.SPECIAL;
    };
  }
}
