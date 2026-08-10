# Rekordbot Mobile

Application Android autonome pour capturer un morceau partagé depuis Spotify, le qualifier rapidement et l’envoyer directement dans Airtable. Rekordbot PC pourra ensuite synchroniser ces entrées vers le workflow Rekordbox.

> Le contrat P7 de synchronisation Mobile ↔ Airtable ↔ PC est documenté et versionné. Voir [docs/AIRTABLE_SYNC_CONTRACT.md](docs/AIRTABLE_SYNC_CONTRACT.md).

## Stack

- Kotlin 2.3 et Jetpack Compose
- Android Gradle Plugin 8.13 / Gradle 8.13
- Room 2.8.4 et WorkManager 2.11.2
- `minSdk 26`, `targetSdk 36`
- Java 17
- thème sombre Rekordbot inspiré des interfaces DJ professionnelles

## Architecture

```text
app/src/main/java/com/loe159/rekordbot/mobile/
├── data/
│   ├── local/                 # Configuration chiffrée et file Room
│   ├── work/                  # Reprise réseau via WorkManager
│   └── remote/airtable/       # Schéma, doublons et création directe Airtable
├── domain/
│   ├── model/                 # Modèles métier indépendants de l’UI
│   ├── spotify/               # Parsing des liens, URI et métadonnées partagées
│   └── repository/            # Contrats du domaine
└── ui/
    ├── components/            # Composants du design system
    ├── home/                  # Écran d’accueil
    ├── queue/                 # Suivi et actions sur la file Airtable
    ├── share/                 # Aperçu modifiable d’un partage Spotify
    └── theme/                 # Couleurs, typographie, formes et thème
```

L’application appelle directement l’API Airtable, sans dépendance à un PC ni API intermédiaire. Le Personal Access Token est chiffré en AES-GCM avec une clé conservée dans l’Android Keystore ; les sauvegardes Android de l’application sont désactivées pour ne pas exporter sa configuration.

## Configurer Airtable

Dans l’application, ouvrir **Configurer Airtable**, puis renseigner :

1. un Personal Access Token limité à la base cible avec les droits `schema.bases:read`, `data.records:read` et `data.records:write` ;
2. le Base ID (`app…`) et le nom ou l’ID de la table (`tbl…`) ;
3. les noms exacts des champs Airtable et les valeurs par défaut. Les valeurs proposées correspondent à la table `Sons`.

Les champs optionnels absents de la table peuvent être laissés vides. **Tester la connexion** lit le schéma de la vraie table, vérifie chaque champ configuré et enregistre la configuration si elle est valide. La création d’un enregistrement de démonstration demande ensuite une confirmation explicite.

## Partager depuis Spotify

Depuis un morceau Spotify, ouvrir **Partager → Plus → Rekordbot**. L’application reconnaît les liens `open.spotify.com/track/...` et les URI `spotify:track:...`, puis affiche un aperçu modifiable du titre, de l’artiste, du lien, du Track ID et d’un ISRC optionnel. Si Spotify ne transmet que le lien, l’application récupère le titre et l’artiste depuis la page publique du morceau.

Avant l’envoi, les raccourcis mobiles permettent de choisir une énergie de 1 à 5, plusieurs moods, situations et DJs inspirants, puis d’ajouter un commentaire. Ces valeurs sont facultatives : l’énergie est envoyée comme nombre Airtable et les sélections multiples comme tableaux JSON, jamais comme texte concaténé.

Le bouton **Ajouter à Airtable** renseigne les champs configurés, la source, le statut de capture et `État RekordBot = À traiter`. Les champs de retour PC (`Erreur RekordBot`, `Dernière synchro`, `Méthode de matching`) restent distincts et ne sont pas inventés par le mobile. **Garder comme brouillon** conserve aussi un morceau incomplet dans la même base Room, sans l’envoyer. La stratégie de doublons se règle dans **Doublons Spotify** : elle peut bloquer un Track ID déjà présent ou autoriser sa création. Si le réseau ou Airtable est indisponible, le morceau est conservé dans Room avec un UUID d’opération stable, puis WorkManager reprend automatiquement l’envoi dès que le réseau revient.

La **File d’attente** de l’accueil affiche les brouillons et les opérations en attente, en cours, envoyées ou en échec. Un brouillon reste modifiable, même incomplet, et n’entre dans la file d’envoi qu’après l’action **Envoyer**. Les opérations non envoyées permettent aussi de modifier la qualification DJ. Un échec peut être relancé ou supprimé. Les opérations interrompues sont récupérées au prochain démarrage ; avant toute nouvelle tentative incertaine, le Track ID Spotify est vérifié pour éviter une seconde création.

Les valeurs exactes proposées sont centralisées dans `DjQualificationOptions` : moods `Sexy`, `Énergique`, `Sombre`, `Joyeux`, `Ambiant`, `Calme`, `Mystérieux`, `Triste`, situations du warm-up au B2B, et la liste de DJs inspirants utilisée dans Airtable. Leur orthographe peut ainsi évoluer à un seul endroit.

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
