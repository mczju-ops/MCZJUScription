package com.github.mczju.mczjuscription.data;

import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.CostType;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.EntityType;

/** 卡牌设计器编辑会话（内存）。 */
public final class CardDesignerSession {

  private static final Map<UUID, CardDesignerSession> SESSIONS = new ConcurrentHashMap<>();

  private String editingId;
  private String displayName = "新卡";
  private EntityType entityType = EntityType.WOLF;
  private final List<SigilId> sigils = new ArrayList<>();
  private int power = 1;
  private int health = 1;
  private int bloodCost = 1;
  private int boneCost = 0;

  public static CardDesignerSession of(UUID playerId) {
    return SESSIONS.computeIfAbsent(playerId, id -> new CardDesignerSession());
  }

  public static void remove(UUID playerId) {
    SESSIONS.remove(playerId);
  }

  public void load(CardTemplate template) {
    editingId = template.id();
    displayName = template.displayName();
    entityType = template.entityType();
    sigils.clear();
    sigils.addAll(template.sigils());
    power = template.power();
    health = template.health();
    if (template.costType() == CostType.BONES) {
      boneCost = template.cost();
      bloodCost = 0;
    } else if (template.costType() == CostType.BLOOD) {
      bloodCost = template.cost();
      boneCost = 0;
    } else {
      bloodCost = 0;
      boneCost = 0;
    }
  }

  public void newCard(String id) {
    editingId = id;
    displayName = "新卡";
    entityType = EntityType.WOLF;
    sigils.clear();
    power = 1;
    health = 1;
    bloodCost = 1;
    boneCost = 0;
  }

  public CardTemplate toTemplate(boolean builtin) {
    CardTemplate t = new CardTemplate(editingId);
    t.setDisplayName(displayName);
    t.setEntityType(entityType);
    t.setPower(power);
    t.setHealth(health);
    if (boneCost > 0) {
      t.setCostType(CostType.BONES);
      t.setCost(boneCost);
    } else if (bloodCost > 0) {
      t.setCostType(CostType.BLOOD);
      t.setCost(bloodCost);
    } else {
      t.setCostType(CostType.FREE);
      t.setCost(0);
    }
    t.setSigils(sigils);
    t.setBuiltin(builtin);
    return t;
  }

  public String editingId() {
    return editingId;
  }

  public String displayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public EntityType entityType() {
    return entityType;
  }

  public void setEntityType(EntityType entityType) {
    if (entityType != null && entityType.isAlive()) {
      this.entityType = entityType;
    }
  }

  public List<SigilId> sigils() {
    return List.copyOf(sigils);
  }

  public boolean toggleSigil(SigilId sigil) {
    if (sigils.contains(sigil)) {
      sigils.remove(sigil);
      return true;
    }
    if (sigils.size() >= com.github.mczju.mczjuscription.game.card.SigilRules.MAX_PER_CARD) {
      return false;
    }
    sigils.add(sigil);
    return true;
  }

  public int power() {
    return power;
  }

  public void addPower(int delta) {
    power = Math.max(0, power + delta);
  }

  public int health() {
    return health;
  }

  public void addHealth(int delta) {
    health = Math.max(1, health + delta);
  }

  public int bloodCost() {
    return bloodCost;
  }

  public int boneCost() {
    return boneCost;
  }

  public void addBloodCost(int delta) {
    bloodCost = Math.max(0, bloodCost + delta);
  }

  public void addBoneCost(int delta) {
    boneCost = Math.max(0, boneCost + delta);
  }

  public void setSigils(List<SigilId> next) {
    sigils.clear();
    sigils.addAll(com.github.mczju.mczjuscription.game.card.SigilRules.normalize(next));
  }

  public String sigilSummary() {
    if (sigils.isEmpty()) return "无";
    return SigilNamesJoin.join(
        com.github.mczju.mczjuscription.game.card.SigilRules.asSet(sigils));
  }

  private static final class SigilNamesJoin {
    static String join(java.util.Set<SigilId> sigils) {
      return com.github.mczju.mczjuscription.game.sigil.SigilNames.join(sigils);
    }
  }
}
