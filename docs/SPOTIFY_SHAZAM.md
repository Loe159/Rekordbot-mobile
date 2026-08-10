# Connexion Shazam via Spotify

Rekordbot Mobile lit la playlist Spotify alimentée automatiquement par Shazam. Aucun morceau n’est envoyé vers Airtable avant une action explicite dans la boîte de réception.

## Préparer Spotify Developer

1. Créer une application dans le [Spotify Developer Dashboard](https://developer.spotify.com/dashboard).
2. Ajouter exactement cette Redirect URI : `rekordbot-mobile-login://callback`.
3. Copier le **Client ID** dans l’écran Shazam de Rekordbot Mobile.
4. Ne jamais renseigner ni distribuer le Client Secret : l’application utilise Authorization Code avec PKCE.
5. Vérifier que le compte Spotify utilisé fait partie des utilisateurs autorisés de l’application en Development Mode.

Le seul scope demandé est `playlist-read-private`. Rekordbot Mobile ne peut donc ni modifier les playlists ni contrôler la lecture Spotify.

## Première synchronisation

1. Dans Shazam Android, activer la synchronisation Spotify afin de créer `My Shazam Tracks` ou `Mes titres Shazam`.
2. Dans Rekordbot Mobile, ouvrir **Shazam**, saisir le Client ID puis choisir **Connecter Spotify**.
3. Autoriser l’accès dans Spotify et revenir automatiquement dans Rekordbot Mobile.
4. Lancer **Synchroniser**. Si la playlist n’est pas détectée automatiquement, renseigner son nom exact.

La première synchronisation lit toutes les pages disponibles. Les suivantes dédoublonnent les morceaux avec le Spotify Track ID et conservent les décisions déjà prises.

## Décisions

- **Préparer** ouvre l’éditeur existant avec les métadonnées Spotify. Après validation, Airtable reçoit `Source = Shazam`.
- **Ignorer** masque durablement le morceau de la liste à traiter.
- **Ignorés** permet de consulter ces morceaux et de les remettre à traiter.
- **Déjà présent** indique qu’une entrée correspondant au Spotify Track ID existe déjà.

## Sécurité et limites Spotify

- Les access et refresh tokens sont chiffrés avec une clé distincte dans Android Keystore.
- Le `state` OAuth est vérifié avant tout échange de code.
- Les nouvelles applications Spotify en Development Mode sont soumises aux limites de compte et d’utilisateurs imposées par Spotify.
- En 2026, Rekordbot utilise le nouvel endpoint `GET /playlists/{id}/items`; la playlist doit appartenir au compte connecté ou être collaborative.
- Une synchronisation périodique Android n’est pas instantanée et peut être retardée par l’économie de batterie.

Références : [OAuth PKCE](https://developer.spotify.com/documentation/web-api/tutorials/code-pkce-flow), [Redirect URIs](https://developer.spotify.com/documentation/web-api/concepts/redirect_uri), [Get Current User’s Playlists](https://developer.spotify.com/documentation/web-api/reference/get-a-list-of-current-users-playlists), [Get Playlist Items](https://developer.spotify.com/documentation/web-api/reference/get-playlists-items).
