# Rekordbot Mobile

Application Android autonome pour capturer un morceau partagé depuis Spotify, le qualifier rapidement et l’envoyer directement dans Airtable. Rekordbot PC pourra ensuite synchroniser ces entrées vers le workflow Rekordbox.

> État actuel : **P2 — partage Spotify**. L’ajout direct dans Airtable arrive en P3 dans la [roadmap](ROADMAP.md).

## Stack

- Kotlin 2.3 et Jetpack Compose
- Android Gradle Plugin 8.13 / Gradle 8.13
- `minSdk 26`, `targetSdk 36`
- Java 17
- thème sombre Rekordbot inspiré des interfaces DJ professionnelles

## Architecture

```text
app/src/main/java/com/loe159/rekordbot/mobile/
├── data/
│   ├── local/                 # Persistance locale, Room prévu en P4
│   └── remote/airtable/       # Accès direct à Airtable prévu en P1/P3
├── domain/
│   ├── model/                 # Modèles métier indépendants de l’UI
│   ├── spotify/               # Parsing des liens, URI et métadonnées partagées
│   └── repository/            # Contrats du domaine
└── ui/
    ├── components/            # Composants du design system
    ├── home/                  # Écran d’accueil
    ├── share/                 # Aperçu modifiable d’un partage Spotify
    └── theme/                 # Couleurs, typographie, formes et thème
```

L’application appelle directement l’API Airtable, sans dépendance à un PC ni API intermédiaire. Le Personal Access Token est chiffré en AES-GCM avec une clé conservée dans l’Android Keystore ; les sauvegardes Android de l’application sont désactivées pour ne pas exporter sa configuration.

## Configurer Airtable

Dans l’application, ouvrir **Configurer Airtable**, puis renseigner :

1. un Personal Access Token limité à la base cible avec les droits `schema.bases:read` et `data.records:write` ;
2. le Base ID (`app…`) et le nom ou l’ID de la table (`tbl…`) ;
3. les noms exacts des champs Airtable et les valeurs par défaut.

Les champs optionnels absents de la table peuvent être laissés vides. **Tester la connexion** lit le schéma de la vraie table, vérifie chaque champ configuré et enregistre la configuration si elle est valide. La création d’un enregistrement de démonstration demande ensuite une confirmation explicite.

## Partager depuis Spotify

Depuis un morceau Spotify, ouvrir **Partager → Plus → Rekordbot**. L’application reconnaît les liens `open.spotify.com/track/...` et les URI `spotify:track:...`, puis affiche un aperçu modifiable du titre, de l’artiste, du lien et du Track ID. Si Spotify ne transmet que le lien, les champs manquants sont signalés clairement. Aucun enregistrement Airtable n’est encore créé automatiquement en P2.

## Lancer le projet

Pré-requis : Android Studio compatible AGP 8.13, JDK 17 et SDK Android 36.

```bash
./gradlew assembleDebug
```

L’APK est généré dans `app/build/outputs/apk/debug/`.

## Qualité

```bash
./gradlew formatCheck lintDebug testDebugUnitTest assembleDebug
```

La CI exécute les mêmes contrôles à chaque push et pull request.

## Configuration locale

Ne jamais commiter de token Airtable ni de clé de signature. Les fichiers `local.properties`, `secrets.properties`, `keystore.properties`, `.env`, `*.jks` et `*.keystore` sont ignorés.
