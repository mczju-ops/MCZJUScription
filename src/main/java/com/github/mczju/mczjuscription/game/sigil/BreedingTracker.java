package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.board.BattleBoard;
import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 记录本回合战斗开始时已在场的造物；
 * 回合末对仍存活者结算 {@link SigilTrigger#ON_TURN_END}（繁殖、幼雏等）。
 */
public final class BreedingTracker {

    private final Set<UUID> combatStartInstances = new HashSet<>();

    public void markCombatStart(BattleBoard board) {
        combatStartInstances.clear();
        for (SlotOwner owner : new SlotOwner[] {SlotOwner.PLAYER, SlotOwner.ENEMY}) {
            for (BoardSlot slot : board.row(owner)) {
                if (!slot.isEmpty() && slot.creature() != null) {
                    combatStartInstances.add(slot.creature().instanceId());
                }
            }
        }
    }

    public void resolveAfterCombat(InscriptionMatch match) {
        if (combatStartInstances.isEmpty()) {
            return;
        }
        for (SlotOwner owner : new SlotOwner[] {SlotOwner.PLAYER, SlotOwner.ENEMY}) {
            for (BoardSlot slot : match.board().row(owner)) {
                if (slot.isEmpty()) continue;
                BoardCreature creature = slot.creature();
                if (creature == null || creature.isDead()) continue;
                if (!combatStartInstances.contains(creature.instanceId())) continue;
                if (!hasActiveTurnEndSigil(creature)) continue;

                SigilRegistry.fire(
                        SigilTrigger.ON_TURN_END,
                        new SigilContext(match, SigilTrigger.ON_TURN_END, creature, null, 0)
                );
            }
        }
        combatStartInstances.clear();
    }

    public void clear() {
        combatStartInstances.clear();
    }

    private static boolean hasActiveTurnEndSigil(BoardCreature creature) {
        for (SigilId sigil : creature.activeSigils()) {
            if (SigilRegistry.triggerOf(sigil) == SigilTrigger.ON_TURN_END) {
                return true;
            }
        }
        return false;
    }
}
