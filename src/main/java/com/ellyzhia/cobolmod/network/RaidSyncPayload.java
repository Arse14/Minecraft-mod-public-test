package com.ellyzhia.cobolmod.network;

import com.ellyzhia.cobolmod.CobolMod;
import com.ellyzhia.cobolmod.raid.RaidInstance;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Serveur → client : l'état complet des instances de raid. */
public record RaidSyncPayload(List<RaidInstance> raids) implements CustomPacketPayload {
    public static final Type<RaidSyncPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CobolMod.MODID, "raid_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RaidSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.raids.size());
                for (RaidInstance r : p.raids) RaidInstance.STREAM_CODEC.encode(buf, r);
            },
            buf -> {
                int n = buf.readVarInt();
                List<RaidInstance> list = new ArrayList<>(n);
                for (int i = 0; i < n; i++) list.add(RaidInstance.STREAM_CODEC.decode(buf));
                return new RaidSyncPayload(list);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
