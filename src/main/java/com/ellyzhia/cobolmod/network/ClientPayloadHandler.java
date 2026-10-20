package com.ellyzhia.cobolmod.network;

import com.ellyzhia.cobolmod.client.ClientRaidState;

/**
 * Gestion des paquets côté client, isolée pour que ModNetwork ne référence
 * jamais de classe client (ce qui planterait un serveur dédié au chargement).
 * Cette classe n'est chargée par la JVM que lorsqu'un paquet client arrive.
 */
final class ClientPayloadHandler {
    private ClientPayloadHandler() {}

    static void handleSync(RaidSyncPayload payload) {
        ClientRaidState.update(payload.raids());
    }
}
