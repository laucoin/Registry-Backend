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

# 🧑‍🤝‍🧑 Structure 🔴

## Definition

Cf. [Glossary](../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Name**\
  The name is the human identifier
* **Address**\
  This address is the administrative location
* **Phone**\
  The phone is the phone contact
* **Email**\
  The email is the web contact
* **Web site**\
  The web site is the site url for the information detail.&#x20;
* **Parent**\
  Structures can be recursive, i.e. one structure can contain another, etc. Concerning the role management, a User inherit from his
  first parent Roles.

## Features

### As a User

* I can consult the entire Structures (with search, sort, etc.).
* I can create a Structure.
* I can edit a Structure.
* I can disable (hide) a Structure.
* I can re-enable (restore from hidden) a Structure.
* I can delete permanently a Structure.

### As an Organization

* I can listen new Structures in a TOPIC 🟣
* I can listen edited Structures in a TOPIC 🟣
* I can listen deleted Structures in a TOPIC 🟣

## Constraints

### As a User

* I can sort Structures by name
* I can search Structures by name
* The Structure creation, edition, deletion should be a global right because it concerns the Organization management.
