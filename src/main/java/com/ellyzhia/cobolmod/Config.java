package com.ellyzhia.cobolmod;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Options de configuration du mod (fichier config/cobolmod-common.toml).
 * Ajouter ici les valeurs réglables par le joueur.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue DEBUG_LOGS = BUILDER
            .comment("Affiche des logs de debug supplémentaires dans la console")
            .define("debugLogs", false);

    static final ModConfigSpec SPEC = BUILDER.build();
}
