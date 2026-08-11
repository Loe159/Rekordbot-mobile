# Connexions d’une build publique

## Domaine public unique

Utiliser une origine HTTPS stable pour le Worker, par exemple `https://api.rekordbot.example`. Elle ne doit contenir ni slash final, ni chemin, ni port, ni paramètres. La valeur `REKORDBOT_PUBLIC_API_BASE_URL` / `publicApiBaseUrl` est l’unique source de vérité ; Gradle en dérive automatiquement :

- la Redirect URI Airtable : `https://api.rekordbot.example/oauth/airtable/callback` ;
- l’hôte de l’Android App Link ;
- l’URL du service Soundcharts.

Ne pas ajouter une seconde variable pour la Redirect URI. Le domaine doit être déployé avant la build publique et rester sous contrôle du projet.

## Spotify

Créer l’application dans Spotify Developer Dashboard et enregistrer exactement `rekordbot-mobile-login://callback`. Fournir son Client ID avec `REKORDBOT_SPOTIFY_CLIENT_ID` ou `spotifyClientId` dans `secrets.properties`. Ne jamais fournir le Client Secret à Android.

## Airtable

Créer une intégration OAuth sans Client Secret, avec les scopes `schema.bases:read`, `data.records:read` et `data.records:write`. Enregistrer exactement la valeur dérivée :

`https://<domaine-du-backend>/oauth/airtable/callback`

Fournir le Client ID avec `REKORDBOT_AIRTABLE_CLIENT_ID` ou `airtableClientId`, et la même origine HTTPS avec `REKORDBOT_PUBLIC_API_BASE_URL` ou `publicApiBaseUrl`. Aucun Client Secret Airtable ne doit être intégré à l’APK ou configuré sur ce backend : l’échange utilise PKCE directement depuis Android.

Le backend sert `/.well-known/assetlinks.json`. Configurer `ANDROID_PACKAGE_NAME=com.loe159.rekordbot.mobile` et `ANDROID_SHA256_CERT_FINGERPRINTS` avec les empreintes SHA-256 autorisées, séparées par des virgules. Pour Google Play, utiliser l’empreinte du **certificat de signature d’application** affichée dans Play Console, pas seulement celle de la clé d’import. Ajouter aussi l’empreinte de la clé qui signe l’APK si la release est distribuée directement.

L’App Link de production est limité à `/oauth/airtable/callback`. `assetlinks.json` doit répondre directement en HTTPS avec un statut 200 et `Content-Type: application/json`, sans redirection. La page du callback sert uniquement de repli si l’application n’est pas installée. Voir [`backend/README.md`](../backend/README.md).

L’utilisateur autorise ses ressources sur l’écran Airtable, puis Rekordbot liste uniquement les bases accessibles et leurs tables. Les refresh tokens tournants sont enregistrés immédiatement dans le coffre Android ; une ancienne valeur n’est jamais réutilisée volontairement.

## Soundcharts

Déployer `backend/`, puis enregistrer `SOUNDCHARTS_CLIENT_ID` et `SOUNDCHARTS_CLIENT_SECRET` comme secrets du serveur. Fournir l’URL HTTPS avec `REKORDBOT_PUBLIC_API_BASE_URL` ou `publicApiBaseUrl`.

Le serveur demande un jeton court par `client_credentials`, le met en cache jusqu’à son expiration et ne renvoie au mobile que l’ISRC et les genres. Ajouter une règle de limitation de débit chez l’hébergeur avant une diffusion large.

## CI GitHub

Créer un environnement GitHub `production`. Y enregistrer les valeurs publiques comme variables :

| Variable | Exemple |
|---|---|
| `REKORDBOT_SPOTIFY_CLIENT_ID` | Client ID Spotify |
| `REKORDBOT_AIRTABLE_CLIENT_ID` | Client ID Airtable |
| `REKORDBOT_PUBLIC_API_BASE_URL` | `https://api.rekordbot.example` |

Y enregistrer séparément les secrets de signature :

| Secret | Valeur |
|---|---|
| `REKORDBOT_UPLOAD_KEYSTORE_BASE64` | keystore d’import encodé en Base64 |
| `REKORDBOT_STORE_PASSWORD` | mot de passe du keystore |
| `REKORDBOT_KEY_ALIAS` | alias de la clé |
| `REKORDBOT_KEY_PASSWORD` | mot de passe de la clé |

La CI normale reste utilisable sans ces valeurs et produit une release de validation non signée. Un tag `v*` déclenche ensuite le job protégé `production`, qui refuse de construire si un Client ID, l’origine HTTPS ou la signature manque. Il génère un AAB pour Google Play, un APK signé pour les tests directs et leurs sommes SHA-256.

## Vérification avant publication

```bash
curl --fail --show-error https://api.rekordbot.example/health
curl --fail --show-error https://api.rekordbot.example/.well-known/assetlinks.json
adb shell pm verify-app-links --re-verify com.loe159.rekordbot.mobile
adb shell pm get-app-links com.loe159.rekordbot.mobile
```

La vérification Android doit indiquer le domaine comme approuvé. Tester ensuite une connexion Airtable complète depuis une installation provenant du canal interne Google Play ; cette build utilise le certificat réel de signature d’application.
