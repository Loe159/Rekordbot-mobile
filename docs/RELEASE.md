# Publication Android

## APK de test

Avec JDK 17 et le SDK Android 36 :

```bash
./gradlew formatCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

L’APK installable est produit dans `app/build/outputs/apk/debug/app-debug.apk`. La variante debug utilise l’identifiant `com.loe159.rekordbot.mobile.debug` et peut cohabiter avec la release.

## Signature stable de la release

La clé de signature est un actif durable : sa perte empêche toute mise à jour de l’application existante. Elle ne doit jamais entrer dans Git, dans un APK ou dans les logs de CI.

Copier `keystore.properties.example` vers le fichier ignoré `keystore.properties`, puis remplacer les quatre valeurs :

```properties
storeFile=release/rekordbot-upload.jks
storePassword=...
keyAlias=rekordbot
keyPassword=...
```

Le chemin `storeFile` est résolu depuis la racine du dépôt. Les mêmes valeurs peuvent être fournies sans fichier :

| Variable | Valeur |
|---|---|
| `REKORDBOT_STORE_FILE` | chemin du keystore |
| `REKORDBOT_STORE_PASSWORD` | mot de passe du keystore |
| `REKORDBOT_KEY_ALIAS` | alias de la clé |
| `REKORDBOT_KEY_PASSWORD` | mot de passe de la clé |

Les quatre valeurs sont obligatoires pour activer la signature. Sans elles, `assembleRelease` produit volontairement un APK non signé afin que la CI puisse quand même vérifier la variante release.

```bash
./gradlew clean formatCheck lintRelease testReleaseUnitTest assembleRelease
```

Résultats attendus :

- configuré : `app/build/outputs/apk/release/app-release.apk` ;
- non configuré : `app/build/outputs/apk/release/app-release-unsigned.apk`.

Avant diffusion, conserver une sauvegarde chiffrée du keystore, vérifier la version `1.0.0`, installer l’APK signé sur un appareil propre et tester le flux Spotify → Rekordbot → Airtable ainsi qu’un envoi sans réseau.

## Dépannage

| Symptôme | Action |
|---|---|
| `SDK location not found` | définir `sdk.dir` dans le fichier ignoré `local.properties` ou ouvrir le projet avec Android Studio |
| La release reste non signée | vérifier que les quatre propriétés ou variables sont présentes et que `storeFile` pointe vers un fichier lisible |
| `Keystore was tampered with` | vérifier le mot de passe du keystore et ne pas recréer une clé pour une application déjà publiée |
| Rekordbot n’apparaît pas dans Partager | partager un contenu `text/plain` contenant un lien ou une URI de morceau Spotify |
| Airtable refuse la connexion | vérifier le PAT, le Base ID, la table, les droits et les noms/types exacts des champs |
| Un envoi reste en attente | rétablir le réseau puis ouvrir la file ; WorkManager reprend automatiquement, et l’action Réessayer reste disponible |
| Soundcharts échoue | vérifier les identifiants legacy ou désactiver l’enrichissement ; l’envoi Airtable reste disponible |
