package com.github.mczju.mczjuscription.game.sigil;

@FunctionalInterface
public interface SigilHandler {
    void apply(SigilContext context);
}
