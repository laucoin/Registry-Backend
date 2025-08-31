# Registry (Backend)

<!-- ALL-CONTRIBUTORS-BADGE:START - Do not remove or modify this section -->
[![All Contributors](https://img.shields.io/badge/all_contributors-1-orange.svg?style=flat-square)](#contributors-)
<!-- ALL-CONTRIBUTORS-BADGE:END -->

## This repository 📖

This project was generated with [spring initializr](https://start.spring.io/), and it uses Spring, Gradle and Kotlin.

This application allows virtual registry management. This is a backend which one is called by the
frontend (https://gitlab.com/laucoin/registry-frontend.git).

Checkout the full document [here](documentation/README.md).

## How to install and use it? ⚙️

### Prerequisites

Install [Java 21 or later](https://www.oracle.com/fr/java/technologies/downloads/#java21)

### Build and run locally

1. Clone this repository with:
    ```shell
    git clone https://gitlab.com/laucoin/registry-backend.git
    ```
   OR
    ```shell
    git clone git@gitlab.com:laucoin/registry-backend.git
    ```
2. Move into the project directory
    ```shell
    cd registry-backend/
    ```
3. Set up your local environment with a
    ```shell
    docker-compose pull
    ```
   and
    ```shell
    docker-compose up -d
    ```
4. Configure Keycloak
    - Go to http://localhost:8080/admin/ and login with the following credentials:
        - Username: `admin`
        - Password: `admin`
    - Import realm from `docker-volumes/keycloak/realm/ne.json`
    - Create all users you need
5. Setup configuration file
   > Please create an `src/main/resources/application-*-local.yml` file and add the following config (for local refer
   > to [docker-compose.yml](docker-compose.yml) for the replacement)

   ```
   -Dspring.profiles.active=<organization> # For example: laucoin
   -Dregistry.datasource.schemas=<database-schemas> # For example: public
   -Dregistry.datasource.base-url=<database-url> # For example: localhost:5432 (Without http(s)://)
   -Dregistry.datasource.database=<database-name> # For example: postgres
   -Dregistry.datasource.username=<database-username> # For example: postgres
   -Dregistry.datasource.password=<database-username> # For example: postgres
   -Dexternal.keycloak.base-url=<oidc-provider-base-url> # For example: http://localhost:8080 (With http(s)://)
   -Dexternal.keycloak.realm=<oidc-provider-realm> # For example: laucoin
   -Dexternal.keycloak.client-id=<oidc-provider-client-id> # For example: registry
   -Dexternal.keycloak.client-secret=<oidc-provider-client-secret> # For example: XXXX
   -Dexternal.keycloak.swagger.client-id=<oidc-provider-client-id> # For example: registry
   -Dregistry.server.logging-level=DEBUG # Or INFO, WARN, ERROR, TRACE, FATAL (avoid using DEBUG for production)
   -Dregistry.server.port=<port> # Commonly use 8081 (because docker compose use 8080 for the keycloak instance)
   -Dregistry.feature.documentation.enabled=false # true only for development
   -Dexternal.frontend.base-url=<frontend-base-url> # For example: http://localhost:4200 (With http(s)://)
   ```
6. Enjoy the following commands 🎉

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

It removes the buildDir folder, thus cleaning everything including leftovers from previous builds which are no longer relevant.

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

WARNING :

- Any development must be done on a separate branch.

If you have more question, please have a look
on [contributing file](https://gitlab.com/laucoin/global-readme/-/blob/main/CONTRIBUTING.md)

## Contributors 🧑‍💻

Thanks goes to these wonderful people ([emoji key](https://allcontributors.org/docs/en/emoji-key)):

<table>
  <tbody>
    <tr>
      <td><div style='text-align: center'><a href="https://laucoin.fr"><img src="https://gitlab.com/uploads/-/system/user/avatar/4656880/avatar.png?width=400" width="100px;" alt="Luc AUCOIN"/><br /><sub><b>Luc AUCOIN</b></sub></a><br /><a href="https://gitlab.com/laucoin/registry-backend/commits?author=laucoin" title="Code">💻</a> <a href="https://gitlab.com/laucoin/registry-backend/commits?author=laucoin" title="Documentation">📖</a> <a href="#" title="Maintenance">🚧</a> <a href="#" title="Project Management">📆</a> <a href="https://gitlab.com/laucoin/registry-backend/commits?author=laucoin" title="Tests">⚠️</a></div></td>
    </tr>
  </tbody>
</table>

This project follows the [all-contributors](https://github.com/all-contributors/all-contributors) specification.
Contributions of any kind welcome!
