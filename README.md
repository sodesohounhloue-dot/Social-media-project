# Mosaïc

Maquette responsive d'un réseau social francophone, construite en HTML, CSS et JavaScript natifs. Aucun serveur ni gestionnaire de paquets n'est nécessaire.

## Lancer

Ouvrir `index.html` dans un navigateur. Les photographies de démonstration sont chargées depuis Unsplash et nécessitent une connexion internet.

## Fonctionnalités de démonstration

- Fil d'actualité, stories, recherche, filtres et navigation entre les vues
- Publications, réactions, commentaires, enregistrements et suggestions d'abonnement
- Politique de confidentialité et conditions d'utilisation accessibles sur ordinateur et mobile
- Mise en page responsive avec navigation mobile
- Base IndexedDB locale avec les classes `User` et `Admin`, un sélecteur de profils et un tableau de bord administrateur distinct
- Gestion de démonstration des états de compte et revue de signalements depuis l'espace admin

Les comptes de démonstration et le profil actif sont conservés dans IndexedDB sur l'appareil. Les publications, réactions et commentaires restent temporaires; aucun backend n'est présent. Le changement de profil n'est pas une authentification sécurisée: une vraie application doit vérifier les rôles et permissions côté serveur. Les données sont fictives et les textes légaux sont à adapter avant la mise en production.
