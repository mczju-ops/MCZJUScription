package com.github.mczju.mczjuscription.arena;

import com.github.mczju.mczjuscription.entity.CreatureEntityService;
import com.github.mczju.mczjuscription.game.board.SlotOwner;
import com.github.mczju.mczjuscription.game.card.BoardCreature;
import com.github.mczju.mczjuscription.game.card.CardCatalog;
import com.github.mczju.mczjuscription.game.match.InscriptionMatch;
import com.github.mczju.mczjuscription.game.match.MatchSide;
import java.util.List;
import org.bukkit.Location;

/**
 * 顶栏 2×11 预览条：内嵌 1.8×10.8 区域居中，召唤「再下一波」造物实体（含头顶信息），不绑定棋盘槽位。
 * <p>
 * 多只时沿内嵌长条长轴间隔约 3 格居中排布（超出 10.8 时自动压缩间距）。
 */
public final class ArenaPreviewStripDisplay {

    private static final double ICON_SPACING = 3.0;
    private static final double INNER_LENGTH = ArenaGridParser.INNER_STRIP_PEDAL_LENGTH;

    private ArenaPreviewStripDisplay() {}

    public static void sync(
            List<BoardCreature> sink,
            InscriptionMatch match,
            BattleArena arena,
            Location stripCenter,
            ArenaOrientation orientation
    ) {
        clear(sink);
        if (match == null || arena == null || stripCenter == null) {
            return;
        }
        List<String> templateIds = match.enemyStripPreviewTemplates();
        if (templateIds == null || templateIds.isEmpty()) {
            return;
        }
        Location base = ArenaCoordinates.toStripCenter(stripCenter);
        if (base.getWorld() == null) {
            return;
        }

        int count = templateIds.size();
        double spacing = ICON_SPACING;
        double totalSpan = (count - 1) * spacing;
        double maxSpan = INNER_LENGTH - 0.2;
        if (count > 1 && totalSpan > maxSpan) {
            spacing = maxSpan / (count - 1);
            totalSpan = maxSpan;
        }
        double start = -totalSpan / 2.0;
        boolean alongZ = ArenaCoordinates.stripLengthAlongWorldZ(orientation);

        for (int i = 0; i < count; i++) {
            String templateId = templateIds.get(i);
            if (templateId == null || !CardCatalog.exists(templateId)) {
                continue;
            }
            double offset = start + i * spacing;
            Location at = base.clone();
            if (alongZ) {
                at.add(0, 0, offset);
            } else {
                at.add(offset, 0, 0);
            }
            Location faceTarget = ArenaFacing.facingTarget(arena, SlotOwner.ENEMY_PREVIEW, Math.min(i, 3));
            Location spawnAt = ArenaFacing.withYawToward(at, faceTarget);

            BoardCreature creature = new BoardCreature(templateId, MatchSide.ENEMY);
            CreatureEntityService.spawn(creature, spawnAt);
            sink.add(creature);
        }
    }

    public static void clear(List<BoardCreature> sink) {
        if (sink == null) {
            return;
        }
        for (BoardCreature creature : sink) {
            if (creature != null) {
                CreatureEntityService.despawn(creature);
            }
        }
        sink.clear();
    }
}
