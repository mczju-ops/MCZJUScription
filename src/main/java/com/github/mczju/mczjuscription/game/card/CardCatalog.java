package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;

/** 内置 + 自定义卡牌模板库。 */
public final class CardCatalog {

  private static final Map<String, CardTemplate> TEMPLATES = new ConcurrentHashMap<>();
  private static File storageFile;

  private CardCatalog() {}

  public static void init(MCZJUScriptionPlugin plugin) {
    storageFile = new File(plugin.getDataFolder(), "cards.yml");
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdirs();
    }
    if (!storageFile.exists()) {
      plugin.saveResource("cards.yml", false);
    }
    reload();
  }

  /** 构牌 / 设计器可选：所有 mob_ 主卡（排除 token）。 */
  public static List<String> deckBuilderPool() {
    return TEMPLATES.values().stream()
        .map(CardTemplate::id)
        .filter(id -> id.startsWith("mob_"))
        .filter(id -> !id.contains("_small") && !id.equals("mob_fish_dried") && !id.equals("mob_vex"))
        .sorted()
        .toList();
  }

  public static void reload() {
    TEMPLATES.clear();
    for (CardId id : CardId.values()) {
      CardTemplate t = CardTemplate.fromDefinition(id, CardRegistry.get(id));
      TEMPLATES.put(id.name(), t);
    }
    loadOverrides();
  }

  private static void loadOverrides() {
    if (storageFile == null || !storageFile.exists()) return;
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
    var section = yaml.getConfigurationSection("cards");
    if (section == null) return;
    for (String key : section.getKeys(false)) {
      try {
        TEMPLATES.put(key, deserialize(key, section.getConfigurationSection(key)));
      } catch (Exception ex) {
        MCZJUScriptionPlugin.getInstance()
            .getLogger()
            .log(Level.WARNING, "跳过无效卡牌配置: " + key, ex);
      }
    }
  }

  public static void save(CardTemplate template) {
    template.setSigils(template.sigils());
    TEMPLATES.put(template.id(), template);
    persist();
  }

  public static void saveRuntime(CardTemplate template) {
    template.setBuiltin(false);
    TEMPLATES.put(template.id(), template);
  }

  public static String newCustomId() {
    return "custom_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
  }

  public static CardTemplate require(String id) {
    CardTemplate t = TEMPLATES.get(id);
    if (t == null) {
      throw new IllegalArgumentException("unknown card template: " + id);
    }
    return t;
  }

  public static CardTemplate get(String id) {
    return TEMPLATES.get(id);
  }

  public static CardTemplate require(CardId id) {
    return require(id.name());
  }

  public static Collection<CardTemplate> all() {
    return Collections.unmodifiableCollection(TEMPLATES.values());
  }

  public static List<CardTemplate> sortedForEditor() {
    List<CardTemplate> list = new ArrayList<>(TEMPLATES.values());
    list.sort(
        (a, b) -> {
          if (a.isBuiltin() != b.isBuiltin()) return a.isBuiltin() ? -1 : 1;
          return a.displayName().compareTo(b.displayName());
        });
    return list;
  }

  public static boolean exists(String id) {
    return TEMPLATES.containsKey(id);
  }

  /** 是否允许从设计器删除（自定义卡，或已写入 cards.yml 的内置覆盖）。 */
  public static boolean canDelete(String id) {
    CardTemplate t = TEMPLATES.get(id);
    if (t == null) {
      return false;
    }
    if (!isEnumBuiltinId(id)) {
      return true;
    }
    return isOverriddenBuiltin(t) || isStoredInYaml(id);
  }

  /**
   * 删除卡牌：移除自定义卡；对已覆盖的内置卡恢复为 {@link CardRegistry} 默认值。
   *
   * @return 操作结果
   */
  public static DeleteResult deleteCard(String id) {
    CardTemplate current = TEMPLATES.get(id);
    if (current == null) {
      return DeleteResult.NOT_FOUND;
    }
    if (!isEnumBuiltinId(id)) {
      TEMPLATES.remove(id);
      persist();
      return DeleteResult.REMOVED;
    }
    if (!isOverriddenBuiltin(current) && !isStoredInYaml(id)) {
      return DeleteResult.BUILTIN_PROTECTED;
    }
    CardId builtin = CardId.valueOf(id);
    TEMPLATES.put(id, CardTemplate.fromDefinition(builtin, CardRegistry.get(builtin)));
    persist();
    return DeleteResult.RESTORED_DEFAULT;
  }

  private static boolean isEnumBuiltinId(String id) {
    try {
      CardId.valueOf(id);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  private static boolean isStoredInYaml(String id) {
    if (storageFile == null || !storageFile.exists()) {
      return false;
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
    return yaml.isConfigurationSection("cards." + id);
  }

  public enum DeleteResult {
    /** 自定义卡已从库与 cards.yml 移除 */
    REMOVED,
    /** 内置卡覆盖已撤销，恢复默认定义 */
    RESTORED_DEFAULT,
    /** ID 不存在 */
    NOT_FOUND,
    /** 核心内置卡且无自定义覆盖，不可删 */
    BUILTIN_PROTECTED
  }

  private static void persist() {
    if (storageFile == null) return;
    YamlConfiguration out = new YamlConfiguration();
    for (CardTemplate t : TEMPLATES.values()) {
      if (t.isBuiltin() && !isOverriddenBuiltin(t)) continue;
      serialize(out.createSection("cards." + t.id()), t);
    }
    try {
      out.save(storageFile);
    } catch (IOException e) {
      MCZJUScriptionPlugin.getInstance()
          .getLogger()
          .log(Level.SEVERE, "无法保存 cards.yml", e);
    }
  }

  private static boolean isOverriddenBuiltin(CardTemplate t) {
    try {
      CardDefinition orig = CardRegistry.get(CardId.valueOf(t.id()));
      CardTemplate base = CardTemplate.fromDefinition(CardId.valueOf(t.id()), orig);
      return !base.displayName().equals(t.displayName())
          || base.power() != t.power()
          || base.health() != t.health()
          || !base.sigils().equals(t.sigils());
    } catch (Exception e) {
      return true;
    }
  }

  private static void serialize(org.bukkit.configuration.ConfigurationSection sec, CardTemplate t) {
    sec.set("displayName", t.displayName());
    sec.set("entityType", t.entityType().name());
    if (t.mountEntityType() != null) {
      sec.set("mountEntityType", t.mountEntityType().name());
    }
    sec.set("power", t.power());
    sec.set("health", t.health());
    sec.set("costType", t.costType().name());
    sec.set("cost", t.cost());
    sec.set("sacrificeValue", t.sacrificeValue());
    sec.set(
        "sigils",
        t.sigils().stream().map(Enum::name).toList());
    sec.set("builtin", t.isBuiltin());
    if (t.evolvesTo() != null && !t.evolvesTo().isBlank()) {
      sec.set("evolvesTo", t.evolvesTo());
    }
  }

  private static CardTemplate deserialize(String id, org.bukkit.configuration.ConfigurationSection sec) {
    if (sec == null) throw new IllegalArgumentException("empty section");
    CardTemplate t = new CardTemplate(id);
    t.setDisplayName(sec.getString("displayName", id));
    t.setEntityType(EntityType.valueOf(sec.getString("entityType", "PIG")));
    if (sec.contains("mountEntityType")) {
      t.setMountEntityType(EntityType.valueOf(sec.getString("mountEntityType")));
    }
    t.setPower(sec.getInt("power", 0));
    t.setHealth(sec.getInt("health", 1));
    t.setCostType(CostType.valueOf(sec.getString("costType", "BLOOD")));
    t.setCost(sec.getInt("cost", 1));
    t.setSacrificeValue(sec.getInt("sacrificeValue", 1));
    List<SigilId> sigils = new ArrayList<>();
    for (String name : sec.getStringList("sigils")) {
      sigils.add(SigilId.valueOf(name));
    }
    t.setSigils(sigils);
    t.setBuiltin(sec.getBoolean("builtin", false));
    t.setEvolvesTo(sec.getString("evolvesTo", null));
    return t;
  }
}
