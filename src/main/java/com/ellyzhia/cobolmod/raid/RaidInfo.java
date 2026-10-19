package com.ellyzhia.cobolmod.raid;

/**
 * Description d'un raid telle qu'affichée dans l'interface.
 * <p>
 * Pour l'instant les raids sont des données factices côté client ({@link RaidRegistry}) ;
 * plus tard elles viendront du serveur via un paquet réseau.
 *
 * @param id          identifiant interne (minuscules, sans espace)
 * @param name        nom affiché
 * @param boss        nom du Pokémon boss
 * @param bossLevel   niveau du boss
 * @param difficulty  difficulté
 * @param isPublic    ouvert à tous (true) ou sur invitation (false)
 * @param players     joueurs inscrits
 * @param maxPlayers  capacité (4 par défaut)
 * @param description texte court affiché dans le panneau de détail
 */
public record RaidInfo(
        String id,
        String name,
        String boss,
        int bossLevel,
        RaidDifficulty difficulty,
        boolean isPublic,
        int players,
        int maxPlayers,
        String description
) {
    public static final int DEFAULT_MAX_PLAYERS = 4;

    public boolean isFull() {
        return players >= maxPlayers;
    }

    public RaidInfo withDifficulty(RaidDifficulty newDifficulty) {
        return new RaidInfo(id, name, boss, bossLevel, newDifficulty, isPublic, players, maxPlayers, description);
    }

    public RaidInfo withPublic(boolean newPublic) {
        return new RaidInfo(id, name, boss, bossLevel, difficulty, newPublic, players, maxPlayers, description);
    }
}
