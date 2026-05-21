package com.github.mczju.mczjuscription.shop;

/** 随机池条目：卡牌、骨币价、抽取权重。 */
public record ShopPoolEntry(String templateId, int priceBones, int weight) {

  public ShopPoolEntry {
    if (templateId == null || templateId.isBlank()) {
      throw new IllegalArgumentException("templateId required");
    }
    priceBones = Math.max(0, priceBones);
    weight = Math.max(1, weight);
  }

  public ShopOffer toOffer() {
    return ShopOffer.of(templateId, priceBones);
  }
}
