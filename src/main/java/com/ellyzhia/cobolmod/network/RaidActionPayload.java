package com.ellyzhia.cobolmod.network;

import com.ellyzhia.cobolmod.CobolMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Client → serveur : une demande d'action sur un raid.
 * Le serveur vérifie tout ; le client ne fait que demander.
 *
 * @param action action demandée
 * @param typeId type de raid concerné
 * @param value  paramètre selon l'action : ordinal de difficulté (OPEN, ADMIN_DIFFICULTY),
 *               1/0 pour public/privé (OPEN, ADMIN_PUBLIC) ou prêt/pas prêt (READY_ANSWER)
 * @param flag   second paramètre booléen (OPEN : public ?)
 */
public record RaidActionPayload(Action action, String typeId, int value, boolean flag) implements CustomPacketPayload {
    public enum Action {
        OPEN,             // ouvrir une instance : value = difficulté, flag = public
        JOIN,             // rejoindre l'instance
        LEAVE,            // quitter (ou dissoudre si leader)
        REQUEST_TELEPORT, // leader : lancer le ready check
        READY_ANSWER,     // membre : value = 1 prêt / 0 refuse
        ADMIN_DIFFICULTY, // admin : value = difficulté
        ADMIN_PUBLIC,     // admin : flag = public
        ADMIN_CLOSE       // admin : fermer l'instance
    }

    public static final Type<RaidActionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobolMod.MODID, "raid_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidActionPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeEnum(p.action);
                buf.writeUtf(p.typeId);
                buf.writeVarInt(p.value);
                buf.writeBoolean(p.flag);
            },
            buf -> new RaidActionPayload(buf.readEnum(Action.class), buf.readUtf(), buf.readVarInt(), buf.readBoolean()));

    public static RaidActionPayload of(Action action, String typeId) {
        return new RaidActionPayload(action, typeId, 0, false);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
