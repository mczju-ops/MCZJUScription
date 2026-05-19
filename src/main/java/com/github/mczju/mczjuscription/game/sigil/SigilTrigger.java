package com.github.mczju.mczjuscription.game.sigil;

public enum SigilTrigger {
  ON_DRAW,
  ON_PLAY,
  ON_TURN_START,
  PRE_COMBAT,
  ON_TARGETED,
  ON_ATTACKED,
  ON_COMBAT_ATTACK,
  ON_DEATH,
  ON_SACRIFICE,
  ON_TURN_END,
  AURA,
  /** 由战斗管线或专用模块直接调用，不通过 {@link SigilRegistry#fire} 默认映射 */
  SPECIAL,
}
