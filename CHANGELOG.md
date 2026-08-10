# Changelog

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
