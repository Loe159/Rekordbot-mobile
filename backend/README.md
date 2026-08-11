# Rekordbot Public API

Petit service sans état utilisé par l’application publique pour interroger Soundcharts sans exposer le `client_secret` dans l’APK et héberger l’Android App Link du retour OAuth Airtable.

## Configuration

Déployer le Worker puis définir :

- les secrets `SOUNDCHARTS_CLIENT_ID` et `SOUNDCHARTS_CLIENT_SECRET` ;
- `SOUNDCHARTS_TEAM_ID` si le compte Soundcharts l’exige ;
- la variable `ANDROID_PACKAGE_NAME`, par exemple `com.loe159.rekordbot.mobile` ;
- la variable `ANDROID_SHA256_CERT_FINGERPRINTS`, contenant une ou plusieurs empreintes SHA-256 de certificats de signature séparées par une virgule. Ajouter les certificats réellement utilisés en production, par exemple celui de l’APK signé directement et celui de Google Play App Signing si les deux distributions doivent ouvrir les liens.

Configurer ensuite l’URL HTTPS du Worker dans la build Android avec `REKORDBOT_PUBLIC_API_BASE_URL`.

Les empreintes sont des données publiques d’association, pas des secrets. Les valeurs invalides font échouer `assetlinks.json` avec un statut 503 au lieu de publier une association permissive.

## OAuth Airtable et Android App Links

Enregistrer cette Redirect URI dans l’intégration OAuth Airtable :

`https://<domaine-du-worker>/oauth/airtable/callback`

L’application Android doit déclarer le même domaine et le chemin `/oauth/airtable/callback` dans un intent-filter HTTPS avec `android:autoVerify="true"`. Le Worker expose :

- `GET /.well-known/assetlinks.json` : association vérifiée entre le domaine, le package et ses certificats ;
- `GET /oauth/airtable/callback` : page de repli statique lorsque l’application n’intercepte pas le lien.

La page de repli retire immédiatement `code`, `state` ou `error` de l’URL du navigateur, ne les reflète ni ne les transmet à aucun tiers, et interdit cache, référent et intégration dans une frame. L’échange du code OAuth reste effectué directement par l’application avec PKCE ; le Worker ne reçoit et ne stocke aucun jeton.

Pour obtenir l’empreinte d’une clé locale :

```shell
keytool -list -v -keystore chemin/vers/rekordbot.jks -alias alias | grep SHA256
```

Pour une version distribuée par Google Play, recopier aussi l’empreinte SHA-256 du certificat **App signing key certificate** affichée dans Play Console.

Vérification après déploiement :

```shell
curl -i https://<domaine-du-worker>/.well-known/assetlinks.json
curl -i https://<domaine-du-worker>/oauth/airtable/callback
npm test
```

`assetlinks.json` doit être accessible directement en HTTPS, sans redirection.

## Soundcharts

Le service n’expose qu’un endpoint contraint par Spotify Track ID :

`GET /v1/soundcharts/tracks/spotify/{spotifyTrackId}`

Le corps fournisseur est réduit à l’ISRC et aux genres. Les erreurs et secrets Soundcharts ne sont jamais renvoyés au mobile. Avant une diffusion large, activer une règle de limitation de débit sur cet endpoint dans l’hébergeur.
