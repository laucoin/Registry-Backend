# Registry (Backend)

[![Build](https://github.com/laucoin/Registry-Backend/actions/workflows/release.yml/badge.svg)](https://github.com/laucoin/Registry-Backend/actions/workflows/release.yml)
[![Pull Request](https://github.com/laucoin/Registry-Backend/actions/workflows/pull-request.yml/badge.svg)](https://github.com/laucoin/Registry-Backend/actions/workflows/pull-request.yml)
[![CodeQL](https://github.com/laucoin/Registry-Backend/actions/workflows/codeql.yml/badge.svg)](https://github.com/laucoin/Registry-Backend/actions/workflows/codeql.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?logo=gradle&logoColor=white)](https://gradle.org)
[![JDK](https://img.shields.io/badge/JDK-25-437291?logo=openjdk&logoColor=white)](https://openjdk.org)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-4-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

## Ce dépôt 📖

Ce projet a été généré avec [spring initializr](https://start.spring.io/) et utilise Spring, Gradle et Kotlin.

Cette application permet la gestion d'un registre virtuel. Il s'agit du backend appelé par le frontend.

Consultez la documentation complète [ici](https://doc.laucoin.fr/registry).

Dépôts liés :

- [Frontend](https://github.com/laucoin/Registry-Frontend.git)
- [Tests E2E](https://github.com/laucoin/Registry-E2E.git)

## Comment l'installer et l'utiliser ? ⚙️

### Prérequis

Installer [Java 25 ou supérieur](https://www.oracle.com/fr/java/technologies/downloads/#java25)

### Construire et lancer en local

##### Procédure

1. Cloner ce dépôt avec :
    ```shell
    git clone https://github.com/laucoin/Registry-Backend.git
    ```
   OU
    ```shell
    git clone git@github.com:laucoin/Registry-Backend.git
    ```
2. Se placer dans le répertoire du projet
    ```shell
    cd Registry-Backend/
    ```
3. Démarrer en arrière-plan les conteneurs de dépendances (PostgreSQL, Authentik, …) :
    ```shell script
    cp local-dev/.example.env local-dev/.env
    # Éditer local-dev/.env pour définir les secrets (PG_PASS, AU_SECRET_KEY, etc.)
    docker compose -f local-dev/compose.yml up -d
    ```
   Authentik applique automatiquement [`local-dev/authentik/blueprints/registry.yaml`](local-dev/authentik/blueprints/registry.yaml)
   au démarrage, ce qui provisionne les clients OAuth2 du backend et six comptes de test (`administrator`,
   `coordinator`, `participant`, `blocked-user`, `blocked-profile`, `unverified`, tous en
   `*@sgdf.fr`), partageant le mot de passe `AU_PASS` (`dev123` par défaut).
4. Lancer le backend avec le profil Spring `local` actif pour que sa propre base reçoive les lignes de seed
   correspondantes (voir [`R__seed_dev_dataset.sql`](src/main/resources/db/seed/R__seed_dev_dataset.sql)) — sans lui, ces
   comptes Authentik n'ont rien à quoi se rattacher à la première connexion :
    ```shell script
    SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
    ```
5. Profitez des commandes suivantes 🎉

##### Configuration

L'application lit sa configuration depuis des variables d'environnement. Une variable avec une valeur par défaut peut
être omise ; une variable sans valeur par défaut est **requise** — `application.yml` la déclare comme un placeholder sans
repli, donc une valeur manquante fait échouer bruyamment le démarrage au lieu de démarrer silencieusement sur une valeur
non voulue.

**Source de données**

| Variable              | Défaut    | Description                                                                               |
|-----------------------|------------|-------------------------------------------------------------------------------------------|
| `DATASOURCE_BASE_URL` | *requis* | Hôte et port de l'instance PostgreSQL, sans schéma. Par exemple : `localhost:5432` |
| `DATASOURCE_DATABASE` | `registry` | Nom de la base de données                                                                             |
| `DATASOURCE_SCHEMA`   | `public`   | Schéma dans lequel travaillent les migrations et l'application                                         |
| `DATASOURCE_USERNAME` | `registry` | Utilisateur de la base de données                                                                             |
| `DATASOURCE_PASSWORD` | *requis* | Mot de passe de la base de données — un secret, voir ci-dessous                                                   |
| `DATASOURCE_LOCK_TIMEOUT_MS` | `5000` | Attente maximale, en millisecondes, d'un verrou de ligne (`SELECT … FOR UPDATE`) avant l'échec de la requête avec un `409 CONCURRENT_UPDATE_CONFLICT` |
| `DATASOURCE_POOL_INITIAL_SIZE` | `10` | Nombre de connexions R2DBC ouvertes au démarrage |
| `DATASOURCE_POOL_MAX_SIZE` | `30` | Nombre maximal de connexions R2DBC du pool |
| `DATASOURCE_POOL_MAX_IDLE_TIME` | `30m` | Durée d'inactivité au-delà de laquelle une connexion du pool est fermée |
| `DATASOURCE_POOL_MAX_ACQUIRE_TIME` | `5s` | Attente maximale d'une connexion du pool avant l'échec de la requête |

**Fournisseur d'identité (OIDC)**

À la première connexion, un utilisateur déjà présent en base n'est rattaché à son compte OIDC que si son `oidc_id` est vide et si le jeton porte `email_verified: true` ; sinon la connexion est refusée (`409`). `GET /api/v2/authentication/login/uri` accepte un paramètre optionnel `state`, repris tel quel dans l'URI d'autorisation : le client le génère et le vérifie au retour.

| Variable                    | Défaut    | Description                                                                                |
|-----------------------------|------------|--------------------------------------------------------------------------------------------|
| `IDP_JWKS_URI`              | *requis* | Par exemple : `http://localhost:9000/application/o/registry/jwks`                           |
| `IDP_AUTHORIZATION_URI`     | *requis* | Par exemple : `http://localhost:9000/application/o/authorize`                               |
| `IDP_TOKEN_URI`             | *requis* | Par exemple : `http://localhost:9000/application/o/token`                                   |
| `IDP_END_SESSION_URI`       | *requis* | Par exemple : `http://localhost:9000/application/o/registry/end-session`                    |
| `IDP_REVOCATION_URI`        | *requis* | Endpoint de révocation de token RFC 7009, appelé de serveur à serveur à la déconnexion pour invalider immédiatement le token d'accès/de rafraîchissement au lieu de le laisser valide jusqu'à son expiration naturelle. Par exemple : `http://localhost:9000/application/o/revoke` |
| `IDP_PRIVATE_CLIENT_ID`     | *requis* | Client confidentiel sous lequel le backend s'authentifie lui-même. Par exemple : `registry`           |
| `IDP_PRIVATE_CLIENT_SECRET` | *requis* | Son secret — un secret, voir ci-dessous                                                           |
| `IDP_PUBLIC_CLIENT_ID`      | *requis* | Client public sous lequel Swagger UI s'authentifie (authorization code + PKCE). Par exemple : `registry-swagger` |
| `IDP_CONNECT_TIMEOUT_MILLIS` | `2000` | Délai de connexion TCP des appels sortants du backend vers l'IdP (échange de connexion, rafraîchissement, révocation) |
| `IDP_RESPONSE_TIMEOUT_MILLIS` | `5000` | Durée d'attente de la réponse de l'IdP avant échec ; sert aussi de délai pour obtenir une connexion du pool lorsque l'IdP est déjà saturé |
| `IDP_MAX_CONNECTIONS`       | `20`       | Nombre maximal de connexions simultanées vers l'IdP dans le pool de connexions partagé |
| `IDP_ALLOWED_REDIRECT_URIS` | *requis* | Liste autorisée, séparée par des virgules, des valeurs exactes de `redirectUri` acceptées par `/authentication/login/uri` et `/token` ; les entrées sont comparées exactement (pas de joker, un `/` final compte), les entrées vides sont ignorées et toute autre valeur est refusée avec un `400`. Par exemple : `http://localhost:4200/auth/callback` |
| `IDP_ALLOWED_LOGOUT_REDIRECT_URIS` | *requis* | Liste autorisée, séparée par des virgules, des valeurs exactes de `redirectUri` acceptées par `/authentication/logout/uri` (où l'utilisateur arrive après sa déconnexion) ; toute autre valeur est refusée avec un `400`. Par exemple : `http://localhost:4200` |

**Serveur**

| Variable                 | Défaut    | Description                                                                                                                                                                                    |
|--------------------------|------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `SERVER_PORT`            | `8081`     | Port de l'API. `9000` est pris en local par le conteneur du fournisseur d'identité                                                                                                                           |
| `SERVER_MANAGEMENT_PORT` | `8082`     | Port séparé servant Actuator (`/health`, `/prometheus`) et Swagger UI/la documentation OpenAPI, chacun conditionné par son indicateur de fonctionnalité ci-dessous. **Non authentifié** — voir l'avertissement ci-dessous                         |
| `REGISTRY_LOGGING_LEVEL` | `INFO`     | Ou `TRACE`, `DEBUG`, `WARN`, `ERROR` (éviter `DEBUG` en production)                                                                                                                             |
| `EXTERNAL_CORS_URLS`     | *requis* | Origines autorisées à appeler l'API, séparées par des virgules. Par exemple : `http://localhost:4200` — la seconde étant le port de management, nécessaire au *try it out* de Swagger lorsque la documentation est activée |
| `COOKIE_SECURE`          | `true`     | Drapeau `Secure` des cookies d'authentification `registry_access_token`/`registry_refresh_token`. À mettre à `false` pour le développement local en HTTP simple — le profil Spring `local` le fait déjà                    |
| `REGISTRY_AUTH_RATE_LIMIT_CAPACITY` | `10` | Nombre maximal d'appels `POST` par IP cliente et par fenêtre sur `/authentication/token` et `/token/refresh` — les seuls endpoints non authentifiés. En mémoire (par instance, remis à zéro au redémarrage) : limiter aussi le débit des endpoints publics au niveau du reverse proxy |
| `REGISTRY_AUTH_RATE_LIMIT_WINDOW_SECONDS` | `60` | Durée en secondes de la fenêtre de limitation ci-dessus |
| `REGISTRY_AUTH_RATE_LIMIT_MAX_TRACKED_CLIENTS` | `10000` | Nombre maximal d'IP clientes suivies simultanément par le limiteur ci-dessus ; les entrées les moins récemment utilisées sont évincées au-delà |
| `REGISTRY_MOVEMENT_FUTURE_TOLERANCE` | `2m` | Décalage d'horloge toléré entre un client et le serveur lorsqu'un mouvement est daté légèrement dans le futur ; une date plus éloignée est rejetée |
| `REGISTRY_CODEC_MAX_IN_MEMORY_SIZE` | `512KB` | Taille maximale d'un corps de requête mis en mémoire tampon ; les corps plus volumineux sont rejetés |
| `REGISTRY_UPDATE_TIMEOUT` | `10s` | Durée maximale d'une mise à jour protégée par `If-Match` ; au-delà, la transaction est annulée et la requête échoue avec un `408` |
| `REGISTRY_PRINCIPAL_CACHE_TTL_SECONDS` | `60` | Durée de vie, en secondes, du principal (utilisateur et autorités) mis en cache après la validation du JWT ; un changement de droits est visible au plus tard à l'expiration, ou immédiatement s'il passe par l'API |

> [!NOTE]
> L'application navigateur s'authentifie via deux cookies `HttpOnly`, `SameSite=Lax` (`registry_access_token`, `registry_refresh_token`)
> posés par `/api/v2/authentication/token` et `/token/refresh`, effacés par `/logout/uri`. L'en-tête `Authorization: Bearer`
> fonctionne toujours — il est vérifié en premier, le cookie en repli — conservé uniquement pour que le flux OAuth2
> « try it out » de Swagger UI continue de fonctionner ; aucun autre client ne devrait l'envoyer.

> [!WARNING]
> Le port de management n'a aucune authentification applicative : Spring Boot le sert depuis un serveur embarqué
> distinct auquel le filtre de sécurité de l'application ne s'applique jamais. Ne jamais l'exposer en dehors d'un réseau
> de confiance/interne — pas d'ingress public, isolé d'Internet par un pare-feu — quels que soient les indicateurs de
> fonctionnalité ci-dessous.

**Fonctionnalités**

| Variable                         | Défaut | Description                                                                              |
|----------------------------------|---------|------------------------------------------------------------------------------------------|
| `REGISTRY_DOCUMENTATION_ENABLED` | `false` | Swagger UI et les documents OpenAPI sur le port de management. `true` uniquement pour le développement |
| `REGISTRY_OBSERVABILITY_ENABLED` | `true`  | Sondes de santé et endpoint Prometheus sur le port de management                         |

#### Lancer l'application en mode dev

Vous pouvez lancer l'application en mode dev, qui permet le live coding, avec (ajouter les options de VM juste après) :

```shell script
./gradlew bootRun
```

#### Empaqueter et lancer l'application

##### Java VM (JVM)

L'application peut être empaquetée avec :

```shell script
./gradlew build
```

Cela produit le fichier `backend-<version>.jar` dans le répertoire `build/libs/`.

L'application est alors exécutable sur une JVM avec `java -jar build/libs/backend-<version>.jar`.

##### Native

L'application peut être empaquetée et lancée :

```shell script
./gradlew bootBuildImage
```

L'application est alors exécutable en tant qu'application native.

#### Nettoyer le projet

L'application peut être nettoyée avec :

```shell script
./gradlew clean
```

Cela supprime le dossier buildDir, nettoyant ainsi tout, y compris les restes de builds précédents devenus inutiles.

#### Aide complémentaire

##### Documentation de référence

Pour aller plus loin, consulter les sections suivantes :

* [Documentation officielle de Gradle](https://docs.gradle.org)
* [Guide de référence du plugin Gradle de Spring Boot](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/)
* [Créer une image OCI](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/#build-image)
* [Section Coroutines de la documentation de Spring Framework](https://docs.spring.io/spring/docs/6.1.8/spring-framework-reference/languages.html#coroutines)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.reactive)
* [Spring Data R2DBC](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#data.sql.r2dbc)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.security.oauth2.server)
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#howto.data-initialization.migration-tool.flyway)
* [Spring Security](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.security)

##### Guides

Les guides suivants illustrent concrètement l'utilisation de certaines fonctionnalités :

* [Construire un service web RESTful réactif](https://spring.io/guides/gs/reactive-rest-service/)
* [Accéder aux données avec R2DBC](https://spring.io/guides/gs/accessing-data-r2dbc/)
* [Sécuriser une application web](https://spring.io/guides/gs/securing-web/)
* [Spring Boot et OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authentifier un utilisateur avec LDAP](https://spring.io/guides/gs/authenticating-ldap/)

##### Liens complémentaires

Ces références complémentaires peuvent aussi vous aider :

* [Gradle Build Scans – analyses du build de votre projet](https://scans.gradle.com#gradle)
* [Page d'accueil de R2DBC](https://r2dbc.io)

## Contribuer 💻

La branche `main` contient le code de développement.

> [!WARNING]
> Tout développement doit être fait sur une branche séparée : chaque changement atteint `main` via une pull request.

Les workflows GitHub Actions sont la barrière de revue — une pull request doit être verte avant d'être fusionnée :

- **Pull Request** ([pull-request.yml](.github/workflows/pull-request.yml)) — exécute `./gradlew build` (tests inclus)
  et publie une image taguée par branche pour la revue.
- **Dependency Review** ([dependency-review.yml](.github/workflows/dependency-review.yml)) — bloque une pull request qui
  introduit des dépendances vulnérables.
- **CodeQL** ([codeql.yml](.github/workflows/codeql.yml)) — analyse statique java-kotlin sur les pull requests, les pushs vers
  `main` et selon un calendrier.
- **Release** ([release.yml](.github/workflows/release.yml), à la fusion dans `main`) — construit et pousse l'image DEV, puis
  Semantic Release dérive la version suivante des messages de commit, la tague et publie l'image de release ; un job de
  rétention élague les anciennes images. Les messages de commit doivent donc suivre
  [Conventional Commits](https://www.conventionalcommits.org/).
- **PR Cleanup** ([pr-cleanup.yml](.github/workflows/pr-cleanup.yml)) — supprime l'image de la branche du registre à la
  fermeture de la pull request.

Avant de contribuer, merci de lire la [documentation](https://doc.laucoin.fr/registry/) et notre
[code de conduite](CODE_OF_CONDUCT.md).

## Contributeurs 🧑‍💻

Merci à ces personnes formidables ([légende des emojis](https://allcontributors.org/en/reference/emoji-key/)) :

<!-- ALL-CONTRIBUTORS-LIST:START - Do not remove or modify this section -->
<!-- prettier-ignore-start -->
<!-- markdownlint-disable -->
<table>
  <tbody>
    <tr>
      <td align="center" valign="top" width="14.28%"><a href="https://doc.laucoin.fr/resume"><img src="https://avatars.githubusercontent.com/u/31480129?v=4?s=100" width="100px;" alt="Luc AUCOIN"/><br /><sub><b>Luc AUCOIN</b></sub></a><br /><a href="#projectManagement-laucoin" title="Project Management">📆</a> <a href="#ideas-laucoin" title="Ideas, Planning, & Feedback">🤔</a> <a href="https://github.com/laucoin/Registry-Backend/commits?author=laucoin" title="Code">💻</a> <a href="#maintenance-laucoin" title="Maintenance">🚧</a> <a href="#infra-laucoin" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/Usinouv"><img src="https://avatars.githubusercontent.com/u/13047412?v=4?s=100" width="100px;" alt="Usinouv"/><br /><sub><b>Usinouv</b></sub></a><br /><a href="#ideas-Usinouv" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/lvicainne"><img src="https://avatars.githubusercontent.com/u/1641160?v=4?s=100" width="100px;" alt="Louis VICAINNE"/><br /><sub><b>Louis VICAINNE</b></sub></a><br /><a href="#infra-lvicainne" title="Infrastructure (Hosting, Build-Tools, etc)">🚇</a> <a href="#ideas-lvicainne" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://github.com/ctruillet"><img src="https://avatars.githubusercontent.com/u/43933447?v=4?s=100" width="100px;" alt="Clément Truillet"/><br /><sub><b>Clément Truillet</b></sub></a><br /><a href="#ideas-ctruillet" title="Ideas, Planning, & Feedback">🤔</a></td>
      <td align="center" valign="top" width="14.28%"><a href="https://www.linkedin.com/in/c%C3%A9cile-crochon/"><img src="https://media.licdn.com/dms/image/v2/C4D03AQEWB-ofOcjZ7A/profile-displayphoto-shrink_800_800/profile-displayphoto-shrink_800_800/0/1626700975351?e=1789603200&v=beta&t=Vj9eWZiVoCro7Lg-L3ewYLOaB3lXejJ8NLNP-LiXkhk" width="100px;" alt="Cécile CROCHON"/><br /><sub><b>Cécile CROCHON</b></sub></a><br /><a href="#projectManagement-crochon" title="Project Management">📆</a></td>
    </tr>
  </tbody>
</table>

<!-- markdownlint-restore -->
<!-- prettier-ignore-end -->

<!-- ALL-CONTRIBUTORS-LIST:END -->

Ce projet suit la spécification [all-contributors](https://github.com/all-contributors/all-contributors).
Les contributions de toute nature sont les bienvenues !

Pour ajouter un contributeur, commenter une issue/PR avec
`@all-contributors please add @<username> for <contributions>` (bot), ou exécuter :

```shell script
npx --yes all-contributors-cli add <username> <contributions>
```
