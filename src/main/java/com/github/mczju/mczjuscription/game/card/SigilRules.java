package com.github.mczju.mczjuscription.game.card;

import com.github.mczju.mczjuscription.game.sigil.SigilId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 印记规则：每张卡最多 {@link #MAX_PER_CARD} 个印记（含卡面自带）。 */
public final class SigilRules {

  public static final int MAX_PER_CARD = 3;

  private SigilRules() {}

  public static List<SigilId> normalize(List<SigilId> sigils) {
    LinkedHashSet<SigilId> set = new LinkedHashSet<>();
    for (SigilId s : sigils) {
      if (s == null) continue;
      set.add(s);
      if (set.size() >= MAX_PER_CARD) break;
    }
    return List.copyOf(set);
  }

  /** 将卡面印记列表转为集合；空列表安全返回空 EnumSet（不可对空集合 {@link EnumSet#copyOf}）。 */
  public static Set<SigilId> asSet(List<SigilId> sigils) {
    if (sigils == null || sigils.isEmpty()) {
      return EnumSet.noneOf(SigilId.class);
    }
    return EnumSet.copyOf(sigils);
  }

  public static boolean canAdd(Set<SigilId> current, SigilId add) {
    if (add == null) return false;
    if (current.contains(add)) return false;
    return current.size() < MAX_PER_CARD;
  }

  public static List<SigilId> merge(List<SigilId> a, List<SigilId> b) {
    List<SigilId> combined = new ArrayList<>();
    combined.addAll(a);
    combined.addAll(b);
    return normalize(combined);
  }

  public static int totalOnCreature(Set<SigilId> onCard, Set<SigilId> bonus) {
    Set<SigilId> all = EnumSet.noneOf(SigilId.class);
    all.addAll(onCard);
    all.addAll(bonus);
    return all.size();
  }
}
