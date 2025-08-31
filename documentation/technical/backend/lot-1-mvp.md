---
layout:
  title:
    visible: true
  description:
    visible: false
  tableOfContents:
    visible: true
  outline:
    visible: true
  pagination:
    visible: true
---

# 🔵 Lot 1 (MVP)

## Organization

Is Registry take a commercial way, it could be interessant to create mono instance to reduce exploitation cost. But for the moment, we will create multi-instance backend but create an organization configuration level To make a futur evolution easier.

An example of organization configuration:

```yaml
registry:
  information:
    name: "Registry"
    description: "This application allows virtual registry management. This is a backend which one is called by the frontend."
  support:
    firstName: Luc
    lastName: AUCOIN
    email: luc.aucoin1998@gmail.com
  security:
    auth:
      keycloak-url: ${external.security.auth.base-url}/realms/${external.security.auth.realm}/protocol/openid-connect
      auth-url: ${external.security.auth.keycloak-url}/auth
      token-url: ${external.security.auth.keycloak-url}/token
      certs-url: ${external.security.auth.keycloak-url}/certs
      claim-keys:
        user-id: "sub"
        email: "email"
        first-name: "given_name"
        last-name: "family_name"
    role-definition:
      user-authority:
        BUSINESS_ROLE:
          - […] Permission
      event-authority:
        BUSINESS_EVENT_ROLE:
          - […] Permission
      registration-authority:
        BUSINESS_REGISTRATION_ROLE:
          - […] Permission
  feature:
    option:
      available:
        - […] Option
    movement:
      draft:
        suggestion-threshold: 30 # in minute
```

## Framework

The following part describe the frame to implement features.

```mermaid
classDiagram
    namespace Domain {
        class HistoryUserModel {
            +UUID id
            +String firstName
            +String lastName
            +String email
            +Boolean visible
        }
        class HistoryModel {
            +LocalDateTime dateTime
            +HistoryUserModel user
        }
        class GenericModel {
            <<abstract>>
            +UUID id
            +Boolean visible
            +HistoryModel creation
            +HistoryModel lastEdition
            +create<T>(UserModel user, LocalDateTime dateTime) T
            +update<T>(UserModel user, LocalDateTime dateTime) T
            *searchableValue() List
        }
    }
    HistoryModel *-- HistoryUserModel
    GenericModel *-- HistoryModel
```

Refer to the section below for the other functionals objects implementation of the MVP.

## User

```mermaid
classDiagram
    namespace Domain {
        class UserModel
        class UserAuthorityEnum
        class CurrentUserModel
        class IUserService 
        class UserService
        class IUserRepository
    }
    <<enum>> UserAuthorityEnum
    <<interface>> IUserService
    <<interface>> IUserRepository
    UserModel --|> GenericModel
    CurrentUserModel --|> UserModel
    CurrentUserModel *-- UserAuthorityEnum
    IUserService *-- CurrentUserModel
    IUserService *-- UserModel
    CurrentUserModel *-- EnrichedProfileModel
    UserService --|> IUserService
    UserService *-- IUserRepository
    namespace InfrastructureWeb {
        class IUserController
        class UserController
    }
    <<interface>> IUserController
    UserController --|> IUserController
    UserController *-- IUserService
    UserController *-- CurrentUserModel
    UserController *-- UserModel
    namespace InfrastructureDatasource {
        class IUserEntityRepository
        class UserRepository
    }
    UserRepository --|> IUserRepository
    UserRepository *-- IUserEntityRepository
```

## Profile



## Event



## Participant

