package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.board.BoardSlot;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class BoardCreature {

  private final UUID instanceId = UUID.randomUUID();
  private final String templateId;
  private final MatchSide owner;
  private BoardSlot slot;
  private int health;
  private int powerModifier;
  private String displayNameOverride;
  private boolean maturedFromFledgling;
  private UUID entityId;
  private UUID displayEntityId;
  private final Set<SigilId> bonusSigils = EnumSet.noneOf(SigilId.class);

  public BoardCreature(String templateId, MatchSide owner) {
    this.templateId = templateId;
    this.owner = owner;
    this.health = CardCatalog.require(templateId).health();
  }

  public BoardCreature(CardId cardId, MatchSide owner) {
    this(cardId.name(), owner);
  }

  public String displayName() {
    return displayNameOverride != null ? displayNameOverride : template().displayName();
  }

  public boolean hasMaturedFromFledgling() {
    return maturedFromFledgling;
  }

  public void applyElderForm() {
    if (maturedFromFledgling) return;
    maturedFromFledgling = true;
    displayNameOverride = "长老" + template().displayName();
    modifyPower(1);
    health += 1;
  }

  public UUID instanceId() {
    return instanceId;
  }

  public String templateId() {
    return templateId;
  }

  public CardId cardId() {
    try {
      return CardId.valueOf(templateId);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  public CardTemplate template() {
    return CardCatalog.require(templateId);
  }

  public CardDefinition definition() {
    return template().toDefinition();
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
    return Math.max(0, template().power() + powerModifier);
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

  public void addBonusSigil(SigilId sigil) {
    if (!SigilRules.canAdd(activeSigils(), sigil)) {
      return;
    }
    bonusSigils.add(sigil);
    com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
  }

  public Set<SigilId> activeSigils() {
    if (template().sigils().isEmpty() && bonusSigils.isEmpty()) {
      return Set.of();
    }
    Set<SigilId> out = EnumSet.noneOf(SigilId.class);
    out.addAll(template().sigils());
    out.addAll(bonusSigils);
    if (maturedFromFledgling) {
      out.remove(SigilId.FLEDGLING);
    }
    return Collections.unmodifiableSet(out);
  }

  public boolean hasSigil(SigilId sigil) {
    if (maturedFromFledgling && sigil == SigilId.FLEDGLING) {
      return false;
    }
    return template().hasSigil(sigil) || bonusSigils.contains(sigil);
  }
}
