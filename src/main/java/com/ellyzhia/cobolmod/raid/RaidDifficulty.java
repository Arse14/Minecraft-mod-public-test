package com.ellyzhia.cobolmod.raid;

/** Niveaux de difficulté d'un raid, avec leur couleur d'affichage (format 0xRRGGBB). */
public enum RaidDifficulty {
    FACILE("Facile", 0x4CAF50),
    NORMAL("Normal", 0x42A5F5),
    DIFFICILE("Difficile", 0xFFA726),
    EXTREME("Extrême", 0xEF5350);

    private final String label;
    private final int color;

    RaidDifficulty(String label, int color) {
        this.label = label;
        this.color = color;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }

    /** Difficulté suivante (cyclique), pratique pour un bouton "difficulté". */
    public RaidDifficulty next() {
        RaidDifficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
