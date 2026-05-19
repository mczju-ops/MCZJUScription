package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
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

        register(SigilId.BONE_ROYALTY, ctx ->
                ctx.match().grantBones(ctx.source().owner(), 4));

        register(SigilId.BREEDING, ctx ->
                ctx.match().grantCardToHandSilent(ctx.source().owner(), ctx.source().cardId()));

        register(SigilId.FLEDGLING, ctx ->
                FledglingGrowth.mature(ctx.match(), ctx.source()));
    }

    private SigilRegistry() {}

    public static void register(SigilId id, SigilHandler handler) {
        HANDLERS.put(id, handler);
    }

    public static void fire(SigilTrigger trigger, SigilContext context) {
        BoardCreature source = context.source();
        for (SigilId sigil : source.definition().sigils()) {
            if (sigilTrigger(sigil) == trigger && HANDLERS.containsKey(sigil)) {
                HANDLERS.get(sigil).apply(context);
            }
        }
    }

    public static void fireOnBoard(SigilTrigger trigger, InscriptionMatch match, BoardCreature primary) {
        List<BoardCreature> all = new ArrayList<>();
        all.addAll(match.board().occupiedSlots(MatchSide.PLAYER).stream()
                .map(s -> s.creature()).toList());
        all.addAll(match.board().occupiedSlots(MatchSide.ENEMY).stream()
                .map(s -> s.creature()).toList());
        for (BoardCreature creature : all) {
            if (creature.definition().sigils().isEmpty()) continue;
            SigilContext ctx = new SigilContext(match, trigger, creature, primary, 0);
            for (SigilId sigil : creature.definition().sigils()) {
                if (sigilTrigger(sigil) == trigger && HANDLERS.containsKey(sigil)) {
                    HANDLERS.get(sigil).apply(ctx);
                }
            }
        }
    }

    public static SigilTrigger triggerOf(SigilId id) {
        return switch (id) {
            case RABBIT_HOLE -> SigilTrigger.ON_PLAY;
            case SPIKY_ARMOR -> SigilTrigger.ON_ATTACKED;
            case BONE_ROYALTY -> SigilTrigger.ON_DEATH;
            case ICY_ENTOMB -> SigilTrigger.ON_DEATH;
            case BREEDING, FLEDGLING -> SigilTrigger.ON_TURN_END;
            default -> SigilTrigger.SPECIAL;
        };
    }

    private static SigilTrigger sigilTrigger(SigilId id) {
        return triggerOf(id);
    }
}
