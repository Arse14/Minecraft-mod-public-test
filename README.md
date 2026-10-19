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
- **Touche `U`** : ouvre l'écran des raids (design « Pokédex moderne »). Liste des raids à gauche,
  fiche à droite, boutons *Rejoindre* / *Se téléporter*. L'onglet **Admin** (difficulté, public/privé)
  n'apparaît que pour les opérateurs (`/op`, ou monde solo avec les commandes activées).
  Les raids sont pour l'instant des données factices.

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
│   └── RaidScreen.java      # écran des raids (Pokédex moderne)
├── raid/
│   ├── RaidInfo.java        # description d'un raid
│   ├── RaidDifficulty.java  # niveaux de difficulté
│   └── RaidRegistry.java    # liste des raids (factice pour l'instant)
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
