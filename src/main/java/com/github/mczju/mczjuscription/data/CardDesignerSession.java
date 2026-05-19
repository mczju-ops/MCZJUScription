package com.github.mczju.mczjuscription.data;

import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.card.CostType;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.EnumSet;
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
  private CostType costType = CostType.BLOOD;
  private int cost = 1;

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
    costType = template.costType();
    cost = template.cost();
  }

  public void newCard(String id) {
    editingId = id;
    displayName = "新卡";
    entityType = EntityType.WOLF;
    sigils.clear();
    power = 1;
    health = 1;
    costType = CostType.BLOOD;
    cost = 1;
  }

  public CardTemplate toTemplate(boolean builtin) {
    CardTemplate t = new CardTemplate(editingId);
    t.setDisplayName(displayName);
    t.setEntityType(entityType);
    t.setPower(power);
    t.setHealth(health);
    t.setCostType(costType);
    t.setCost(cost);
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

  public void cycleEntity() {
    EntityType[] pool = {
      EntityType.RABBIT, EntityType.WOLF, EntityType.BEE, EntityType.CHICKEN,
      EntityType.CAT, EntityType.FROG, EntityType.BAT, EntityType.SILVERFISH,
      EntityType.IRON_GOLEM, EntityType.ZOMBIE, EntityType.SLIME
    };
    int idx = 0;
    for (int i = 0; i < pool.length; i++) {
      if (pool[i] == entityType) {
        idx = (i + 1) % pool.length;
        break;
      }
    }
    entityType = pool[idx];
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

  public CostType costType() {
    return costType;
  }

  public void cycleCostType() {
    costType =
        switch (costType) {
          case FREE -> CostType.BLOOD;
          case BLOOD -> CostType.BONES;
          case BONES -> CostType.FREE;
        };
  }

  public int cost() {
    return cost;
  }

  public void addCost(int delta) {
    cost = Math.max(0, cost + delta);
  }

  public String sigilSummary() {
    if (sigils.isEmpty()) return "无";
    return SigilNamesJoin.join(EnumSet.copyOf(sigils));
  }

  private static final class SigilNamesJoin {
    static String join(java.util.Set<SigilId> sigils) {
      return com.github.mczju.mczjuscription.game.sigil.SigilNames.join(sigils);
    }
  }
}
