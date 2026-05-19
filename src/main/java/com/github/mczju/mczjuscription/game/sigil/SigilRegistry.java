package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BoardShift;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardId;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SigilRegistry {

  private static final Map<SigilId, SigilHandler> HANDLERS = new EnumMap<>(SigilId.class);

  static {
    register(SigilId.SPIKY_ARMOR, ctx -> {
      if (ctx.target() != null && ctx.damage() > 0) {
        ctx.target().damage(1);
      }
    });

    register(SigilId.BEE_STING, ctx ->
        ctx.match().grantCardToHandSilent(ctx.source().owner(), CardId.BEE.name()));

    register(SigilId.BONE_ROYALTY, ctx -> ctx.match().grantBones(ctx.source().owner(), 4));

    register(
        SigilId.COPY_ON_DEATH,
        ctx -> ctx.match().grantCardToHandSilent(ctx.source().owner(), ctx.source().templateId()));

    register(SigilId.ICY_ENTOMB, ctx -> IcyEntombRelease.release(ctx.match(), ctx.source()));

    register(
        SigilId.STEEL_TRAP,
        ctx -> {
          BoardCreature opposite = BoardTokens.oppositeInLane(ctx.match().board(), ctx.source());
          if (opposite != null && !opposite.isDead()) {
            ctx.match().killCreature(opposite, ctx.source().owner(), false);
          }
          ctx.match().grantCardToHandSilent(ctx.source().owner(), CardId.PELT.name());
        });

    register(
        SigilId.BREEDING,
        ctx -> ctx.match().grantCardToHandSilent(ctx.source().owner(), ctx.source().templateId()));

    register(SigilId.FLEDGLING, ctx -> FledglingGrowth.mature(ctx.match(), ctx.source()));

    for (SigilId onPlay :
        List.of(
            SigilId.RABBIT_HOLE,
            SigilId.COPY_ON_PLAY,
            SigilId.ANT_QUEEN,
            SigilId.DAM_BUILDER,
            SigilId.BELL_RINGER)) {
      register(onPlay, ctx -> OnPlayEffects.apply(ctx.match(), ctx.source(), onPlay));
    }

    register(SigilId.RUSH_LEFT, ctx -> BoardShift.move(ctx.match(), ctx.source(), -1));
    register(SigilId.RUSH_RIGHT, ctx -> BoardShift.move(ctx.match(), ctx.source(), 1));

    register(SigilId.TUTOR, ctx -> TutorEffect.open(ctx.match(), ctx.source()));

    register(
        SigilId.RANDOM_SIGIL,
        ctx -> {
          SigilId rolled = RandomSigilPool.roll();
          ctx.source().addBonusSigil(rolled);
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
    all.addAll(
        match.board().occupiedSlots(MatchSide.PLAYER).stream().map(s -> s.creature()).toList());
    all.addAll(
        match.board().occupiedSlots(MatchSide.ENEMY).stream().map(s -> s.creature()).toList());
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

  public static SigilTrigger triggerOf(SigilId id) {
    return switch (id) {
      case RABBIT_HOLE,
          COPY_ON_PLAY,
          ANT_QUEEN,
          DAM_BUILDER,
          BELL_RINGER,
          TUTOR,
          RANDOM_SIGIL,
          ITEM_VENDOR ->
          SigilTrigger.ON_PLAY;
      case BEE_STING, SPIKY_ARMOR -> SigilTrigger.ON_ATTACKED;
      case BONE_ROYALTY, COPY_ON_DEATH, ICY_ENTOMB, STEEL_TRAP -> SigilTrigger.ON_DEATH;
      case BREEDING, FLEDGLING, RUSH_LEFT, RUSH_RIGHT -> SigilTrigger.ON_TURN_END;
      case ETERNAL_LIFE, QUALITY_SACRIFICE -> SigilTrigger.ON_SACRIFICE;
      case STINKY, LEADER_POWER, ROCK_BODY -> SigilTrigger.AURA;
      case AIR_STRIKE,
          WATER_STRIKE,
          HIGH_JUMP,
          TOUCH_OF_DEATH,
          PREVENT_ATTACK,
          SPLIT_STRIKE,
          TRI_STRIKE,
          ALL_STRIKE -> SigilTrigger.SPECIAL;
      case GUARD_DOG, WHACK_A_MOLE, TAIL_ON_HIT -> SigilTrigger.PRE_COMBAT;
      case RUSH_PUSH -> SigilTrigger.ON_TURN_END;
      case CORPSE_EATER -> SigilTrigger.SPECIAL;
      case ORBIT -> SigilTrigger.ON_TURN_START;
    };
  }
}
