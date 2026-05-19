package com.github.mczju.mczjuscription.game.sigil;

import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import org.jetbrains.annotations.Nullable;

public final class SigilContext {

    private final InscriptionMatch match;
    private final SigilTrigger trigger;
    private final BoardCreature source;
    private final @Nullable BoardCreature target;
    private final int damage;

    public SigilContext(
            InscriptionMatch match,
            SigilTrigger trigger,
            BoardCreature source,
            @Nullable BoardCreature target,
            int damage
    ) {
        this.match = match;
        this.trigger = trigger;
        this.source = source;
        this.target = target;
        this.damage = damage;
    }

    public InscriptionMatch match() {
        return match;
    }

    public SigilTrigger trigger() {
        return trigger;
    }

    public BoardCreature source() {
        return source;
    }

    public @Nullable BoardCreature target() {
        return target;
    }

    public int damage() {
        return damage;
    }
}
