# Ellyzhia (cobolmod)

Mod NeoForge pour **Minecraft 1.21.1** (NeoForge 21.1.x).

## Prérequis

- **JDK 21** (ex. [Eclipse Temurin 21](https://adoptium.net/))
- **IntelliJ IDEA** (Community suffit) — ou Eclipse / VS Code
- Pas besoin d'installer Gradle : le wrapper `gradlew` s'en charge.

## Lancer le jeu avec le mod

### Depuis IntelliJ

1. `File > Open…` et choisir le dossier du projet (celui qui contient `build.gradle`).
2. Laisser IntelliJ importer le projet Gradle (le premier import télécharge Minecraft et NeoForge : plusieurs minutes).
3. Dans la fenêtre **Gradle** (à droite) : `Tasks > neogradle > runs > runClient`, double-clic.
   Ou utiliser la configuration de lancement `runClient` créée automatiquement.

### En ligne de commande

```bash
./gradlew runClient        # Linux / macOS
gradlew.bat runClient      # Windows
```

Le monde de test est créé dans `run/client/`.

## Tester en jeu

- Onglet créatif **Ellyzhia** (icône : éclat violet) avec :
  - **Éclat d'Ellyzhia** (`cobolmod:ellyzhia_shard`)
  - **Bloc d'Ellyzhia** (`cobolmod:ellyzhia_block`) — émet de la lumière, se mine à la pioche en pierre minimum.
- Recettes : `améthyste + lapis` → 2 éclats ; 9 éclats ↔ 1 bloc.
- Commande : `/give @s cobolmod:ellyzhia_shard`
- **Touche `U`** : ouvre l'écran des raids (design « Pokédex moderne »).
  - Gauche : les types de raid, avec une pastille d'état (gris = fermé, vert = ouvert, orange = ready check, rouge = en cours/complet).
  - Droite : grande bannière du boss, **badge de difficulté cliquable** (change la difficulté avant d'ouvrir),
    état, équipe, boutons.
  - **Créer** crée l'instance (vous en êtes le chef) ; les autres joueurs voient alors **Rejoindre**.
  - **Se téléporter** (chef uniquement) lance un **ready check** : chaque membre reçoit une fenêtre Prêt / Pas prêt ;
    quand tout le monde est prêt l'équipe entière est téléportée à l'arène. Un refus ou 30 s sans réponse annule.
  - Onglet **Admin** (opérateurs) : changer la difficulté d'une instance (clic sur le badge), public/privé, fermer.
  - Les positions d'arène et les boss sont dans `RaidTypes.java` ; les images de boss dans `textures/gui/boss/`
    (placeholders en attendant les sprites Cobblemon).

## Construire le `.jar`

```bash
./gradlew build
```

Le fichier est produit dans `build/libs/cobolmod-<version>.jar`, à déposer dans le dossier `mods/` d'une instance NeoForge 1.21.1.

## Structure du code

```
src/main/java/com/ellyzhia/cobolmod/
├── CobolMod.java            # point d'entrée (@Mod), enregistre les registres
├── CobolModClient.java      # code client uniquement (écrans, rendus, touches)
├── Config.java              # options de config (config/cobolmod-common.toml)
├── client/
│   ├── ModKeyBindings.java  # touche U -> ouvre RaidScreen
│   ├── RaidScreen.java      # écran des raids (Pokédex moderne)
│   ├── ReadyCheckScreen.java# fenêtre Prêt / Pas prêt
│   └── ClientRaidState.java # copie client de l'état des raids
├── network/
│   ├── ModNetwork.java      # enregistrement des paquets
│   ├── RaidSyncPayload.java # serveur -> client : état des raids
│   └── RaidActionPayload.java # client -> serveur : ouvrir, rejoindre, ready...
├── raid/
│   ├── RaidType(s).java     # catalogue des raids (boss, niveau, arène)
│   ├── RaidInstance.java    # instance ouverte : chef, équipe, état
│   ├── RaidDifficulty.java  # niveaux de difficulté
│   ├── RaidManager.java     # règles côté serveur
│   └── RaidServerEvents.java# connexion / tick / arrêt
└── registry/
    ├── ModItems.java        # tous les items
    ├── ModBlocks.java       # tous les blocs
    └── ModCreativeTabs.java # onglet créatif Ellyzhia

src/main/resources/
├── META-INF/neoforge.mods.toml   # métadonnées du mod
├── assets/cobolmod/              # côté client : modèles, textures, traductions
└── data/cobolmod/                # côté données : recettes, loot tables, tags
```

Les identifiants (`mod_id`, version, nom) se règlent dans `gradle.properties`.
