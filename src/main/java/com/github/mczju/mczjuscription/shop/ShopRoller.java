package com.github.mczju.mczjuscription.shop;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ShopRoller {

  private ShopRoller() {}

  public static List<ShopOffer> roll(List<ShopPoolEntry> pool, int count) {
    List<ShopOffer> result = new ArrayList<>();
    if (pool == null || pool.isEmpty() || count <= 0) {
      return result;
    }
    List<ShopPoolEntry> remaining = new ArrayList<>(pool);
    for (int i = 0; i < count && !remaining.isEmpty(); i++) {
      ShopPoolEntry picked = weightedPick(remaining);
      result.add(picked.toOffer());
      remaining.remove(picked);
    }
    return result;
  }

  public static ShopOffer rollOne(List<ShopPoolEntry> pool) {
    List<ShopOffer> one = roll(pool, 1);
    return one.isEmpty() ? ShopOffer.empty() : one.getFirst();
  }

  private static ShopPoolEntry weightedPick(List<ShopPoolEntry> pool) {
    int total = 0;
    for (ShopPoolEntry entry : pool) {
      total += entry.weight();
    }
    int roll = ThreadLocalRandom.current().nextInt(total);
    int acc = 0;
    for (ShopPoolEntry entry : pool) {
      acc += entry.weight();
      if (roll < acc) {
        return entry;
      }
    }
    return pool.getLast();
  }
}
