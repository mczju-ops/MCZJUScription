package com.github.mczju.mczjuscription.shop;

import com.github.mczju.mczjuscription.MCZJUScriptionPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class ShopConfigStorage {

  private static File storageFile;
  private static ShopConfig config = new ShopConfig();

  private ShopConfigStorage() {}

  public static void init(JavaPlugin plugin) {
    storageFile = new File(plugin.getDataFolder(), "shop.yml");
    if (!plugin.getDataFolder().exists()) {
      plugin.getDataFolder().mkdirs();
    }
    reload();
    if (!storageFile.exists()) {
      seedDefaultsFromLegacyCatalog();
      save();
      plugin.getLogger().info("已生成默认 shop.yml（mob 卡刷新池）。");
    }
  }

  public static ShopConfig get() {
    return config;
  }

  public static void reload() {
    config = new ShopConfig();
    if (storageFile == null || !storageFile.exists()) {
      return;
    }
    YamlConfiguration yaml = YamlConfiguration.loadConfiguration(storageFile);
    loadPermanent(yaml.getConfigurationSection("permanent"));
    loadPool(yaml.getConfigurationSection("rotatingPool"), config.rotatingPool());
    // 旧版 premiumPool 合并进刷新池
    loadPool(yaml.getConfigurationSection("premiumPool"), config.rotatingPool());
    if (yaml.contains("extraSlotUnlockBlood")) {
      config.setExtraSlotUnlockBlood(yaml.getInt("extraSlotUnlockBlood"));
    }
  }

  public static void save() {
    if (storageFile == null) {
      return;
    }
    YamlConfiguration yaml = new YamlConfiguration();
    savePermanent(yaml.createSection("permanent"), config);
    savePool(yaml.createSection("rotatingPool"), config.rotatingPool());
    yaml.set("extraSlotUnlockBlood", config.extraSlotUnlockBlood());
    try {
      yaml.save(storageFile);
    } catch (IOException e) {
      MCZJUScriptionPlugin.getInstance()
          .getLogger()
          .log(Level.SEVERE, "无法保存 shop.yml", e);
    }
  }

  private static void seedDefaultsFromLegacyCatalog() {
    MobShopDefaults.seedRotatingPool(config);
    config.setPermanent(0, ShopOffer.of("mob_rabbit", 0));
    config.setPermanent(1, ShopOffer.of("mob_wolf", 3));
    config.setPermanent(2, ShopOffer.of("mob_bee", 2));
    config.setPermanent(3, ShopOffer.empty());
  }

  private static void loadPermanent(ConfigurationSection sec) {
    if (sec == null) {
      return;
    }
    for (int i = 0; i < ShopConfig.PERMANENT_SLOTS; i++) {
      ConfigurationSection slot = sec.getConfigurationSection("slot" + i);
      if (slot == null) {
        continue;
      }
      config.setPermanent(
          i,
          ShopOffer.of(slot.getString("cardId"), slot.getInt("priceBones", 0)));
    }
  }

  private static void savePermanent(ConfigurationSection sec, ShopConfig cfg) {
    for (int i = 0; i < ShopConfig.PERMANENT_SLOTS; i++) {
      ShopOffer offer = cfg.permanent(i);
      if (offer.isEmpty()) {
        continue;
      }
      ConfigurationSection slot = sec.createSection("slot" + i);
      slot.set("cardId", offer.templateId());
      slot.set("priceBones", offer.priceBones());
    }
  }

  private static void loadPool(ConfigurationSection sec, List<ShopPoolEntry> target) {
    if (sec == null) {
      return;
    }
    for (var raw : sec.getMapList("entries")) {
      Object cardId = raw.get("cardId");
      if (cardId == null) {
        continue;
      }
      int price =
          raw.get("priceBones") instanceof Number n ? n.intValue() : 2;
      int weight = raw.get("weight") instanceof Number n ? n.intValue() : 10;
      try {
        target.add(new ShopPoolEntry(cardId.toString(), price, weight));
      } catch (Exception ignored) {
      }
    }
  }

  private static void savePool(ConfigurationSection sec, List<ShopPoolEntry> pool) {
    List<java.util.Map<String, Object>> entries = new ArrayList<>();
    for (ShopPoolEntry entry : pool) {
      entries.add(
          java.util.Map.of(
              "cardId", entry.templateId(),
              "priceBones", entry.priceBones(),
              "weight", entry.weight()));
    }
    sec.set("entries", entries);
  }
}
