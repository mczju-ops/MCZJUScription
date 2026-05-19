package com.github.mczju.mczjuscription.game.sigil;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/** 无形之物：可随机到的印记（不含自身与未实现项）。 */
public final class RandomSigilPool {

  private static final List<SigilId> POOL =
      List.of(
          SigilId.AIR_STRIKE,
          SigilId.STINKY,
          SigilId.SPIKY_ARMOR,
          SigilId.BONE_ROYALTY,
          SigilId.TOUCH_OF_DEATH,
          SigilId.RABBIT_HOLE,
          SigilId.BREEDING,
          SigilId.FLEDGLING);

  private RandomSigilPool() {}

  public static SigilId roll() {
    return POOL.get(ThreadLocalRandom.current().nextInt(POOL.size()));
  }
}
