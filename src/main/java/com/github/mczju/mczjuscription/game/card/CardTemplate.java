package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import com.github.mczju.mczjuscription.game.sigil.SigilNames;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/** 可序列化的卡牌模板（内置 + 自定义 + 肉鸽融合）。 */
public final class CardTemplate {

  private final String id;
  private String displayName;
  private EntityType entityType;
  private EntityType mountEntityType;
  private int power;
  private int health;
  private CostType costType;
  private int cost;
  private int sacrificeValue;
  private List<SigilId> sigils;
  private String evolvesTo;
  private boolean builtin;

  public CardTemplate(String id) {
    this.id = id;
    this.displayName = id;
    this.entityType = EntityType.PIG;
    this.power = 0;
    this.health = 1;
    this.costType = CostType.BLOOD;
    this.cost = 1;
    this.sacrificeValue = 1;
    this.sigils = List.of();
    this.builtin = false;
  }

  public static CardTemplate fromDefinition(CardId cardId, CardDefinition def) {
    CardTemplate t = new CardTemplate(cardId.name());
    t.displayName = def.displayName();
    t.entityType = def.entityType();
    t.power = def.power();
    t.health = def.health();
    t.costType = def.costType();
    t.cost = def.cost();
    t.sacrificeValue = def.sacrificeValue();
    t.sigils = new ArrayList<>(def.sigils());
    t.builtin = true;
    return t;
  }

  public CardTemplate copy(String newId) {
    CardTemplate c = new CardTemplate(newId);
    c.displayName = displayName;
    c.entityType = entityType;
    c.mountEntityType = mountEntityType;
    c.power = power;
    c.health = health;
    c.costType = costType;
    c.cost = cost;
    c.sacrificeValue = sacrificeValue;
    c.sigils = new ArrayList<>(sigils);
    c.evolvesTo = evolvesTo;
    c.builtin = false;
    return c;
  }

  public String evolvesTo() {
    return evolvesTo;
  }

  public void setEvolvesTo(String evolvesTo) {
    this.evolvesTo = evolvesTo;
  }

  public String id() {
    return id;
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
    this.entityType = entityType;
  }

  public EntityType mountEntityType() {
    return mountEntityType;
  }

  public void setMountEntityType(EntityType mountEntityType) {
    this.mountEntityType = mountEntityType;
  }

  public boolean hasMount() {
    return mountEntityType != null;
  }

  public int power() {
    return power;
  }

  public void setPower(int power) {
    this.power = Math.max(0, power);
  }

  public int health() {
    return health;
  }

  public void setHealth(int health) {
    this.health = Math.max(1, health);
  }

  public CostType costType() {
    return costType;
  }

  public void setCostType(CostType costType) {
    this.costType = costType;
  }

  public int cost() {
    return cost;
  }

  public void setCost(int cost) {
    this.cost = Math.max(0, cost);
  }

  public int sacrificeValue() {
    return sacrificeValue;
  }

  public void setSacrificeValue(int sacrificeValue) {
    this.sacrificeValue = Math.max(0, sacrificeValue);
  }

  public List<SigilId> sigils() {
    return Collections.unmodifiableList(sigils);
  }

  public void setSigils(List<SigilId> sigils) {
    this.sigils = new ArrayList<>(SigilRules.normalize(sigils));
  }

  public boolean hasSigil(SigilId sigil) {
    return sigils.contains(sigil);
  }

  public boolean isBuiltin() {
    return builtin;
  }

  public void setBuiltin(boolean builtin) {
    this.builtin = builtin;
  }

  public Material spawnEggMaterial() {
    Material egg = Material.getMaterial(entityType.name() + "_SPAWN_EGG");
    return egg != null ? egg : Material.EGG;
  }

  public String sigilsDisplay() {
    if (sigils.isEmpty()) return "无";
    return SigilNames.join(EnumSet.copyOf(sigils));
  }

  /** 供仍使用 {@link CardDefinition} 的路径（逐步淘汰）。 */
  public CardDefinition toDefinition() {
    CardId enumId;
    try {
      enumId = CardId.valueOf(id);
    } catch (IllegalArgumentException e) {
      enumId = CardId.RABBIT;
    }
    var b =
        CardDefinition.builder(enumId)
            .displayName(displayName)
            .entity(entityType)
            .stats(power, health)
            .sacrificeValue(sacrificeValue);
    if (costType == CostType.FREE) {
      b.free();
    } else {
      b.cost(costType, cost);
    }
    for (SigilId s : sigils) {
      b.sigil(s);
    }
    return b.build();
  }
}
