package com.ellyzhia.cobolmod.raid;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Définition d'un raid : ce qui ne change pas d'une instance à l'autre.
 * Pour l'instant la liste est en dur dans {@link RaidTypes} ; plus tard elle
 * pourra venir d'un fichier de config ou d'un datapack.
 *
 * @param id          identifiant interne (minuscules, sans espace)
 * @param name        nom affiché
 * @param boss        nom du Pokémon boss (sert aussi à trouver l'image : textures/gui/boss/&lt;boss minuscule&gt;.png)
 * @param baseLevel   niveau du boss en difficulté Normal
 * @param description texte court affiché dans la fiche
 * @param arena       position de l'arène (x, y, z) où téléporter l'équipe
 */
public record RaidType(
        String id,
        String name,
        String boss,
        int baseLevel,
        String description,
        int arenaX, int arenaY, int arenaZ
) {
    public static final StreamCodec<RegistryFriendlyByteBuf, RaidType> STREAM_CODEC = StreamCodec.of(
            (buf, t) -> {
                buf.writeUtf(t.id);
                buf.writeUtf(t.name);
                buf.writeUtf(t.boss);
                buf.writeVarInt(t.baseLevel);
                buf.writeUtf(t.description);
                buf.writeVarInt(t.arenaX);
                buf.writeVarInt(t.arenaY);
                buf.writeVarInt(t.arenaZ);
            },
            buf -> new RaidType(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readVarInt(), buf.readUtf(),
                    buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
    );

    public String bossTextureName() {
        return boss.toLowerCase().replaceAll("[^a-z0-9_]", "");
    }
}
