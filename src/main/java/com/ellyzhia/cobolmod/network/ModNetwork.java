package com.ellyzhia.cobolmod.network;

import com.ellyzhia.cobolmod.CobolMod;
import com.ellyzhia.cobolmod.raid.RaidDifficulty;
import com.ellyzhia.cobolmod.raid.RaidManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/** Enregistrement des paquets réseau et de leurs gestionnaires. */
@EventBusSubscriber(modid = CobolMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private ModNetwork() {}

    @SubscribeEvent
    static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToClient(RaidSyncPayload.TYPE, RaidSyncPayload.STREAM_CODEC, ModNetwork::handleSync);
        registrar.playToServer(RaidActionPayload.TYPE, RaidActionPayload.STREAM_CODEC, ModNetwork::handleAction);
    }

    /** Côté client : mise à jour de l'état local (délégué à une classe client-only). */
    private static void handleSync(RaidSyncPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> ClientPayloadHandler.handleSync(payload));
    }

    /** Côté serveur : exécution de l'action demandée, après vérifications dans RaidManager. */
    private static void handleAction(RaidActionPayload payload, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            String id = payload.typeId();
            switch (payload.action()) {
                case OPEN -> RaidManager.open(player, id, RaidDifficulty.byOrdinal(payload.value()), payload.flag());
                case JOIN -> RaidManager.join(player, id);
                case LEAVE -> RaidManager.leave(player);
                case REQUEST_TELEPORT -> RaidManager.requestTeleport(player, id);
                case READY_ANSWER -> RaidManager.answerReady(player, id, payload.value() == 1);
                case ADMIN_DIFFICULTY -> RaidManager.adminSetDifficulty(player, id, RaidDifficulty.byOrdinal(payload.value()));
                case ADMIN_PUBLIC -> RaidManager.adminSetPublic(player, id, payload.flag());
                case ADMIN_CLOSE -> RaidManager.adminClose(player, id);
            }
        });
    }
}
