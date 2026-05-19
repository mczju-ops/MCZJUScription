package com.github.mczju.mczjuscription.game.match;

/**
 * 血量（本回合）与骨币（可跨回合累积）。
 */
public final class Currency {

    private int blood;
    private int bones;

    public int getBlood() {
        return blood;
    }

    public int getBones() {
        return bones;
    }

    public void addBlood(int amount) {
        blood = Math.max(0, blood + amount);
    }

    public void addBones(int amount) {
        bones = Math.max(0, bones + amount);
    }

    public boolean trySpendBlood(int cost) {
        if (blood < cost) return false;
        blood -= cost;
        return true;
    }

    public boolean trySpendBones(int cost) {
        if (bones < cost) return false;
        bones -= cost;
        return true;
    }

    public void clearBloodOnTurnEnd() {
        blood = 0;
    }
}
