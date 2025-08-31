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

# ☁️ Smoke Report 🔴

## Definition

Cf. [Glossary](../../../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Firemen comment**\
  To keep in memory, the firemen feedback on our report.
* **Hidden**\
  A Movement Report can be hide and restore

## Features

If the Event has the Smoke Report Option.

Refer the [Activity Communication features](../#features), because Smoke Report is closely linked to an Activity Communication.

It is important to specify questions about fire in the front side

* **Smoke color**\
  Color of the smoke
* **Fire target**\
  Type of thing which currently burn (Example: Vehicle, Forest, etc.)
* **Size**\
  Smoke size (Example: small, medium, large)

### As a User

* I can consult non Finished Smoke Report

### As the Frontend

* I can listen new Smoke Report non mark as FINISHED in a Web Socket to refresh displayed list 🟣
* I can listen edited Smoke Report non mark as FINISHED in a Web Socket to refresh displayed list 🟣
* I can listen deleted Smoke Report non mark as FINISHED in a Web Socket to refresh displayed list 🟣
