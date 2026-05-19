package com.github.mczju.mczjuscription.lobby;

import com.github.mczju.mczjuscription.game.session.PlayVariant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.ArmorStand;

/** 大厅内存状态：占座、待确认双人开局。 */
public final class HubSession {

    private static final Map<UUID, SeatBinding> SEATS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingDuel> PENDING_DUELS = new ConcurrentHashMap<>();

    private HubSession() {}

    public static boolean tryOccupy(UUID playerId, String seatKey, SeatKind kind) {
        SeatBinding existing = SEATS.get(playerId);
        if (existing != null && existing.seatKey().equals(seatKey)) {
            return true;
        }
        for (SeatBinding binding : SEATS.values()) {
            if (binding.seatKey().equals(seatKey) && !binding.playerId().equals(playerId)) {
                return false;
            }
        }
        SEATS.put(playerId, new SeatBinding(playerId, seatKey, kind));
        return true;
    }

    public static void release(UUID playerId) {
        SEATS.remove(playerId);
        PENDING_DUELS.remove(playerId);
    }

    public static SeatBinding seatOf(UUID playerId) {
        return SEATS.get(playerId);
    }

    public static void bindMount(UUID playerId, ArmorStand stand) {
        SeatBinding binding = SEATS.get(playerId);
        if (binding == null) {
            return;
        }
        SEATS.put(playerId, binding.withMount(stand.getUniqueId()));
    }

    public static UUID mountOf(UUID playerId) {
        SeatBinding binding = SEATS.get(playerId);
        return binding != null ? binding.mountId() : null;
    }

    public static void setPendingDuel(UUID leaderId, PlayVariant variant) {
        PENDING_DUELS.put(leaderId, new PendingDuel(variant, false));
    }

    public static PendingDuel pendingDuel(UUID leaderId) {
        return PENDING_DUELS.get(leaderId);
    }

    public static void markDuelConfirmed(UUID leaderId) {
        PendingDuel pending = PENDING_DUELS.get(leaderId);
        if (pending != null) {
            PENDING_DUELS.put(leaderId, pending.withConfirmed(true));
        }
    }

    public enum SeatKind {
        SOLO,
        DUEL
    }

    public record SeatBinding(UUID playerId, String seatKey, SeatKind kind, UUID mountId) {
        SeatBinding(UUID playerId, String seatKey, SeatKind kind) {
            this(playerId, seatKey, kind, null);
        }

        SeatBinding withMount(UUID mount) {
            return new SeatBinding(playerId, seatKey, kind, mount);
        }
    }

    public record PendingDuel(PlayVariant variant, boolean curseConfirmed) {
        PendingDuel withConfirmed(boolean confirmed) {
            return new PendingDuel(variant, confirmed);
        }
    }
}
