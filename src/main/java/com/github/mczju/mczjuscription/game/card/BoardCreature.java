package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.match.MatchSide;

import java.util.UUID;

public final class BoardCreature {

    private final UUID instanceId = UUID.randomUUID();
    private final CardId cardId;
    private final MatchSide owner;
    private BoardSlot slot;
    private int health;
    private int powerModifier;
    private String displayNameOverride;
    private boolean maturedFromFledgling;
    private UUID entityId;
    private UUID displayEntityId;

    public BoardCreature(CardId cardId, MatchSide owner) {
        this.cardId = cardId;
        this.owner = owner;
        this.health = CardRegistry.get(cardId).health();
    }

    public String displayName() {
        return displayNameOverride != null ? displayNameOverride : definition().displayName();
    }

    public boolean hasMaturedFromFledgling() {
        return maturedFromFledgling;
    }

    /** 被附加幼雏的通用成长：+1/+1，名称变为「长老xx」。 */
    public void applyElderForm() {
        if (maturedFromFledgling) return;
        maturedFromFledgling = true;
        displayNameOverride = "长老" + definition().displayName();
        modifyPower(1);
        health += 1;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public CardId cardId() {
        return cardId;
    }

    public CardDefinition definition() {
        return CardRegistry.get(cardId);
    }

    public MatchSide owner() {
        return owner;
    }

    public BoardSlot slot() {
        return slot;
    }

    public void bind(BoardSlot slot) {
        this.slot = slot;
        slot.setCreature(this);
    }

    public int currentPower() {
        return Math.max(0, definition().power() + powerModifier);
    }

    public int currentAttack() {
        return currentPower();
    }

    public int health() {
        return health;
    }

    public void modifyPower(int delta) {
        powerModifier += delta;
    }

    public UUID entityId() {
        return entityId;
    }

    public UUID displayEntityId() {
        return displayEntityId;
    }

    public void bindEntity(UUID entityId, UUID displayEntityId) {
        this.entityId = entityId;
        this.displayEntityId = displayEntityId;
    }

    public void clearEntityRefs() {
        this.entityId = null;
        this.displayEntityId = null;
    }

    public void damage(int amount) {
        health -= amount;
        com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public boolean hasSigil(com.github.mczju.mczjuscription.game.sigil.SigilId sigil) {
        if (maturedFromFledgling && sigil == com.github.mczju.mczjuscription.game.sigil.SigilId.FLEDGLING) {
            return false;
        }
        return definition().hasSigil(sigil);
    }
}
