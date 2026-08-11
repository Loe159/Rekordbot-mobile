# Changelog

## 1.2.0

- Client ID Spotify intégré aux builds publiques, avec saisie développeur conservée si absent ;
- connexion Airtable OAuth PKCE, jetons chiffrés et renouvellement avec rotation du refresh token ;
- sélection des bases et tables Airtable autorisées depuis les réglages ;
- PAT Airtable conservé comme mode avancé de transition ;
- service Soundcharts côté serveur et client mobile sans identifiant fournisseur ;
- contrôles CI du service public et configuration des builds via secrets GitHub.

## 1.1.1

- pré-écoute lecture/pause des titres Shazam sur le client Spotify actif ;
- ouverture directe de chaque morceau dans l’application Spotify, avec repli navigateur ;
- demande explicite du scope Spotify de contrôle de lecture et parcours de reconnexion pour les sessions existantes.

## 1.1.0

- connexion Spotify OAuth Authorization Code avec PKCE, sans client secret embarqué ;
- synchronisation paginée de `My Shazam Tracks` / `Mes titres Shazam` avec l’API Spotify 2026 ;
- boîte de réception Shazam locale avec décisions persistantes à traiter, enregistrer, ignorer ou déjà présent ;
- ouverture de l’éditeur existant avec les métadonnées préremplies et `Source = Shazam` ;
- synchronisation manuelle et périodique sous contrainte réseau ;
- jetons Spotify et session PKCE chiffrés dans un coffre Keystore dédié.

## 1.0.0

- capture des partages de morceaux Spotify et aperçu modifiable ;
- configuration Airtable sécurisée, validation de schéma et gestion des doublons ;
- file Room hors ligne avec reprise WorkManager et envois idempotents ;
- qualification DJ rapide et brouillons modifiables ;
- enrichissement Soundcharts optionnel sans écrasement des saisies ;
- contrat de synchronisation Rekordbot PC via Airtable ;
- identité Android finalisée : icônes adaptatives/monochromes, splash, navigation retour et contraste accessible ;
- contrôles CI des variantes debug et release, signature de release externe et documentée.
