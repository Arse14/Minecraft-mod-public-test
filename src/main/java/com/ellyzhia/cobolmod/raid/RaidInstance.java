package com.ellyzhia.cobolmod.raid;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Instance d'un raid ouverte par un joueur : son état tel qu'il est synchronisé vers les clients.
 * <p>
 * Cycle de vie : OPEN (on peut rejoindre) → READY_CHECK (vote de téléportation en cours)
 * → IN_PROGRESS (équipe téléportée, combat) → terminé (instance supprimée).
 *
 * @param typeId      type de raid ({@link RaidTypes})
 * @param leader      joueur qui a ouvert le raid
 * @param difficulty  difficulté choisie à l'ouverture
 * @param isPublic    ouvert à tous ou sur invitation
 * @param maxPlayers  capacité (4)
 * @param members     joueurs inscrits (le leader en premier)
 * @param state       état courant
 * @param readyCheck  joueurs ayant répondu "prêt" pendant un ready check (vide sinon)
 */
public record RaidInstance(
        String typeId,
        RaidMember leader,
        RaidDifficulty difficulty,
        boolean isPublic,
        int maxPlayers,
        List<RaidMember> members,
        State state,
        List<UUID> readyCheck
) {
    public static final int DEFAULT_MAX_PLAYERS = 4;

    public enum State { OPEN, READY_CHECK, IN_PROGRESS }

    /** Un joueur inscrit : identifiant + nom affiché (le client n'a pas forcément le joueur en vue). */
    public record RaidMember(UUID uuid, String name) {
        public static final StreamCodec<RegistryFriendlyByteBuf, RaidMember> STREAM_CODEC = StreamCodec.of(
                (buf, m) -> { buf.writeUUID(m.uuid); buf.writeUtf(m.name); },
                buf -> new RaidMember(buf.readUUID(), buf.readUtf()));
    }

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidInstance> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                buf.writeUtf(r.typeId);
                RaidMember.STREAM_CODEC.encode(buf, r.leader);
                buf.writeEnum(r.difficulty);
                buf.writeBoolean(r.isPublic);
                buf.writeVarInt(r.maxPlayers);
                buf.writeVarInt(r.members.size());
                for (RaidMember m : r.members) RaidMember.STREAM_CODEC.encode(buf, m);
                buf.writeEnum(r.state);
                buf.writeVarInt(r.readyCheck.size());
                for (UUID u : r.readyCheck) buf.writeUUID(u);
            },
            buf -> {
                String typeId = buf.readUtf();
                RaidMember leader = RaidMember.STREAM_CODEC.decode(buf);
                RaidDifficulty difficulty = buf.readEnum(RaidDifficulty.class);
                boolean isPublic = buf.readBoolean();
                int maxPlayers = buf.readVarInt();
                int n = buf.readVarInt();
                List<RaidMember> members = new ArrayList<>(n);
                for (int i = 0; i < n; i++) members.add(RaidMember.STREAM_CODEC.decode(buf));
                State state = buf.readEnum(State.class);
                int r = buf.readVarInt();
                List<UUID> ready = new ArrayList<>(r);
                for (int i = 0; i < r; i++) ready.add(buf.readUUID());
                return new RaidInstance(typeId, leader, difficulty, isPublic, maxPlayers, members, state, ready);
            }
    );

    public static RaidInstance open(RaidType type, RaidMember leader, RaidDifficulty difficulty, boolean isPublic) {
        return new RaidInstance(type.id(), leader, difficulty, isPublic, DEFAULT_MAX_PLAYERS,
                List.of(leader), State.OPEN, List.of());
    }

    public boolean isFull() {
        return members.size() >= maxPlayers;
    }

    public boolean isMember(UUID uuid) {
        return members.stream().anyMatch(m -> m.uuid().equals(uuid));
    }

    public boolean isLeader(UUID uuid) {
        return leader.uuid().equals(uuid);
    }

    public boolean isReady(UUID uuid) {
        return readyCheck.contains(uuid);
    }

    public boolean everyoneReady() {
        return members.stream().allMatch(m -> readyCheck.contains(m.uuid()));
    }

    // --- "with..." : le record est immuable, chaque changement crée une copie ---

    public RaidInstance withMembers(List<RaidMember> newMembers) {
        return new RaidInstance(typeId, leader, difficulty, isPublic, maxPlayers, List.copyOf(newMembers), state, readyCheck);
    }

    public RaidInstance withState(State newState, List<UUID> newReady) {
        return new RaidInstance(typeId, leader, difficulty, isPublic, maxPlayers, members, newState, List.copyOf(newReady));
    }

    public RaidInstance withDifficulty(RaidDifficulty d) {
        return new RaidInstance(typeId, leader, d, isPublic, maxPlayers, members, state, readyCheck);
    }

    public RaidInstance withPublic(boolean p) {
        return new RaidInstance(typeId, leader, difficulty, p, maxPlayers, members, state, readyCheck);
    }
}
