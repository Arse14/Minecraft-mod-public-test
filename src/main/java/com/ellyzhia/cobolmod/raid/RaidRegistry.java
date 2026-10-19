package com.ellyzhia.cobolmod.raid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Liste des raids connus par le client.
 * <p>
 * Version provisoire : contenu factice en dur, pour pouvoir tester l'interface.
 * À terme cette liste sera remplie par le serveur.
 */
public final class RaidRegistry {
    private static final List<RaidInfo> RAIDS = new ArrayList<>(List.of(
            new RaidInfo("volcan", "Raid du Volcan", "Charizard", 65, RaidDifficulty.DIFFICILE, true, 2, 4,
                    "Un Charizard enragé garde le cratère. Prévoyez des attaques Eau ou Roche."),
            new RaidInfo("abysses", "Raid des Abysses", "Gyarados", 58, RaidDifficulty.NORMAL, true, 0, 4,
                    "Au fond du lac, un Gyarados colossal attend les téméraires."),
            new RaidInfo("foret", "Raid de la Forêt", "Venusaur", 42, RaidDifficulty.FACILE, true, 4, 4,
                    "Idéal pour débuter : un Venusaur protège la clairière."),
            new RaidInfo("sommet", "Raid du Sommet", "Dragonite", 80, RaidDifficulty.EXTREME, false, 1, 4,
                    "Réservé aux dresseurs confirmés. Sur invitation uniquement."),
            new RaidInfo("ruines", "Raid des Ruines", "Gengar", 55, RaidDifficulty.DIFFICILE, false, 3, 4,
                    "Un Gengar hante les ruines. Les attaques Spectre et Ténèbres font la différence.")
    ));

    private RaidRegistry() {}

    public static List<RaidInfo> all() {
        return Collections.unmodifiableList(RAIDS);
    }

    public static RaidInfo get(int index) {
        return RAIDS.get(index);
    }

    public static int size() {
        return RAIDS.size();
    }

    /** Remplace un raid (utilisé par la page admin pour modifier difficulté / visibilité). */
    public static void replace(int index, RaidInfo updated) {
        RAIDS.set(index, updated);
    }
}
