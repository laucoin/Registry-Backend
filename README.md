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

## This repository 📖

This project was generated with [spring initializr](https://start.spring.io/), and it uses Spring, Gradle and Kotlin.

This application allows virtual registry management. This is a backend which one is called by the frontend.

Checkout the full documentation [here](https://doc.laucoin.fr/registry).

Linked repositories:

- [Frontend](https://github.com/laucoin/Registry-Frontend.git)
- [E2E tests](https://github.com/laucoin/Registry-E2E.git)

## How to install and use it? ⚙️

### Prerequisites

Install [Java 25 or later](https://www.oracle.com/fr/java/technologies/downloads/#java25)

### Build and run locally

##### Procedure

1. Clone this repository with:
    ```shell
    git clone https://github.com/laucoin/Registry-Backend.git
    ```
   OR
    ```shell
    git clone git@github.com:laucoin/Registry-Backend.git
    ```
2. Move into the project directory
    ```shell
    cd Registry-Backend/
    ```
3. Start dependency containers (PostgreSQL, Authentik, …) in the background:
    ```shell script
    cp local-dev/.example.env local-dev/.env
    # Edit local-dev/.env to set secrets (PG_PASS, AU_SECRET_KEY, etc.)
    docker compose -f local-dev/compose.yml up -d
    ```
4. Enjoy the following commands 🎉

##### JAVA_OPTS

```
-Dregistry.datasource.schemas=<database-schemas> # For example: public
-Dregistry.datasource.base-url=<database-url> # For example: localhost:5432 (Without http(s)://)
-Dregistry.datasource.database=<database-name> # For example: postgres
-Dregistry.datasource.username=<database-username> # For example: postgres
-Dregistry.datasource.password=<database-username> # For example: postgres
-Dexternal.oidc.jwks-uri=<oidc-jwks-uri> # For example: http://localhost:9000/application/o/registry/jwks
-Dexternal.oidc.issuer=<oidc-issuer> # REQUIRED. The `iss` claim every accepted token must carry — mind the trailing slash. For example: http://localhost:9000/application/o/registry/
-Dexternal.oidc.audiences=<oidc-audiences> # Optional, comma-separated. Defaults to the backend and Swagger client ids. Accepted values of the `aud` claim
-Dexternal.oidc.authorization-uri=<oidc-authorization-endpoint> # For example: http://localhost:9000/application/o/authorize
-Dexternal.oidc.token-uri=<oidc-token-endpoint> # For example: http://localhost:9000/application/o/token
-Dexternal.oidc.end-session-uri=<oidc-end-session-endpoint> # For example: http://localhost:9000/application/o/registry/end-session
-Dexternal.oidc.client-id=<oidc-provider-client-id> # For example: registry
-Dexternal.oidc.client-secret=<oidc-provider-client-secret> # For example: XXXX
-Dexternal.oidc.swagger.client-id=<oidc-provider-client-id> # For example: registry
-Dregistry.server.logging-level=DEBUG # Or INFO, WARN, ERROR, TRACE, FATAL (avoid using DEBUG for production)
-Dregistry.server.port=<port> # Commonly use 8081 (because docker compose use 9000 for the identity provider instance)
-Dregistry.feature.documentation.enabled=false # true only for development
-Dexternal.cors.urls=<cors-urls> # For example: http://localhost:4200 (With http(s):// separate with "," if multiple)
```

### Local identity provider

`local-dev/authentik/blueprints/registry.yaml` provisions everything the identity provider needs on
startup — the OAuth2 client, its application, two scope mappings and six test personas — so a fresh
`docker compose up` yields a working login with no clicking through the Authentik UI. Authentik
re-applies the blueprint whenever the file changes, matching the provider on `client_id` and users on
`username`, so it updates in place instead of duplicating.

> [!WARNING]
> Applying the blueprint sets the password of all six personas to `AU_DEV_PASSWORD`.

Four settings in it are load-bearing and easy to get wrong when configuring by hand:

- **`sub_mode: user_uuid`** — the backend reads `sub` straight into `UUID.fromString`. Authentik's
  default (`hashed_user_id`) is not a UUID and every sign-in fails.
- **`signing_key`** — without it Authentik signs with HS256 and serves an empty JWKS, so the resource
  server cannot validate anything.
- **`grant_types`** — explicit since Authentik 2026.5. The field defaults to an empty list, which
  refuses every token request with *Invalid grant_type for provider*.
- **The two local scope mappings.** Authentik stores a single `name` field, so its stock `profile`
  mapping puts the *full* name in `given_name` and emits no `family_name` — while
  `TokenConverterService` reads both. The local mappings read them from user attributes instead, and
  drive `email_verified` from an attribute rather than hardcoding `False`.

#### Test personas

| Username | Name | Purpose |
| -------- | ---- | ------- |
| `administrator` | Jane SMITH | Platform administration |
| `coordinator` | John DOE | Project-level management |
| `participant` | Charles PINA | Ground-level staff |
| `blocked-user` | Aliyah NIELSEN | Blocked in Registry — the provider still issues a token, the rejection happens at JWT conversion |
| `blocked-profile` | Emil BRADFORD | Account fine, project profile blocked |
| `unverified` | Nina VOGEL | Carries `email_verified: false` |

Decoding a real token from this stack gives the values the backend expects:

| Claim | Value | Note |
| ----- | ----- | ---- |
| `iss` | `http://localhost:9000/application/o/registry/` | Trailing slash included — `external.oidc.issuer` must match exactly |
| `aud` | `registry` | A bare string, not an array |
| `sub` | a UUID | Thanks to `sub_mode: user_uuid` |

> [!WARNING]
> `docker-entrypoint-initdb.d` scripts run **only on an empty data directory**. If
> `local-dev/postgres/data` already exists from an earlier run, the `registry` and `authentik`
> databases are never created and Authentik loops on `password authentication failed`. Recreating
> just the `authentik` database is enough to recover, and leaves the `registry` data alone —
> the blueprint reprovisions the identity provider from scratch.

> [!IMPORTANT]
> **Secrets** (`registry.datasource.password`, `external.oidc.client-secret`) must
> be passed as JVM options or environment variables — **never committed** to any
> `application*.yml`. The profile files reference them as placeholders so
> startup fails loudly if they are missing.

#### Running the application in dev mode

You can run your application in dev mode that enables live coding using (please add the VM options just after):

```shell script
./gradlew bootRun
```

#### Packaging and running the application

##### Java VM (JVM)

The application can be packaged using:

```shell script
./gradlew build
```

It produces the `backend-<version>.jar` file in the `build/libs/` directory.

The application is now runnable in a JVM using `java -jar build/libs/backend-<version>.jar`.

##### Native

The application can be packaged and run:

```shell script
./gradlew bootBuildImage
```

The application is now runnable using as native application.

#### Clean project

The application can be cleaned using:

```shell script
./gradlew clean
```

It removes the buildDir folder, thus cleaning everything including leftovers from previous builds which are no longer
relevant.

#### Further help

##### Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/3.3.0/gradle-plugin/reference/html/#build-image)
* [Coroutines section of the Spring Framework Documentation](https://docs.spring.io/spring/docs/6.1.8/spring-framework-reference/languages.html#coroutines)
* [Spring Reactive Web](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.reactive)
* [Spring Data R2DBC](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#data.sql.r2dbc)
* [OAuth2 Resource Server](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.security.oauth2.server)
* [Flyway Migration](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#howto.data-initialization.migration-tool.flyway)
* [Spring Security](https://docs.spring.io/spring-boot/docs/3.3.0/reference/htmlsingle/index.html#web.security)

##### Guides

The following guides illustrate how to use some features concretely:

* [Building a Reactive RESTful Web Service](https://spring.io/guides/gs/reactive-rest-service/)
* [Accessing data with R2DBC](https://spring.io/guides/gs/accessing-data-r2dbc/)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)

##### Additional Links

These additional references should also help you:

* [Gradle Build Scans – insights for your project's build](https://scans.gradle.com#gradle)
* [R2DBC Homepage](https://r2dbc.io)

## Contributing 💻

The `main` branch contain the development code.

> [!WARNING]
> Any development must be done on a separate branch: every change reaches `main` through a pull request.

The GitHub Actions workflows are the review gate — a pull request must be green before merge:

- **Pull Request** ([pull-request.yml](.github/workflows/pull-request.yml)) — runs `./gradlew build` (tests included)
  and publishes a branch-tagged image for review.
- **Dependency Review** ([dependency-review.yml](.github/workflows/dependency-review.yml)) — blocks a pull request that
  introduces vulnerable dependencies.
- **CodeQL** ([codeql.yml](.github/workflows/codeql.yml)) — java-kotlin static analysis on pull requests, pushes to
  `main` and on a schedule.
- **Release** ([release.yml](.github/workflows/release.yml), on merge to `main`) — builds & pushes the DEV image, then
  Semantic Release derives the next version from the commit messages, tags it and publishes the release image; a
  retention job prunes old images. Commit messages must therefore follow
  [Conventional Commits](https://www.conventionalcommits.org/).
- **Hotfix** ([hotfix.yml](.github/workflows/hotfix.yml)) — pushing a tag matching `*-hotfix-*` (branched off an
  existing release tag) builds & pushes an isolated hotfix image, outside Semantic Release.
- **PR Cleanup** ([pr-cleanup.yml](.github/workflows/pr-cleanup.yml)) — deletes the branch image from the registry when
  the pull request closes.

Before contributing, please read the [documentation](https://doc.laucoin.fr/registry/) and our
[code of conduct](CODE_OF_CONDUCT.md).

## Contributors 🧑‍💻

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/en/reference/emoji-key/)):

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

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification.
Contributions of any kind welcome!

To add a contributor, either comment on an issue/PR with
`@all-contributors please add @<username> for <contributions>` (bot), or run:

```shell script
npx --yes all-contributors-cli add <username> <contributions>
```
