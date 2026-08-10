# Contrat Airtable RekordBot — version 1

Ce contrat relie Rekordbot Mobile et RekordBot PC via la table Airtable `Sons`. Airtable est l’unique frontière d’échange : l’application Android ne dépend pas du PC.

## Champs

| Champ par défaut | Type attendu | Producteur | Rôle |
|---|---|---|---|
| `Titre` | texte | Mobile | identité de repli |
| `Artiste` | texte | Mobile | identité de repli |
| `Lien Spotify` | URL/texte | Mobile | source et extraction possible du Track ID |
| `Spotify Track ID` | texte | Mobile | identifiant prioritaire |
| `ISRC` | texte, optionnel | Mobile | deuxième identifiant stable |
| `Statut` | select/texte | Mobile/utilisateur | état de capture, distinct du traitement PC |
| `Genre brut` | texte, optionnel | Mobile | genre exact renvoyé par Soundcharts |
| `Énergie` | nombre 1–5, optionnel | Mobile | qualification DJ |
| `Mood`, `Situation`, `DJs inspirants` | listes, optionnelles | Mobile | qualifications DJ |
| `Commentaire` | texte, optionnel | Mobile | note libre |
| `Source` | texte/select | Mobile | `Spotify` par défaut |
| `État RekordBot` | select/texte | Mobile puis PC | `À traiter`, `Fichier trouvé`, `Fichier absent`, `Prêt pour Rekordbox` ou `Erreur` |
| `Erreur RekordBot` | texte | PC | diagnostic propre à la ligne |
| `Dernière synchro` | date/texte ISO 8601 | PC | date UTC du dernier traitement |
| `Méthode de matching` | texte | PC | `Spotify Track ID`, `ISRC`, `Titre + artiste` ou vide |

Les noms sont configurables dans l’application. Les valeurs ci-dessus sont les valeurs par défaut de la base `Sons`.

## Règles de synchronisation

1. Le mobile crée la ligne avec `État RekordBot = À traiter` et ne renseigne aucun résultat PC.
2. Le PC matche uniquement un fichier local réellement présent, dans l’ordre `Spotify Track ID` → `ISRC` → `Titre + Artiste` exact normalisé.
3. Un résultat absent ou ambigu ne modifie jamais Rekordbox.
4. `Statut` reste l’état de capture et n’est jamais écrasé par le PC.
5. Un dry-run ne PATCH jamais Airtable et ne modifie jamais la base Rekordbox.
6. Chaque ligne est isolée : une erreur n’empêche pas les suivantes d’être traitées.

## Limite actuelle

RekordBot PC écrit directement dans une copie sécurisée de la base Rekordbox puis, en usage réel, dans `master.db` avec sauvegarde et Rekordbox fermé. Aucun XML intermédiaire n’est produit.
