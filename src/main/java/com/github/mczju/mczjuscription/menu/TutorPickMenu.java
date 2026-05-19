package com.github.mczju.mczjuscription.menu;

import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.card.CardTemplate;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import com.github.mczjuops.mczjugamecore.menu.Menu;
import com.github.mczjuops.mczjugamecore.utils.ItemBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Range;

/** 囤积狂：从主牌组顶展示最多 3 张，选 1 张入手。 */
public final class TutorPickMenu extends Menu {

  private final InscriptionMatch match;
  private final MatchSide side;
  private final List<String> revealed = new ArrayList<>();

  public TutorPickMenu(Player player, InscriptionMatch match, MatchSide side) {
    super(player, match, side);
    this.match = match;
    this.side = side;
    Deque<String> deck = match.participant(side).mainDeck();
    for (int i = 0; i < 3 && !deck.isEmpty(); i++) {
      revealed.add(deck.pollFirst());
    }
  }

  @Override
  protected void setup() {
    inventory.clear();
    if (revealed.isEmpty()) {
      setSlot(
          4,
          ItemBuilder.of(Material.BARRIER)
              .customName("<red>牌组已空")
              .build(),
          (p, e) -> p.player().closeInventory());
      return;
    }

    int[] slots = {11, 13, 15};
    for (int i = 0; i < revealed.size() && i < slots.length; i++) {
      String id = revealed.get(i);
      CardTemplate def = CardCatalog.require(id);
      int index = i;
      setSlot(
          slots[i],
          ItemBuilder.of(def.spawnEggMaterial())
              .customName("<green>" + def.displayName())
              .lore(List.of("<gray>点击选入手中"))
              .build(),
          (p, e) -> pick(index));
    }

    setSlot(
        22,
        ItemBuilder.of(Material.BARRIER)
            .customName("<red>取消")
            .lore(List.of("<gray>未选的牌放回牌组顶"))
            .build(),
        (p, e) -> {
          returnUnpickedToDeck();
          p.player().closeInventory();
        });
  }

  private void pick(int index) {
    if (index < 0 || index >= revealed.size()) return;
    String chosen = revealed.remove(index);
    returnUnpickedToDeck();
    match.grantCardToHand(side, chosen);
    player.player().closeInventory();
  }

  private void returnUnpickedToDeck() {
    Deque<String> deck = match.participant(side).mainDeck();
    List<String> rest = new ArrayList<>(revealed);
    Collections.reverse(rest);
    for (String id : rest) {
      deck.offerFirst(id);
    }
    revealed.clear();
  }

  @Override
  protected String getTitle() {
    return "囤积狂 · 选一张";
  }

  @Override
  protected @Range(from = 1, to = 6) int getRows() {
    return 3;
  }

  @Override
  protected String getPermission() {
    return "inscription.play";
  }
}
