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
  private boolean firstShieldConsumed;
  private boolean sniffConsumed;
  private boolean skipNextAttack;
  private UUID entityId;
  private UUID displayEntityId;
  private final Set<SigilId> bonusSigils = EnumSet.noneOf(SigilId.class);

  public BoardCreature(String templateId, MatchSide owner) {
    this.templateId = templateId;
    this.owner = owner;
    this.health = CardCatalog.require(templateId).health();
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
    if (this.slot != null && this.slot != slot) {
      this.slot.clear();
    }
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

  /** 潜影贝【护盾】：免疫第一次受到伤害。 */
  public boolean absorbFirstHitWithShield() {
    if (!hasSigil(SigilId.FIRST_SHIELD) || firstShieldConsumed) {
      return false;
    }
    firstShieldConsumed = true;
    com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
    return true;
  }

  /** 【墨水】：下一会合攻击阶段跳过该造物的一次攻击。 */
  public void markSkipNextAttack() {
    skipNextAttack = true;
    com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
    com.github.mczju.mczjuscription.vfx.InkAuraVfx.start(this);
  }

  public boolean willSkipNextAttack() {
    return skipNextAttack;
  }

  public boolean consumesSkipNextAttack() {
    if (!skipNextAttack) {
      return false;
    }
    skipNextAttack = false;
    com.github.mczju.mczjuscription.vfx.InkAuraVfx.stop(this);
    return true;
  }

  public void heal(int amount) {
    if (amount <= 0) return;
    health += amount;
    com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
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

  public void consumeSniffSigil() {
    sniffConsumed = true;
    com.github.mczju.mczjuscription.entity.CreatureEntityService.refreshLabel(this);
  }

  public boolean hasSigil(SigilId sigil) {
    if (maturedFromFledgling && sigil == SigilId.FLEDGLING) {
      return false;
    }
    if (sniffConsumed && sigil == SigilId.SNIFF_STEAL) {
      return false;
    }
    return template().hasSigil(sigil) || bonusSigils.contains(sigil);
  }
}
