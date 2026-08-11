# Connexions d’une build publique

## Spotify

Créer l’application dans Spotify Developer Dashboard et enregistrer exactement `rekordbot-mobile-login://callback`. Fournir son Client ID avec `REKORDBOT_SPOTIFY_CLIENT_ID` ou `spotifyClientId` dans `secrets.properties`. Ne jamais fournir le Client Secret à Android.

## Airtable

Créer une intégration OAuth sans Client Secret, avec les scopes `schema.bases:read`, `data.records:read` et `data.records:write`. Enregistrer exactement `rekordbot-mobile-login://airtable-callback`, puis fournir le Client ID avec `REKORDBOT_AIRTABLE_CLIENT_ID` ou `airtableClientId`.

L’utilisateur autorise ses ressources sur l’écran Airtable, puis Rekordbot liste uniquement les bases accessibles et leurs tables. Les refresh tokens tournants sont enregistrés immédiatement dans le coffre Android ; une ancienne valeur n’est jamais réutilisée volontairement.

## Soundcharts

Déployer `backend/`, puis enregistrer `SOUNDCHARTS_CLIENT_ID` et `SOUNDCHARTS_CLIENT_SECRET` comme secrets du serveur. Fournir l’URL HTTPS avec `REKORDBOT_PUBLIC_API_BASE_URL` ou `publicApiBaseUrl`.

Le serveur demande un jeton court par `client_credentials`, le met en cache jusqu’à son expiration et ne renvoie au mobile que l’ISRC et les genres. Ajouter une règle de limitation de débit chez l’hébergeur avant une diffusion large.

## CI GitHub

Créer les trois secrets de dépôt `REKORDBOT_SPOTIFY_CLIENT_ID`, `REKORDBOT_AIRTABLE_CLIENT_ID` et `REKORDBOT_PUBLIC_API_BASE_URL`. Une CI sans ces valeurs compile une build développeur : le Client ID Spotify et le PAT Airtable peuvent encore être saisis, tandis que Soundcharts utilise le mode legacy.
