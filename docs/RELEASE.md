# Publication Android

## APK de test

Avec JDK 17 et le SDK Android 36 :

```bash
./gradlew formatCheck lintDebug testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

L’APK installable est produit dans `app/build/outputs/apk/debug/app-debug.apk`. La variante debug utilise l’identifiant `com.loe159.rekordbot.mobile.debug` et peut cohabiter avec la release.

## Signature stable de la release

Activer Play App Signing et conserver une clé d’import dédiée. Sa perte bloque les nouveaux envois jusqu’à sa réinitialisation ; la clé de signature d’application reste protégée par Google Play. Ni le keystore ni ses mots de passe ne doivent entrer dans Git, dans un APK ou dans les logs de CI.

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

Pour une vraie build publique, configurer aussi les trois valeurs de connexion décrites dans [`PUBLIC_CONNECTIONS.md`](PUBLIC_CONNECTIONS.md), puis activer la validation stricte :

```bash
./gradlew -PrekordbotProduction=true \
  formatCheck lintRelease testReleaseUnitTest bundleRelease assembleRelease
```

La commande échoue si les Client ID, l’origine HTTPS ou la signature sont absents. Elle produit :

- `app/build/outputs/bundle/release/app-release.aab` pour Google Play ;
- `app/build/outputs/apk/release/app-release.apk` pour les tests directs.

## Publication GitHub et Google Play

1. Déployer le backend sur son domaine HTTPS définitif et vérifier `/health`, la callback Airtable et `/.well-known/assetlinks.json`.
2. Enregistrer la Redirect URI exacte dans Airtable et le schéma personnalisé dans Spotify.
3. Configurer l’environnement GitHub `production`, ses variables publiques et ses secrets de signature.
4. Vérifier que `versionCode = 6`, `versionName = 1.2.1` et le changelog correspondent à la release.
5. Fusionner sur `main`, attendre la CI verte, puis créer le tag `v1.2.1` sur ce commit.
6. Récupérer l’AAB signé du job `production` et vérifier sa somme SHA-256.
7. Importer l’AAB dans le canal de test interne Google Play et activer Play App Signing.
8. Copier l’empreinte SHA-256 du certificat de **signature d’application** de Play Console dans `ANDROID_SHA256_CERT_FINGERPRINTS`, redéployer le backend, puis vérifier l’App Link.
9. Installer depuis le canal interne et tester Spotify, OAuth Airtable, Soundcharts, le mode hors ligne et la mise à jour depuis la version précédente.
10. Publier progressivement seulement après ces contrôles et après avoir renseigné la fiche Store, la politique de confidentialité et la section Sécurité des données.

L’AAB est le format de publication Google Play. L’APK signé reste utile au test direct, mais son certificat peut différer de celui appliqué par Play ; les deux empreintes peuvent donc être nécessaires dans `assetlinks.json`.

## Dépannage

| Symptôme | Action |
|---|---|
| `SDK location not found` | définir `sdk.dir` dans le fichier ignoré `local.properties` ou ouvrir le projet avec Android Studio |
| La release reste non signée | vérifier que les quatre propriétés ou variables sont présentes et que `storeFile` pointe vers un fichier lisible |
| `Keystore was tampered with` | vérifier le mot de passe du keystore et ne pas recréer une clé pour une application déjà publiée |
| Rekordbot n’apparaît pas dans Partager | partager un contenu `text/plain` contenant un lien ou une URI de morceau Spotify |
| Airtable refuse la connexion | reconnecter OAuth, vérifier que la base a été autorisée, puis contrôler la table et les noms/types exacts des champs |
| Le callback Airtable reste dans le navigateur | vérifier l’URL exacte, le package et l’empreinte Play dans `assetlinks.json`, puis relancer `pm verify-app-links` |
| La build de production s’arrête à la configuration | fournir les trois valeurs publiques, les quatre valeurs de signature et une origine HTTPS sans slash final |
| Un envoi reste en attente | rétablir le réseau puis ouvrir la file ; WorkManager reprend automatiquement, et l’action Réessayer reste disponible |
| Soundcharts échoue | vérifier le service public et ses secrets serveur, ou désactiver l’enrichissement ; l’envoi Airtable reste disponible |
