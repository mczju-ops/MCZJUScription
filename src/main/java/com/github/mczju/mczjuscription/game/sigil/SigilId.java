package com.github.mczju.mczjuscription.game.sigil;

public enum SigilId {
    RABBIT_HOLE,
    SPIKY_ARMOR,
    AIR_STRIKE,
    WATER_STRIKE,
    HIGH_JUMP,
    STINKY,
    ROCK_BODY,
    TOUCH_OF_DEATH,
    BONE_ROYALTY,
    ETERNAL_LIFE,
    QUALITY_SACRIFICE,
    /** @deprecated 占位，后续若实现「冰封」机制可复用 */
    ICY_ENTOMB,
    LEADER_POWER,
    RUSH_LEFT,
    RUSH_RIGHT,
    /** 繁殖：本回合战斗开始时已在场，且完整经历攻击阶段后仍存活，回合末获得一张同卡召唤蛋。 */
    BREEDING,
    /** 幼雏：在场上完整经历一回合战斗后成长（原生有专属形态，否则变为长老+1/+1）。 */
    FLEDGLING
}
