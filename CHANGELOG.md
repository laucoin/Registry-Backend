## [1.16.0](https://github.com/laucoin/Registry-Backend/compare/1.15.0...1.16.0) (2026-09-22)

### 🧹 Améliorations

* **architecture:** Rendre plus lisibles les dossiers d'entrée et de sortie de l'infrastructure ([#55](https://github.com/laucoin/Registry-Backend/issues/55)) ([65ef791](https://github.com/laucoin/Registry-Backend/commit/65ef791805165e83fb29e0e502cf5c830d5f1d09))

## [1.15.0](https://github.com/laucoin/Registry-Backend/compare/1.14.0...1.15.0) (2026-09-20)

### 🏎️ Améliorations

* **r2dbc:** Corriger la taille du pool ([#54](https://github.com/laucoin/Registry-Backend/issues/54)) ([190134a](https://github.com/laucoin/Registry-Backend/commit/190134aa62c7ee0a1e3405274e7b5c733d97860b))

## [1.14.0](https://github.com/laucoin/Registry-Backend/compare/1.13.0...1.14.0) (2026-09-20)

### 🏎️ Améliorations

* **search:** Améliorer les performances de recherche ([#53](https://github.com/laucoin/Registry-Backend/issues/53)) ([5a1e8cf](https://github.com/laucoin/Registry-Backend/commit/5a1e8cf168340c93317a251b376f8d2875f35675))

## [1.13.0](https://github.com/laucoin/Registry-Backend/compare/1.12.0...1.13.0) (2026-09-15)

### 📦️ Autres

* **error:** Gérer l'erreur de compte de service et de rôle ([#48](https://github.com/laucoin/Registry-Backend/issues/48)) ([84addbe](https://github.com/laucoin/Registry-Backend/commit/84addbe6c8e75c933708831119bea38325bbecde))

### 🏎️ Améliorations

* **cardinality:** Arrêter de charger une liste entière pour un simple test de cardinalité ([#49](https://github.com/laucoin/Registry-Backend/issues/49)) ([2a74e98](https://github.com/laucoin/Registry-Backend/commit/2a74e98a8256e5bfa13c9a344901d127a4880ce3))
* **purge:** Ajouter la concurrence de purge ([#50](https://github.com/laucoin/Registry-Backend/issues/50)) ([4768824](https://github.com/laucoin/Registry-Backend/commit/4768824560b97e907a2e8d62a0dce6774f466a6c))
* **repository:** Éviter la requête SQL en double pour la recherche et la pagination grâce à COUNT(*) OVER() ([#51](https://github.com/laucoin/Registry-Backend/issues/51)) ([736684f](https://github.com/laucoin/Registry-Backend/commit/736684f1d3be75ee9d867505039ed9c287eea65f))

### 🐛 Corrections de bugs

* **birthday:** L'anniversaire n'était renvoyé que si l'année correspondait aussi ^^' ([#52](https://github.com/laucoin/Registry-Backend/issues/52)) ([768d3ce](https://github.com/laucoin/Registry-Backend/commit/768d3ce53f8d10e16112985c43964cbf41fafe32))

## [1.12.0](https://github.com/laucoin/Registry-Backend/compare/1.11.0...1.12.0) (2026-09-14)

### 🏎️ Améliorations

* **movement:** Améliorer la vérification des données de création d'un mouvement ([#47](https://github.com/laucoin/Registry-Backend/issues/47)) ([d289ab8](https://github.com/laucoin/Registry-Backend/commit/d289ab8ca090faef50358b33c8a2ea90ace2eafe))

## [1.11.0](https://github.com/laucoin/Registry-Backend/compare/1.10.0...1.11.0) (2026-09-14)

### 📦️ Autres

* **IdP:** Ajouter un timeout sur les appels à l'IdP ([#46](https://github.com/laucoin/Registry-Backend/issues/46)) ([c2f1bc0](https://github.com/laucoin/Registry-Backend/commit/c2f1bc037235d67a527a50022e913127c2fb6610))

## [1.10.0](https://github.com/laucoin/Registry-Backend/compare/1.9.0...1.10.0) (2026-09-14)

### 🏎️ Améliorations

* **auth:** Ajouter un cache interne pour l'authentification afin de réduire le temps de réponse ([#45](https://github.com/laucoin/Registry-Backend/issues/45)) ([c26e4b4](https://github.com/laucoin/Registry-Backend/commit/c26e4b4c13c9ca40137af34b81df12f7da933b09))

## [1.9.0](https://github.com/laucoin/Registry-Backend/compare/1.8.0...1.9.0) (2026-09-14)

### 📝 Documentation

* **changelog:** réconcilier CHANGELOG.md et version.properties avec l'historique réel des releases ([b78f7ac](https://github.com/laucoin/Registry-Backend/commit/b78f7ac9a8123648ed92fc23ddc8e008910e1cf9)), closes [#39](https://github.com/laucoin/Registry-Backend/issues/39) [#40-43](https://github.com/laucoin/Registry-Backend/issues/40-43)

## [1.8.0](https://github.com/laucoin/Registry-Backend/compare/1.7.0...1.8.0) (2026-09-13)

### 🚀 Fonctionnalités

* **auth:** Ajouter la possibilité de se connecter avec un cookie ([#41](https://github.com/laucoin/Registry-Backend/issues/41)) ([e712169](https://github.com/laucoin/Registry-Backend/commit/e7121698d21b64a2f61218c1bd2505c7deada381))
* **management:** Exposer les informations sensibles sur un port dédié 8082 ([#40](https://github.com/laucoin/Registry-Backend/issues/40)) ([2902582](https://github.com/laucoin/Registry-Backend/commit/2902582e757f41bf23216c68ef99655eb59bfebf))

### 🐛 Corrections de bugs

* **logout:** améliorer le schéma de déconnexion (révocation auprès de l'IdP) ([#42](https://github.com/laucoin/Registry-Backend/issues/42)) ([0182b2f](https://github.com/laucoin/Registry-Backend/commit/0182b2fecca34038377a6ba001637c2113963796))

### 📦️ Autres

* **security:** En-tête de sécurité, journalisation des e-mails/nom de classe et autres correctifs de sécurité ([#43](https://github.com/laucoin/Registry-Backend/issues/43)) ([54c8388](https://github.com/laucoin/Registry-Backend/commit/54c83888b319799a0481b305862c4e7efd752c55))

## [1.7.0](https://github.com/laucoin/Registry-Backend/compare/1.6.1...1.7.0) (2026-09-11)

### 🧹 Améliorations

* **idp:** renommer keycloak en IDP et petit correctif de NPE dans le mapper ([#39](https://github.com/laucoin/Registry-Backend/issues/39)) ([df90ca4](https://github.com/laucoin/Registry-Backend/commit/df90ca41143759651572def140c51c5d3bf31172))

## [1.6.1](https://github.com/laucoin/Registry-Backend/compare/1.6.0...1.6.1) (2026-09-05)

### 🐛 Corrections de bugs

* **purge:** Rejeter un seuil de purge fixé dans le futur ([#21](https://github.com/laucoin/Registry-Backend/issues/21)) ([b152100](https://github.com/laucoin/Registry-Backend/commit/b152100ba4f1ad393dd39d6a71dca7899c4d173e))

## [1.6.0](https://github.com/laucoin/Registry-Backend/compare/1.5.0...1.6.0) (2026-09-05)

### 📦️ Autres

* **ci:** Aligner la configuration des conventional commits du backend et du frontend ([#18](https://github.com/laucoin/Registry-Backend/issues/18)) ([e769bd9](https://github.com/laucoin/Registry-Backend/commit/e769bd9cb6e71a1c159ab0feb650f857211af375))

## [1.5.0](https://github.com/laucoin/Registry-Backend/compare/1.4.2...1.5.0) (2026-09-04)

### 📦️ Autres

* **AI:** Ajouter l'AGENTS.md généré à partir de la documentation ([#17](https://github.com/laucoin/Registry-Backend/issues/17)) ([57c8bc2](https://github.com/laucoin/Registry-Backend/commit/57c8bc2859225992bd993564740ba48afb5e9d81))

## [1.4.2](https://github.com/laucoin/Registry-Backend/compare/1.4.1...1.4.2) (2026-08-30)

### 🐛 Corrections de bugs

* **communication:** Conditionner les endpoints de recherche à leur option de projet ([#15](https://github.com/laucoin/Registry-Backend/issues/15)) ([c2b877b](https://github.com/laucoin/Registry-Backend/commit/c2b877baddf9fe8b40ae60d9ea1eadd53f5f55df)), closes [#projectId](https://github.com/laucoin/Registry-Backend/issues/projectId)

## [1.4.1](https://github.com/laucoin/Registry-Backend/compare/1.4.0...1.4.1) (2026-08-30)

### 🐛 Corrections de bugs

* **participant:** Corriger le filtre majeur/mineur inversé ([#14](https://github.com/laucoin/Registry-Backend/issues/14)) ([8c9a1b7](https://github.com/laucoin/Registry-Backend/commit/8c9a1b7bfa9dceab446467119375a9f8f8964bba))

## [1.4.0](https://github.com/laucoin/Registry-Backend/compare/1.3.0...1.4.0) (2026-08-30)

### 📦️ Autres

* **AI:** Ajouter le skill de comparaison avec la documentation ([#13](https://github.com/laucoin/Registry-Backend/issues/13)) ([42a4193](https://github.com/laucoin/Registry-Backend/commit/42a419325d43db449ab59da7451e561f632ae900))

## [1.3.0](https://github.com/laucoin/Registry-Backend/compare/1.2.0...1.3.0) (2026-08-30)

### 📦️ Autres

* **PR:** Ajouter le modèle de pull request ([#12](https://github.com/laucoin/Registry-Backend/issues/12)) ([7d840fa](https://github.com/laucoin/Registry-Backend/commit/7d840fa6978d5d8c0c05ce8e09aed6fc26890fa9))

## [1.2.0](https://github.com/laucoin/Registry-Backend/compare/1.1.0...1.2.0) (2026-08-30)

### 🚀 Fonctionnalités

* **readme:** Améliorer le contenu de README.md ([#11](https://github.com/laucoin/Registry-Backend/issues/11)) ([73e1ab7](https://github.com/laucoin/Registry-Backend/commit/73e1ab7a91ded2de52232166ba7e3f507bc979c3))

## [1.1.0](https://github.com/laucoin/Registry-Backend/compare/1.0.2...1.1.0) (2026-07-24)

### 👷 Automatisation

* **distroless:** Utiliser une image distroless pour le Dockerfile ([a62a36c](https://github.com/laucoin/Registry-Backend/commit/a62a36cc2f0b09d73ad325db55d5973d5951e381))
* **rework:** retravailler la CI et ajouter le hotfix ([2c6d1d8](https://github.com/laucoin/Registry-Backend/commit/2c6d1d8e3aeeec41e9e14ebc1d808fe8be4c13d5))

### 📦️ Autres

* corriger une erreur ([97a819c](https://github.com/laucoin/Registry-Backend/commit/97a819c64658ff4107fe0f51805c09a9a28adc62))
* **update:** Spring Boot 4, Java 25 et quelques mises à jour de bibliothèques. ([32eba59](https://github.com/laucoin/Registry-Backend/commit/32eba59a83ed9e17dfc7e8a6fdd1ffec7590c872))
* **version:** mettre à jour les versions ([c4d893e](https://github.com/laucoin/Registry-Backend/commit/c4d893ed8b31b02bd0a7337d7215859a39adad8b))

### 🚀 Fonctionnalités

* **devContainer:** commencer à utiliser et configurer le dev container ([6c1e2a8](https://github.com/laucoin/Registry-Backend/commit/6c1e2a86cd5239177a8fcccafa40b72d2156e408))
* **github:** migrer de GitLab vers GitHub ([#1](https://github.com/laucoin/Registry-Backend/issues/1)) ([89b5a60](https://github.com/laucoin/Registry-Backend/commit/89b5a60b6c414bea022d2157d7e650aa2924231d))

### 🧹 Améliorations

* **clean:** nettoyer du code ([d680070](https://github.com/laucoin/Registry-Backend/commit/d680070097c8e2caf184f6d62bc1ea71f0be9581))
* recommandations globales pour se simplifier la vie ([0288749](https://github.com/laucoin/Registry-Backend/commit/0288749d7991b309ed2e077d47de6e9adef9eb57))
* **hexa:** changer le nommage des ports et des dossiers ([96c311d](https://github.com/laucoin/Registry-Backend/commit/96c311d89198f5005ecb157a1633e6953533bcaa))

### 📝 Documentation

* ajout de la documentation (elle était dans un autre dépôt) ([c8bc1e7](https://github.com/laucoin/Registry-Backend/commit/c8bc1e726443c19d13a970107b0e3c6967d4e729))

### 🧪 Tests

* ajout de tests pour atteindre 80 % ([82b7c21](https://github.com/laucoin/Registry-Backend/commit/82b7c2152196cb5fae6799db05f4a09c8e4dbe14))
