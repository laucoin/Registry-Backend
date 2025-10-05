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

# 🏢 Organization 🔵

## Definition

Cf. [Glossary](../../../../../Perso/registry-documentation/glossary.md)

## Configuration properties

The organization registration imply multiple configuration.&#x20;

### Authentication

An organization has multiple possibilities for authentication configuration.&#x20;

* Use Registry's authentication provider (Keycloak). It mean, the user should create a new realm and client via
  registry.
* Use his authentication provider.
* Use Both. It mean, you can create a new realm and client. Moreover you register and an external authentication
  provider.

### Option

An Organization can limit the option access.

For example Registry provide 10 options. The organization can limit its scope to 8 options.

### Support

Each Organization need to specify its Registry's support contact.

### Roles

All Organization must defined at least 1 role for the Event. It is highly recommended to create more of them, especially
for user
management and granularity.

> If a Permission is not assigned to a Role, It mean the feature is disable (because no one can do it).
