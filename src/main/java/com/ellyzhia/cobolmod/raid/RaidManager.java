package com.ellyzhia.cobolmod.raid;

import com.ellyzhia.cobolmod.CobolMod;
import com.ellyzhia.cobolmod.network.RaidSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * État des raids côté serveur : une instance au plus par type de raid.
 * Toutes les règles de jeu sont ici ; le client ne fait qu'envoyer des demandes.
 * Après chaque changement, l'état complet est renvoyé à tous les joueurs.
 */
public final class RaidManager {
    private static final Map<String, RaidInstance> INSTANCES = new LinkedHashMap<>();
    /** Durée d'un ready check avant annulation automatique (ticks). */
    private static final int READY_CHECK_TIMEOUT_TICKS = 20 * 30;
    private static final Map<String, Integer> READY_CHECK_DEADLINE = new LinkedHashMap<>();

    private RaidManager() {}

    private static MutableComponent prefix() {
        return Component.literal("[Raids] ").withStyle(ChatFormatting.RED);
    }

    private static void msg(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(prefix().append(Component.translatable(key, args)));
    }

    private static void broadcast(MinecraftServer server, RaidInstance raid, String key, Object... args) {
        for (RaidInstance.RaidMember m : raid.members()) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.uuid());
            if (p != null) msg(p, key, args);
        }
    }

    // -------------------------------------------------------------- lecture

    public static List<RaidInstance> all() {
        return new ArrayList<>(INSTANCES.values());
    }

    public static Optional<RaidInstance> get(String typeId) {
        return Optional.ofNullable(INSTANCES.get(typeId));
    }

    public static Optional<RaidInstance> raidOf(UUID player) {
        return INSTANCES.values().stream().filter(r -> r.isMember(player)).findFirst();
    }

    // ----------------------------------------------------------- synchro

    /** Envoie l'état complet à tout le monde. */
    public static void syncAll(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(new RaidSyncPayload(all()));
    }

    /** Envoie l'état complet à un seul joueur (à sa connexion par exemple). */
    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RaidSyncPayload(all()));
    }

    public static void clear() {
        INSTANCES.clear();
        READY_CHECK_DEADLINE.clear();
    }

    // ----------------------------------------------------------- actions

    /** Ouvre une instance du raid. Un joueur ne peut être que dans un raid à la fois. */
    public static void open(ServerPlayer player, String typeId, RaidDifficulty difficulty, boolean isPublic) {
        Optional<RaidType> type = RaidTypes.byId(typeId);
        if (type.isEmpty()) return;
        if (INSTANCES.containsKey(typeId)) {
            msg(player, "raid.cobolmod.already_open", type.get().name());
            return;
        }
        if (raidOf(player.getUUID()).isPresent()) {
            msg(player, "raid.cobolmod.already_in_raid");
            return;
        }
        RaidInstance.RaidMember leader = new RaidInstance.RaidMember(player.getUUID(), player.getGameProfile().getName());
        INSTANCES.put(typeId, RaidInstance.open(type.get(), leader, difficulty, isPublic));
        CobolMod.LOGGER.info("[Raids] {} ouvre {} ({})", leader.name(), typeId, difficulty);
        msg(player, "raid.cobolmod.opened", type.get().name(), difficulty.label());
        syncAll(player.server);
    }

    public static void join(ServerPlayer player, String typeId) {
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null) {
            msg(player, "raid.cobolmod.not_open");
            return;
        }
        if (raid.isMember(player.getUUID())) return;
        if (raid.state() != RaidInstance.State.OPEN) {
            msg(player, "raid.cobolmod.not_joinable");
            return;
        }
        if (raid.isFull()) {
            msg(player, "raid.cobolmod.full");
            return;
        }
        if (!raid.isPublic() && !player.hasPermissions(2)) {
            msg(player, "raid.cobolmod.private");
            return;
        }
        if (raidOf(player.getUUID()).isPresent()) {
            msg(player, "raid.cobolmod.already_in_raid");
            return;
        }
        List<RaidInstance.RaidMember> members = new ArrayList<>(raid.members());
        members.add(new RaidInstance.RaidMember(player.getUUID(), player.getGameProfile().getName()));
        RaidInstance updated = raid.withMembers(members);
        INSTANCES.put(typeId, updated);
        broadcast(player.server, updated, "raid.cobolmod.player_joined", player.getGameProfile().getName(),
                members.size(), raid.maxPlayers());
        syncAll(player.server);
    }

    public static void leave(ServerPlayer player) {
        Optional<RaidInstance> opt = raidOf(player.getUUID());
        if (opt.isEmpty()) return;
        RaidInstance raid = opt.get();
        if (raid.isLeader(player.getUUID())) {
            // Le leader part : le raid est dissous
            INSTANCES.remove(raid.typeId());
            READY_CHECK_DEADLINE.remove(raid.typeId());
            broadcast(player.server, raid, "raid.cobolmod.disbanded");
        } else {
            List<RaidInstance.RaidMember> members = new ArrayList<>(raid.members());
            members.removeIf(m -> m.uuid().equals(player.getUUID()));
            List<UUID> ready = new ArrayList<>(raid.readyCheck());
            ready.remove(player.getUUID());
            RaidInstance updated = raid.withMembers(members).withState(raid.state(), ready);
            INSTANCES.put(raid.typeId(), updated);
            broadcast(player.server, updated, "raid.cobolmod.player_left", player.getGameProfile().getName());
        }
        syncAll(player.server);
    }

    /** Le leader demande la téléportation : lance un ready check auprès de toute l'équipe. */
    public static void requestTeleport(ServerPlayer player, String typeId) {
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null || !raid.isMember(player.getUUID())) return;
        if (!raid.isLeader(player.getUUID())) {
            msg(player, "raid.cobolmod.leader_only");
            return;
        }
        if (raid.state() != RaidInstance.State.OPEN) return;

        // Le leader est considéré prêt d'office
        RaidInstance updated = raid.withState(RaidInstance.State.READY_CHECK, List.of(player.getUUID()));
        INSTANCES.put(typeId, updated);
        READY_CHECK_DEADLINE.put(typeId, player.server.getTickCount() + READY_CHECK_TIMEOUT_TICKS);
        broadcast(player.server, updated, "raid.cobolmod.ready_check_started", player.getGameProfile().getName());
        syncAll(player.server);
        checkAllReady(player.server, typeId);
    }

    /** Réponse d'un membre au ready check. */
    public static void answerReady(ServerPlayer player, String typeId, boolean ready) {
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null || !raid.isMember(player.getUUID())) return;
        if (raid.state() != RaidInstance.State.READY_CHECK) return;

        if (!ready) {
            cancelReadyCheck(player.server, raid, player.getGameProfile().getName());
            return;
        }
        List<UUID> readyList = new ArrayList<>(raid.readyCheck());
        if (!readyList.contains(player.getUUID())) readyList.add(player.getUUID());
        RaidInstance updated = raid.withState(RaidInstance.State.READY_CHECK, readyList);
        INSTANCES.put(typeId, updated);
        broadcast(player.server, updated, "raid.cobolmod.player_ready", player.getGameProfile().getName(),
                readyList.size(), raid.members().size());
        syncAll(player.server);
        checkAllReady(player.server, typeId);
    }

    private static void cancelReadyCheck(MinecraftServer server, RaidInstance raid, String who) {
        RaidInstance updated = raid.withState(RaidInstance.State.OPEN, List.of());
        INSTANCES.put(raid.typeId(), updated);
        READY_CHECK_DEADLINE.remove(raid.typeId());
        broadcast(server, updated, "raid.cobolmod.ready_check_cancelled", who);
        syncAll(server);
    }

    private static void checkAllReady(MinecraftServer server, String typeId) {
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null || raid.state() != RaidInstance.State.READY_CHECK || !raid.everyoneReady()) return;
        teleportTeam(server, raid);
    }

    /** Tout le monde est prêt : téléportation de l'équipe à l'arène et passage en IN_PROGRESS. */
    private static void teleportTeam(MinecraftServer server, RaidInstance raid) {
        RaidType type = RaidTypes.byId(raid.typeId()).orElseThrow();
        ServerLevel level = server.overworld();
        for (RaidInstance.RaidMember m : raid.members()) {
            ServerPlayer p = server.getPlayerList().getPlayer(m.uuid());
            if (p == null) continue;
            // Petite dispersion autour du point d'arène pour ne pas empiler les joueurs
            double dx = (raid.members().indexOf(m) % 2) * 2.0 - 1.0;
            double dz = (raid.members().indexOf(m) / 2) * 2.0 - 1.0;
            p.teleportTo(level, type.arenaX() + 0.5 + dx, type.arenaY(), type.arenaZ() + 0.5 + dz,
                    p.getYRot(), p.getXRot());
        }
        RaidInstance updated = raid.withState(RaidInstance.State.IN_PROGRESS, List.of());
        INSTANCES.put(raid.typeId(), updated);
        READY_CHECK_DEADLINE.remove(raid.typeId());
        broadcast(server, updated, "raid.cobolmod.teleported", type.name(),
                raid.difficulty().scaledLevel(type.baseLevel()));
        CobolMod.LOGGER.info("[Raids] Équipe téléportée au raid {}", raid.typeId());
        syncAll(server);
        // TODO : faire apparaître le boss Cobblemon ici.
    }

    /** Termine et supprime une instance (fin de combat, ou commande admin). */
    public static void close(MinecraftServer server, String typeId) {
        RaidInstance raid = INSTANCES.remove(typeId);
        READY_CHECK_DEADLINE.remove(typeId);
        if (raid != null) {
            broadcast(server, raid, "raid.cobolmod.closed");
            syncAll(server);
        }
    }

    // ----------------------------------------------------------- admin

    public static void adminSetDifficulty(ServerPlayer admin, String typeId, RaidDifficulty difficulty) {
        if (!admin.hasPermissions(2)) return;
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null || raid.state() == RaidInstance.State.IN_PROGRESS) return;
        INSTANCES.put(typeId, raid.withDifficulty(difficulty));
        syncAll(admin.server);
    }

    public static void adminSetPublic(ServerPlayer admin, String typeId, boolean isPublic) {
        if (!admin.hasPermissions(2)) return;
        RaidInstance raid = INSTANCES.get(typeId);
        if (raid == null) return;
        INSTANCES.put(typeId, raid.withPublic(isPublic));
        syncAll(admin.server);
    }

    public static void adminClose(ServerPlayer admin, String typeId) {
        if (!admin.hasPermissions(2)) return;
        close(admin.server, typeId);
    }

    // ----------------------------------------------------------- tick

    /** Appelé à chaque tick serveur : expire les ready checks trop longs. */
    public static void tick(MinecraftServer server) {
        if (READY_CHECK_DEADLINE.isEmpty()) return;
        int now = server.getTickCount();
        for (String typeId : new ArrayList<>(READY_CHECK_DEADLINE.keySet())) {
            if (now >= READY_CHECK_DEADLINE.get(typeId)) {
                RaidInstance raid = INSTANCES.get(typeId);
                READY_CHECK_DEADLINE.remove(typeId);
                if (raid != null && raid.state() == RaidInstance.State.READY_CHECK) {
                    cancelReadyCheck(server, raid, "⏱");
                }
            }
        }
    }
}
