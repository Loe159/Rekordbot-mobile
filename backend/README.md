# Rekordbot Public API

Petit service sans état utilisé par l’application publique pour interroger Soundcharts sans exposer le `client_secret` dans l’APK.

## Configuration

Déployer le Worker puis définir les secrets `SOUNDCHARTS_CLIENT_ID` et `SOUNDCHARTS_CLIENT_SECRET`. `SOUNDCHARTS_TEAM_ID` est facultatif. Configurer ensuite l’URL publique dans la build Android avec `REKORDBOT_PUBLIC_API_BASE_URL`.

Le service n’expose qu’un endpoint contraint par Spotify Track ID :

`GET /v1/soundcharts/tracks/spotify/{spotifyTrackId}`

Le corps fournisseur est réduit à l’ISRC et aux genres. Les erreurs et secrets Soundcharts ne sont jamais renvoyés au mobile. Avant une diffusion large, activer une règle de limitation de débit sur cet endpoint dans l’hébergeur.
