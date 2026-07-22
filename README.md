# Bela-Pillage

Plugin Spigot 1.8.8 gerant la durabilite des blocs premium (Obsidienne, Enclume, Table
d'Enchantement, ...) face aux **explosions** pendant les pillages de factions.

⚠️ **Perimetre strict** : la durabilite ne s'applique **que** face aux explosions (TNT,
Creeper, ou toute autre source cote serveur). Le **minage vanilla n'est jamais modifie** :
un bloc protege reste cassable normalement a la pioche, recuperable normalement, avec un
comportement 100% vanilla. Ce plugin n'ecoute jamais `BlockBreakEvent` pour empecher ou
modifier une action de minage.

## Fonctionnement

- Chaque bloc protege possede une durabilite configurable (nombre d'explosions
  necessaires pour le detruire).
- A chaque explosion touchant un bloc protege : durabilite -1.
- Quand la durabilite atteint 0 : le bloc casse et est lache naturellement au sol (drop
  Minecraft normal), exactement comme n'importe quelle explosion vanilla.

Valeurs par defaut (`config.yml`, section `blocks`) :

| Bloc                  | Durabilité (explosions) |
|------------------------|--------------------------|
| Obsidienne              | 10                       |
| Enclume (vanilla)       | 20                       |
| Table d'Enchantement (vanilla) | 20                |

Le systeme est concu pour etre facilement extensible : pour proteger un nouveau bloc,
il suffit d'ajouter une entree dans `config.yml` (materiau + durabilite + nom
d'affichage) et de recharger (`/belapillage reload`) ou redemarrer. Aucune modification
de code n'est necessaire.

## Compatibilite avec Bela-Customs

Les blocs premium suivants sont **deja** proteges par Bela-Customs (`EmeraldChestListener`
et `BlockProtectionListener`) et ne sont **jamais** touches par Bela-Pillage :

| Bloc                                | Resistance (geree par Bela-Customs) |
|--------------------------------------|----------------------------------------|
| Coffre en Émeraude (Material.ENDER_CHEST) | 5 explosions                     |
| Table d'Enchantement en Émeraude (Material.PRISMARINE, data 2) | Insensible aux explosions |
| Enclume en Émeraude (Material.SEA_LANTERN) | Insensible aux explosions        |

Ces trois entrees sont affichees dans le GUI `/explosion` et `/pillage` a titre
purement informatif (`config.yml` -> `already-managed-by-bela-customs`), mais Bela-Pillage
n'implemente **aucune** logique pour elles. La securite ne repose pas que sur la
configuration : les materiaux `ENDER_CHEST`, `SEA_LANTERN` et `PRISMARINE` sont
explicitement rejetes au demarrage si un administrateur tentait malgre tout de les
ajouter a la section `blocks` (voir `PillageConfig`), pour qu'il soit impossible de
faire chevaucher Bela-Pillage et Bela-Customs, meme par erreur de configuration.

Concretement, les materiaux geres par Bela-Pillage (`OBSIDIAN`, `ANVIL`,
`ENCHANTMENT_TABLE` par defaut) sont completement disjoints de ceux geres par
Bela-Customs : les deux plugins ne se voient jamais mutuellement dans la liste des
blocs d'une meme explosion (`blockList()`), quelle que soit la priorite d'ecoute des
evenements.

> Note historique : l'ancien "Bloc d'Émeraude Renforcé" posable a ete retire de
> Bela-Customs (remplace par l'item "Émeraude Renforcée", pure monnaie d'echange, non
> posable) et n'est donc plus liste dans le GUI Bela-Pillage.

## Verification de la durabilite avec une patate

Un joueur tenant une **patate normale** (`Material.POTATO_ITEM`) en main peut faire un
**clic droit ou gauche** sur un bloc protege **situe en territoire ennemi** (relation
`ENEMY` via l'API publique SaberFactions) pour afficher immediatement sa durabilite
restante :

```
Obsidienne
Durabilité : 8 / 10
```

Cette verification est purement informative (aucun effet de bord, aucun evenement
annule) et ne fonctionne que sur les blocs geres activement par Bela-Pillage (pas sur
les blocs Bela-Customs, dont l'etat interne n'est pas accessible depuis ce plugin -
volontaire, pour ne jamais avoir a dependre de son implementation interne).

## Commandes

| Commande      | Description                                | Permission          |
|----------------|---------------------------------------------|-----------------------|
| `/explosion`   | Ouvre le GUI Bela-Pillage                    | `belapillage.use` (defaut: tous) |
| `/pillage`     | Alias strictement identique a `/explosion`   | `belapillage.use` (defaut: tous) |
| `/belapillage reload` | Recharge `config.yml` et `messages.yml` | `belapillage.admin` (defaut: op) |

## GUI

`/explosion` et `/pillage` ouvrent le meme GUI premium (verres decoratifs gris avec
coins emeraude, couleurs sobres) listant la resistance de tous les blocs concernes,
qu'ils soient geres par Bela-Pillage ou deja proteges par Bela-Customs (clairement
distingues, les blocs insensibles etant explicitement indiques comme tels).

## Persistance

Les degats de durabilite sont sauvegardes dans un fichier binaire compact
(`plugins/Bela-Pillage/durability.dat`, pas de YAML) qui ne contient que les blocs
ayant deja subi au moins une explosion : la structure reste donc performante meme avec
plusieurs milliers de blocs endommages. Sauvegarde automatique asynchrone periodique
(`storage.autosave-interval-seconds`, defaut 300s) + sauvegarde synchrone systematique a
l'extinction du serveur. Ecriture atomique (fichier temporaire puis renommage) pour
eviter toute corruption en cas de crash pendant une sauvegarde.

## Compilation

Requiert `spigot-api` 1.8.8-R0.1-SNAPSHOT (recupere automatiquement depuis le depot
SpigotMC) et `SaberFactions.jar` (plugin premium, non publie sur un depot Maven public) :

```bash
mkdir -p libs
# Recuperer SaberFactions.jar (ex: depuis le depot Bela-faction, ou copie depuis
# le dossier plugins/ de ton serveur) :
curl -fsSL -o SaberFactions.zip https://raw.githubusercontent.com/getwshape-gif/Bela-faction/main/SaberFactions.zip
unzip -o -j SaberFactions.zip "SaberFactions.jar" -d libs

mvn clean package
# jar genere dans target/Bela-Pillage.jar
```

La CI GitHub Actions (`.github/workflows/build.yml`) effectue automatiquement ces
etapes a chaque push/PR et publie le jar compile en artifact.

## Installation

1. Placer `SaberFactions.jar` dans `plugins/` (obligatoire, `depend: [Factions]`).
2. Recuperer `Bela-Pillage.jar` (compile via GitHub Actions, ou compile localement).
3. Placer le `.jar` dans `plugins/`.
4. Redemarrer le serveur.

Bela-Pillage cree automatiquement `config.yml` et `messages.yml` dans
`plugins/Bela-Pillage/` au premier demarrage, avec migration additive a chaque mise a
jour (les cles existantes personnalisees ne sont jamais ecrasees).
