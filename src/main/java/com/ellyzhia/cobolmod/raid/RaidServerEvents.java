package com.ellyzhia.cobolmod.raid;

import com.ellyzhia.cobolmod.CobolMod;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Événements serveur liés aux raids : connexion, déconnexion, tick, arrêt. */
@EventBusSubscriber(modid = CobolMod.MODID)
public final class RaidServerEvents {
    private RaidServerEvents() {}

    @SubscribeEvent
    static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaidManager.syncTo(player);
        }
    }

    @SubscribeEvent
    static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            RaidManager.leave(player);
        }
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        RaidManager.tick(event.getServer());
    }

    @SubscribeEvent
    static void onServerStopped(ServerStoppedEvent event) {
        RaidManager.clear();
    }
}
