package com.github.mczju.mczjuscription.game.match;

/**
 * 腐肉（本回合）、骨币与鱼干（可跨回合累积）。
 */
public final class Currency {

    private int blood;
    private int bones;
    private int fish;

    public int getBlood() {
        return blood;
    }

    public int getBones() {
        return bones;
    }

    public int getFish() {
        return fish;
    }

    public void addBlood(int amount) {
        blood = Math.max(0, blood + amount);
    }

    public void addBones(int amount) {
        bones = Math.max(0, bones + amount);
    }

    public void addFish(int amount) {
        fish = Math.max(0, fish + amount);
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

    public boolean trySpendFish(int cost) {
        if (fish < cost) return false;
        fish -= cost;
        return true;
    }

    public void clearBloodOnTurnEnd() {
        blood = 0;
    }
}
