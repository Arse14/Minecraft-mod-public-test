package com.ellyzhia.cobolmod.raid;

/** Niveaux de difficulté d'un raid, avec leur couleur d'affichage (format 0xRRGGBB). */
public enum RaidDifficulty {
    FACILE("Facile", 0x4CAF50, 0.8f),
    NORMAL("Normal", 0x42A5F5, 1.0f),
    DIFFICILE("Difficile", 0xFFA726, 1.3f),
    EXTREME("Extrême", 0xEF5350, 1.7f);

    private final String label;
    private final int color;
    /** Multiplicateur appliqué au niveau du boss. */
    private final float levelScale;

    RaidDifficulty(String label, int color, float levelScale) {
        this.label = label;
        this.color = color;
        this.levelScale = levelScale;
    }

    public String label() {
        return label;
    }

    public int color() {
        return color;
    }

    public int scaledLevel(int baseLevel) {
        return Math.min(100, Math.round(baseLevel * levelScale));
    }

    /** Difficulté suivante (cyclique). */
    public RaidDifficulty next() {
        RaidDifficulty[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static RaidDifficulty byOrdinal(int ordinal) {
        RaidDifficulty[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
