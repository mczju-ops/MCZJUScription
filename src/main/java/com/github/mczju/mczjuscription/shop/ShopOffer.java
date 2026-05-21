package com.github.mczju.mczjuscription.shop;

import org.jetbrains.annotations.Nullable;

/** 商店货架上的一张可购卡（固定或当回合刷新）。 */
public record ShopOffer(String templateId, int priceBones) {

  public static ShopOffer of(String templateId, int priceBones) {
    if (templateId == null || templateId.isBlank()) {
      return empty();
    }
    return new ShopOffer(templateId, Math.max(0, priceBones));
  }

  public static ShopOffer empty() {
    return new ShopOffer(null, 0);
  }

  public boolean isEmpty() {
    return templateId == null || templateId.isBlank();
  }

  public @Nullable String templateId() {
    return templateId;
  }
}
