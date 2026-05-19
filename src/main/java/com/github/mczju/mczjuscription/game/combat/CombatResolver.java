package com.github.mczju.mczjuscription.game.combat;

import com.github.mczju.mczjuscription.entity.CreatureAnimator;
import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilContext;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilRegistry;
import com.github.mczju.mczjuscription.game.sigil.SigilTrigger;
import com.github.mczju.mczjuscription.vfx.BoardVfx;
import org.bukkit.Location;

/**
 * 战斗结算。{@link #resolveSideCombatAnimated} 按槽位 0→3 顺序播放攻击动画并结算。
 */
public final class CombatResolver {

    private final InscriptionMatch match;

    public CombatResolver(InscriptionMatch match) {
        this.match = match;
    }

    /** @deprecated 瞬时结算，请使用 {@link #resolveSideCombatAnimated} */
    public void resolveSideCombat(MatchSide attackerSide) {
        for (int i = 0; i < BoardSlot.SLOT_COUNT; i++) {
            applySlot(attackerSide, i);
        }
    }

    public void resolveSideCombatAnimated(MatchSide attackerSide, Runnable onComplete) {
        resolveSlotAnimated(attackerSide, 0, onComplete);
    }

    private void resolveSlotAnimated(MatchSide attackerSide, int slotIndex, Runnable onComplete) {
        if (slotIndex >= BoardSlot.SLOT_COUNT || match.isMatchOver()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        SlotStrike strike = evaluateSlot(attackerSide, slotIndex);
        if (strike == null) {
            CreatureAnimator.schedule(
                    () -> resolveSlotAnimated(attackerSide, slotIndex + 1, onComplete),
                    4L
            );
            return;
        }

        strike.play(() -> {
            strike.apply();
            match.syncHud();
            if (match.isMatchOver()) {
                if (onComplete != null) onComplete.run();
                return;
            }
            CreatureAnimator.schedule(
                    () -> resolveSlotAnimated(attackerSide, slotIndex + 1, onComplete),
                    CreatureAnimator.PAUSE_TICKS
            );
        });
    }

    private SlotStrike evaluateSlot(MatchSide attackerSide, int slotIndex) {
        SlotOwner attackerOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.PLAYER : SlotOwner.ENEMY;
        SlotOwner defenderOwner = attackerSide == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
        BattleBoard board = match.board();

        BoardSlot attackerSlot = board.slot(attackerOwner, slotIndex);
        if (attackerSlot.isEmpty()) return null;

        BoardCreature attacker = attackerSlot.creature();
        BoardSlot defenderSlot = board.slot(defenderOwner, slotIndex);
        BoardCreature defender = defenderSlot.isEmpty() ? null : defenderSlot.creature();
        boolean submerged = defender != null
                && defender.hasSigil(SigilId.WATER_STRIKE)
                && defender.owner() != attackerSide;

        if (defender != null && !submerged && !attacker.hasSigil(SigilId.AIR_STRIKE)) {
            int atk = computeAttack(attacker, defender);
            if (atk <= 0) return null;
            boolean instantKill = attacker.hasSigil(SigilId.TOUCH_OF_DEATH);
            return new CreatureStrike(match, attacker, defender, atk, instantKill);
        }
        if (defender == null || submerged || attacker.hasSigil(SigilId.AIR_STRIKE)) {
            int damage = attacker.currentAttack();
            if (damage <= 0) return null;
            return new DirectStrike(match, attacker, attackerSide.opposite(), damage);
        }
        return null;
    }

    private void applySlot(MatchSide attackerSide, int slotIndex) {
        SlotStrike strike = evaluateSlot(attackerSide, slotIndex);
        if (strike == null) return;
        strike.apply();
    }

    private static int computeAttack(BoardCreature attacker, BoardCreature defender) {
        int atk = attacker.currentAttack();
        if (defender.hasSigil(SigilId.STINKY) && !attacker.hasSigil(SigilId.ROCK_BODY)) {
            atk = Math.max(0, atk - 1);
        }
        return atk;
    }

    private sealed interface SlotStrike permits CreatureStrike, DirectStrike {
        void play(Runnable onFinished);

        void apply();
    }

    private static final class CreatureStrike implements SlotStrike {
        private final InscriptionMatch match;
        private final BoardCreature attacker;
        private final BoardCreature defender;
        private final int atk;
        private final boolean instantKill;

        private CreatureStrike(
                InscriptionMatch match,
                BoardCreature attacker,
                BoardCreature defender,
                int atk,
                boolean instantKill
        ) {
            this.match = match;
            this.attacker = attacker;
            this.defender = defender;
            this.atk = atk;
            this.instantKill = instantKill;
        }

        @Override
        public void play(Runnable onFinished) {
            Location home = standOf(match, attacker);
            Location target = standOf(match, defender);
            CreatureAnimator.playAttackSequence(attacker, home, target, () -> {
                playAttackVfx(match, defender, atk, instantKill);
            }, onFinished);
        }

        @Override
        public void apply() {
            if (instantKill) {
                match.killCreature(defender, attacker.owner(), false);
                return;
            }
            defender.damage(atk);
            SigilRegistry.fire(
                    SigilTrigger.ON_ATTACKED,
                    new SigilContext(match, SigilTrigger.ON_ATTACKED, defender, attacker, atk)
            );
            if (defender.isDead()) {
                match.killCreature(defender, attacker.owner(), false);
            }
        }
    }

    private static final class DirectStrike implements SlotStrike {
        private final InscriptionMatch match;
        private final BoardCreature attacker;
        private final MatchSide victimSide;
        private final int damage;

        private DirectStrike(InscriptionMatch match, BoardCreature attacker, MatchSide victimSide, int damage) {
            this.match = match;
            this.attacker = attacker;
            this.victimSide = victimSide;
            this.damage = damage;
        }

        @Override
        public void play(Runnable onFinished) {
            Location home = standOf(match, attacker);
            Location target = directAttackLaneTarget(match, attacker);
            CreatureAnimator.playAttackSequence(attacker, home, target, () -> {
                BoardVfx.playDirectDamage(match, victimSide, damage);
            }, onFinished);
        }

        @Override
        public void apply() {
            if (victimSide == MatchSide.PLAYER) {
                match.scales().damagePlayer(damage);
            } else {
                match.scales().damageEnemy(damage);
            }
            match.checkRoundEnd();
        }
    }

    private static Location standOf(InscriptionMatch match, BoardCreature creature) {
        Location slot = BoardVfx.locationOf(match, creature);
        return CreatureAnimator.slotStand(slot);
    }

    /** 直击时冲向正前方同列空槽，不转向天平或玩家本体。 */
    private static Location directAttackLaneTarget(InscriptionMatch match, BoardCreature attacker) {
        if (match.arena() == null || attacker.slot() == null) {
            return standOf(match, attacker);
        }
        SlotOwner laneRow = attacker.owner() == MatchSide.PLAYER ? SlotOwner.ENEMY : SlotOwner.PLAYER;
        Location inFront = match.arena().slotLocation(laneRow, attacker.slot().index());
        if (inFront != null) {
            return CreatureAnimator.slotStand(inFront);
        }
        return standOf(match, attacker);
    }

    private static void playAttackVfx(InscriptionMatch match, BoardCreature defender, int damage, boolean instantKill) {
        Location loc = BoardVfx.locationOf(match, defender);
        if (loc != null) {
            BoardVfx.playAttackAt(loc, damage, instantKill);
        }
    }
}
