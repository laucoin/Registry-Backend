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

# 🚶‍♂️ Presence Report 🟣

## Definition

Cf. [Glossary](../../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Location**\
  Movement position
* **Description**\
  Other movement description elements (Example: People description, with vehicle(s) or not, etc.)
* **People quantity**\
  Number of people
* **Type**\
  Is an entrance to or exit
* **Hidden**\
  A Movement Report can be hide and restore

## Features

If the Event has the Movement Report Option.

Refer the [Activity Communication features](./#features), because Movement Report is closely linked to an Activity Communication.

### As a User

* I can consult number of people in the monitored zone.

### As the Frontend

* I can listen number of people in the monitored zone in a Web Socket to refresh it 🟣

## Constraint

### As a User

* Number of people is calculated from the current day reporting
