package com.ellyzhia.cobolmod.raid;

import java.util.List;
import java.util.Optional;

/** Catalogue des types de raids disponibles (provisoirement en dur). */
public final class RaidTypes {
    public static final List<RaidType> ALL = List.of(
            new RaidType("volcan", "Raid du Volcan", "Charizard", 65,
                    "Un Charizard enragé garde le cratère. Prévoyez des attaques Eau ou Roche.",
                    200, 80, 200),
            new RaidType("abysses", "Raid des Abysses", "Gyarados", 58,
                    "Au fond du lac, un Gyarados colossal attend les téméraires.",
                    -150, 40, 300),
            new RaidType("foret", "Raid de la Forêt", "Venusaur", 42,
                    "Idéal pour débuter : un Venusaur protège la clairière.",
                    50, 70, -120),
            new RaidType("sommet", "Raid du Sommet", "Dragonite", 80,
                    "Réservé aux dresseurs confirmés. Le vent y est glacial.",
                    400, 150, 400),
            new RaidType("ruines", "Raid des Ruines", "Gengar", 55,
                    "Un Gengar hante les ruines. Les attaques Spectre et Ténèbres font la différence.",
                    -300, 60, -300)
    );

    private RaidTypes() {}

    public static Optional<RaidType> byId(String id) {
        return ALL.stream().filter(t -> t.id().equals(id)).findFirst();
    }

    public static int indexOf(String id) {
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(id)) return i;
        }
        return -1;
    }
}
