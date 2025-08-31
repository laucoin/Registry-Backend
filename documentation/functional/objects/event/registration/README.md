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

# 📜 Registration 🟢

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Name**\
  The name is the human identifier for the Registration.
* **User**\
  The user is the Registration owner
* **Structure**\
  The structure is the Registration owner
* **Information**\
  The information is the subscription for result.
* **Status**\
  The status is the registration status. If the registration was not a request it is approved by default.
* **Start date and time**\
  This is the Registration announced start date and time (by default, it's the Ticketing start date and time).
* **End date and time**\
  This is the Registration announced end date and time (by default, it's the Ticketing end date and time).
* **Ticketing**\
  Linked Ticketing (if created from Ticketing)
* **Hidden**\
  A registration can be hide and restore.

## Features

### As a User

* I can consult the Registration list (with search, sort, etc.)
* I can consult all Registrations linked to me or my Structure
* I can consult all Registrations linked to User or Structure whose already has a Registration for my Event
* I can create a Registration
* I can edit a Registration
* I can accept a Registration
* I can refuse a Registration
* I can delete a Registration

### As the Frontend

* I can listen new Registrations in a Web Socket to refresh displayed list 🟣
* I can listen edited Registrations in a Web Socket to refresh displayed list 🟣
* I can listen deleted Registrations in a Web Socket to refresh displayed list 🟣

## Constraints

### As a User

* I can sort Registrations by name.
* I can search Registrations by name and date.
* I cannot create or edit a Registration with End date and time before or equal Start date and time
