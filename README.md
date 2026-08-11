# Rekordbot Mobile

Application Android autonome pour capturer un morceau partagé depuis Spotify ou synchronisé depuis Shazam, le qualifier rapidement et l’envoyer directement dans Airtable. Rekordbot PC pourra ensuite synchroniser ces entrées vers le workflow Rekordbox.

Version actuelle : **1.1.1**.

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
│   └── remote/                # Airtable, Spotify public et Soundcharts optionnel
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

Les identifiants Soundcharts sont facultatifs et conservés dans un coffre Keystore distinct de celui d’Airtable. Ils ne sont ni committés ni journalisés.

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

## Synchroniser Shazam via Spotify

La phase P9 ajoute une boîte de réception alimentée par la playlist Spotify `My Shazam Tracks` / `Mes titres Shazam`. La connexion utilise OAuth PKCE : aucun mot de passe Spotify ni client secret n’est stocké dans l’application. Le Client ID et l’URI de redirection proviennent d’une application créée dans Spotify Developer Dashboard ; les jetons sont chiffrés dans un coffre Android Keystore séparé.

Chaque morceau synchronisé reste localement **À décider** jusqu’à une action explicite. **Préparer** ouvre l’éditeur de métadonnées existant et enverra ensuite la ligne Airtable avec `Source = Shazam`. **Ignorer** conserve la décision localement afin que le morceau ne revienne pas lors des synchronisations suivantes. L’application ne crée jamais automatiquement une ligne Airtable depuis la playlist.

**Écouter / Pause** pilote le client Spotify actif depuis la liste, avec Spotify Premium. **Spotify** ouvre directement le morceau dans l’application Spotify ou, si elle n’est pas installée, dans le navigateur. Une session créée avant la version 1.1.1 doit être reconnectée une fois pour autoriser le contrôle de lecture.

La synchronisation s’effectue à l’ouverture de la boîte Shazam, par actualisation manuelle et périodiquement sous les contraintes Android. La première lecture est paginée et importe l’historique disponible du plus récent au plus ancien.

La création de l’application Spotify, la Redirect URI et le dépannage sont détaillés dans [docs/SPOTIFY_SHAZAM.md](docs/SPOTIFY_SHAZAM.md).

## Enrichissement Soundcharts optionnel

Dans **Réglages → Enrichissement Soundcharts**, l’option peut être activée avec un `App ID` et une `API Key` legacy existants, puis testée. L’application interroge l’endpoint officiel `GET /api/v2.25/song/by-platform/spotify/{id}` à partir du Spotify Track ID et en extrait l’ISRC ainsi que les genres `root`/`sub`, conservés dans leur ordre sous forme de texte brut dédoublonné.

Soundcharts recommande désormais des jetons d’accès obtenus avec un Client ID et un Client Secret côté serveur. Le mode direct mobile `x-app-id` / `x-api-key` est donc réservé aux comptes disposant déjà de ces identifiants legacy ; aucun nouveau secret ne doit être intégré au code ou distribué dans l’APK.

L’enrichissement reste non bloquant : une erreur d’authentification, un morceau absent ou une limite de requêtes n’empêche jamais l’envoi ni l’enregistrement d’un brouillon. Un genre ou un ISRC déjà saisi est conservé. Si Soundcharts propose un genre différent, l’aperçu affiche une suggestion et demande explicitement de choisir **Remplacer par la suggestion**.

## Lancer le projet

Pré-requis : Android Studio compatible AGP 8.13, JDK 17 et SDK Android 36.

```bash
./gradlew assembleDebug
```

L’APK est généré dans `app/build/outputs/apk/debug/`.

## Qualité

```bash
./gradlew formatCheck lintDebug lintRelease testDebugUnitTest testReleaseUnitTest assembleDebug assembleDebugAndroidTest assembleRelease
```

La CI exécute les mêmes contrôles à chaque push et pull request, puis publie les APK debug et release non signé comme artefacts. La procédure de signature stable, de livraison et le dépannage sont détaillés dans [docs/RELEASE.md](docs/RELEASE.md). Les changements de la version sont listés dans [CHANGELOG.md](CHANGELOG.md).

## Configuration locale

Ne jamais commiter de token Airtable, d’identifiants Soundcharts ni de clé de signature. Les fichiers `local.properties`, `secrets.properties`, `keystore.properties`, `.env`, `*.jks` et `*.keystore` sont ignorés.

Pour signer une release, copier `keystore.properties.example` vers `keystore.properties` et renseigner une clé durable, ou fournir les quatre variables `REKORDBOT_STORE_*` / `REKORDBOT_KEY_*` documentées. Sans configuration, le build release reste volontairement non signé.
