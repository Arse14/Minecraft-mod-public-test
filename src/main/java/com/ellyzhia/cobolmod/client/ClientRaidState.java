package com.ellyzhia.cobolmod.client;

import com.ellyzhia.cobolmod.raid.RaidInstance;
import net.minecraft.client.Minecraft;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Copie côté client de l'état des raids, mise à jour par {@code RaidSyncPayload}. */
public final class ClientRaidState {
    private static final Map<String, RaidInstance> INSTANCES = new LinkedHashMap<>();

    private ClientRaidState() {}

    public static void update(List<RaidInstance> raids) {
        INSTANCES.clear();
        for (RaidInstance r : raids) INSTANCES.put(r.typeId(), r);

        // Si l'écran des raids est ouvert, on le rafraîchit ; si un ready check nous concerne, on l'ouvre
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof RaidScreen screen) {
            screen.onStateUpdated();
        } else if (mc.screen == null && mc.player != null) {
            pendingReadyCheck(mc.player.getUUID()).ifPresent(raid -> mc.setScreen(new ReadyCheckScreen(raid)));
        }
    }

    public static Optional<RaidInstance> get(String typeId) {
        return Optional.ofNullable(INSTANCES.get(typeId));
    }

    public static Optional<RaidInstance> raidOf(UUID player) {
        return INSTANCES.values().stream().filter(r -> r.isMember(player)).findFirst();
    }

    /** Le raid du joueur est en ready check et il n'a pas encore répondu. */
    public static Optional<RaidInstance> pendingReadyCheck(UUID player) {
        return raidOf(player).filter(r -> r.state() == RaidInstance.State.READY_CHECK && !r.isReady(player));
    }
}
