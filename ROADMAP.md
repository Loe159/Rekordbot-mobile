# Roadmap — Rekordbot Mobile

## Objectif

Une application Android autonome permettant de capturer un titre depuis Spotify et de l’ajouter immédiatement à Airtable. Le Rekordbot PC synchronisera ensuite les entrées Airtable pour poursuivre la préparation et l’import dans Rekordbox.

```
Spotify → Partager vers Rekordbot Mobile → Airtable → Rekordbot PC → Rekordbox
```

L’application ne dépend ni du PC ni d’une API intermédiaire : elle appelle Airtable directement.

## Périmètre et décisions

- Android natif : Kotlin + Jetpack Compose.
- Réception du partage Spotify via `ACTION_SEND`, notamment les liens `open.spotify.com/track/...` et les URI `spotify:track:...`.
- Écriture directe dans Airtable avec un Personal Access Token configuré dans l’application.
- Le token et la configuration Airtable restent uniquement sur le téléphone, dans le stockage sécurisé Android ; aucun secret ne doit être commité.
- Aucune récupération ou téléchargement automatique de musique depuis Spotify ou YouTube.
- Le genre Soundcharts reste une valeur brute dans un champ Airtable texte libre, sans mapping vers une liste fermée.
- L’échange avec le Rekordbot PC passe uniquement par Airtable. La première intégration Rekordbox reste l’export XML ; l’écriture directe de la base Rekordbox n’est pas dans ce périmètre.

## Direction visuelle

Rekordbot Mobile s’inspire de l’ergonomie des logiciels DJ professionnels, notamment l’ambiance sombre et les repères bleus de Rekordbox, tout en conservant une identité propre. Aucun logo, icône ou écran de Rekordbox n’est reproduit.

- Thème sombre uniquement au lancement : fond `#111318`, surfaces `#1B1F27` et bordures discrètes.
- Bleu électrique `#168BFF` pour l’action principale, les éléments sélectionnés et les indicateurs de progression ; le bleu n’est jamais le seul moyen de transmettre une information.
- Composants compacts et lisibles : listes de titres, cartes de préparation sobres, champs peu encombrants, tags colorés et valeurs d’énergie immédiatement repérables.
- Hiérarchie orientée usage DJ : titre et artiste d’abord, puis statut d’envoi, métadonnées et actions secondaires.
- Les couleurs, espacements, rayons, typographies et états interactifs sont centralisés dans un design system Compose ; le contraste reste accessible, y compris en plein soleil.

## Données Airtable

La première configuration permet de renseigner le Base ID, la table et le nom de chaque champ. Les champs visés sont :

| Donnée | Source initiale | Obligatoire |
|---|---|---|
| Titre | partage Spotify / enrichissement | oui |
| Artiste | partage Spotify / enrichissement | oui |
| Lien Spotify | partage Spotify | oui |
| Spotify Track ID | lien/URI Spotify | oui |
| Statut | valeur configurée, par défaut `À qualifier` | oui |
| Genre brut | saisie ou enrichissement ultérieur | non |
| Énergie | saisie rapide | non |
| Mood | saisie rapide | non |
| Situation | saisie rapide | non |
| DJs inspirants | saisie rapide | non |
| Commentaire | saisie ou note vocale transcrite ultérieurement | non |
| Source | valeur configurée, par défaut `Spotify` | recommandé |

Les intitulés exacts et les valeurs de champs Select seront configurables, pour correspondre à la base Airtable existante.

## Phases

### P0 — Socle du projet

- Initialiser un projet Android Kotlin / Jetpack Compose.
- Ajouter une architecture claire : UI Compose, logique métier, accès Airtable, persistance locale.
- Définir le design system sombre : palette sémantique, typographie, composants, états et prévisualisations Compose.
- Prévoir les variantes debug/release et ignorer les fichiers de secrets/configuration locale.
- Ajouter un contrôle qualité minimal : formatage, tests unitaires et build APK de debug.

**Terminé quand** : l’application s’installe, s’ouvre et le build est reproductible.

### P1 — Configuration Airtable et test de connexion

- Créer l’écran Réglages : token, Base ID, table, champs et valeurs par défaut.
- Stocker le token dans le coffre-fort Android.
- Vérifier la configuration en lisant la structure de la table Airtable et afficher des erreurs compréhensibles.
- Ajouter une action de test qui crée un enregistrement de démonstration uniquement après confirmation.

**Terminé quand** : l’utilisateur peut configurer la vraie table Airtable depuis son téléphone et valider la connexion.

### P2 — Partage Spotify vers Rekordbot

- Déclarer Rekordbot Mobile parmi les cibles du menu **Partager** Android.
- Parser les liens et URI Spotify pour extraire le Spotify Track ID.
- Extraire titre et artiste depuis le texte partagé lorsqu’ils sont présents.
- Ouvrir un écran d’aperçu : titre, artiste, lien Spotify et Track ID modifiables.
- Gérer les partages incomplets sans créer de doublon silencieux.

**Terminé quand** : depuis Spotify, `Partager → Rekordbot` ouvre correctement le morceau dans l’application.

### P3 — Ajout Airtable immédiat

- Créer l’enregistrement Airtable depuis l’aperçu.
- Renseigner au minimum titre, artiste, lien Spotify, Track ID, source et statut.
- Afficher un résultat net : ajouté, échec détaillé ou ajout différé.
- Détecter les doublons à partir du Spotify Track ID avant création, selon une stratégie configurable : bloquer ou autoriser.

**Terminé quand** : un partage Spotify crée directement une entrée exploitable par Rekordbot PC.

### P4 — Mode hors-ligne fiable

- Mettre les ajouts en attente dans une base locale Room si Airtable ou le réseau est indisponible.
- Relancer l’envoi automatiquement avec WorkManager.
- Créer un écran de file d’attente : en attente, envoyé, échec, réessayer ou supprimer.
- Garantir l’idempotence : une même opération ne doit pas créer plusieurs morceaux après plusieurs tentatives.

**Terminé quand** : aucun partage n’est perdu sans réseau et la file se vide automatiquement au retour de connexion.

### P5 — Saisie DJ rapide

- Ajouter une saisie facultative d’énergie, mood, situation, DJs inspirants et commentaire avant envoi ou depuis la file.
- Utiliser les valeurs déjà employées dans Airtable, avec des raccourcis adaptés au téléphone et des tags colorés cohérents avec le design system.
- Ajouter un brouillon local pour compléter un titre plus tard.
- Préparer l’extension de note vocale et sa transcription, sans la rendre bloquante pour le MVP.

**Terminé quand** : le morceau peut être suffisamment contextualisé en quelques secondes, directement après l’écoute.

### P6 — Enrichissement des métadonnées

- Conserver le Spotify Track ID comme identifiant de référence.
- Ajouter un enrichissement optionnel des métadonnées depuis une source compatible.
- Ajouter le genre Soundcharts brut dans le champ texte Airtable dédié.
- Ne jamais écraser une valeur renseignée manuellement sans choix explicite.

**Terminé quand** : les données complémentaires peuvent être ajoutées sans perturber la capture rapide.

### P7 — Contrat de synchronisation avec Rekordbot PC

- Documenter les champs Airtable lus/écrits par Rekordbot PC et leurs statuts.
- Faire reconnaître en priorité Spotify Track ID puis ISRC, avant toute comparaison par titre/artiste.
- Renvoyer dans Airtable un état détaillé : à traiter, fichier trouvé, fichier absent, prêt pour Rekordbox, erreur.
- Conserver l’objectif : association avec des fichiers obtenus légalement, puis export Rekordbox XML.

**Terminé quand** : Rekordbot PC peut traiter les entrées de l’application sans ambiguïté.

### P8 — Finition et publication

- Ajouter icône, nom, écran d’accueil et messages d’erreur soignés.
- Vérifier le rendu du thème sombre et la lisibilité des actions bleues sur un appareil réel, y compris en extérieur.
- Tester le flux complet sur Spotify Android et sur un réseau intermittent.
- Produire un APK de test puis une release signée.
- Rédiger un guide court de configuration Airtable et de dépannage.

## Ordre recommandé

1. P0 — Socle
2. P1 — Airtable
3. P2 — Partage Spotify
4. P3 — Ajout direct
5. P4 — Hors-ligne
6. P5 — Saisie DJ
7. P7 — Synchronisation PC
8. P6 — Enrichissement
9. P8 — Publication

Le premier livrable utile est donc : **Spotify → Partager → Rekordbot Mobile → aperçu → ajout direct dans Airtable**.
