package com.github.mczju.mczjuscription.game.board;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import org.jetbrains.annotations.Nullable;

public final class BoardSlot {

    public static final int SLOT_COUNT = 4;

    private final SlotOwner owner;
    private final int index;
    private BoardCreature creature;

    public BoardSlot(SlotOwner owner, int index) {
        if (index < 0 || index >= SLOT_COUNT) {
            throw new IllegalArgumentException("slot index out of range: " + index);
        }
        this.owner = owner;
        this.index = index;
    }

    public SlotOwner owner() {
        return owner;
    }

    public int index() {
        return index;
    }

    public boolean isEmpty() {
        return creature == null;
    }

    public @Nullable BoardCreature creature() {
        return creature;
    }

    public void setCreature(@Nullable BoardCreature creature) {
        this.creature = creature;
    }

    public void clear() {
        this.creature = null;
    }
}
