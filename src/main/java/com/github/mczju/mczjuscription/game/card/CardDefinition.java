package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

public final class CardDefinition {

    private final String id;
    private final String displayName;
    private final EntityType entityType;
    private final int power;
    private final int health;
    private final CostType costType;
    private final int cost;
    private final int sacrificeValue;
    private final Set<SigilId> sigils;

    private CardDefinition(Builder builder) {
        this.id = builder.id;
        this.displayName = builder.displayName;
        this.entityType = builder.entityType;
        this.power = builder.power;
        this.health = builder.health;
        this.costType = builder.costType;
        this.cost = builder.cost;
        this.sacrificeValue = builder.sacrificeValue;
        this.sigils = builder.sigils.isEmpty()
                ? Set.of()
                : Collections.unmodifiableSet(EnumSet.copyOf(builder.sigils));
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public EntityType entityType() {
        return entityType;
    }

    public Material spawnEggMaterial() {
        Material egg = Material.getMaterial(entityType.name() + "_SPAWN_EGG");
        return egg != null ? egg : Material.EGG;
    }

    /** 力量：攻击时能造成的伤害 */
    public int power() {
        return power;
    }

    /** @deprecated 使用 {@link #power()} */
    public int attack() {
        return power;
    }

    public int health() {
        return health;
    }

    public CostType costType() {
        return costType;
    }

    public int cost() {
        return cost;
    }

    public int sacrificeValue() {
        return sacrificeValue;
    }

    public Set<SigilId> sigils() {
        return sigils;
    }

    public boolean hasSigil(SigilId sigil) {
        return sigils.contains(sigil);
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public static final class Builder {
        private final String id;
        private String displayName;
        private EntityType entityType = EntityType.PIG;
        private int power;
        private int health = 1;
        private CostType costType = CostType.BLOOD;
        private int cost = 1;
        private int sacrificeValue = 1;
        private final Set<SigilId> sigils = EnumSet.noneOf(SigilId.class);

        private Builder(String id) {
            this.id = id;
            this.displayName = id;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder entity(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        public Builder stats(int power, int health) {
            this.power = power;
            this.health = health;
            return this;
        }

        public Builder cost(CostType type, int amount) {
            this.costType = type;
            this.cost = amount;
            return this;
        }

        public Builder free() {
            return cost(CostType.FREE, 0);
        }

        public Builder sacrificeValue(int value) {
            this.sacrificeValue = value;
            return this;
        }

        public Builder sigil(SigilId sigil) {
            sigils.add(sigil);
            return this;
        }

        public CardDefinition build() {
            return new CardDefinition(this);
        }
    }
}
