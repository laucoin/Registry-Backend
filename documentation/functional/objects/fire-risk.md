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

# 🔥 Fire risk 🔴

## Definition

Cf. [Glossary](../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Date**\
  The the date concerned by the risk level
* **Level**\
  The risk level qualify
* **Zone**\
  The geographic zone concerned by the risk

## Features

### As a User

* When I create an Address (with an Event) and it match with a Fire Risk zone, I can see the today and tomorrow Fire Risk on the
  related Event home screen.
* I can fetch Fire Risk
* I can refresh Fire Risk

### As the Frontend

* I can listen the fire risk in a Web Socket to refresh or display it 🟣

## Constraints

* That information is related to an external dependency
    * Currently we use [http://bpatp.paca-ate.fr/](http://bpatp.paca-ate.fr/)
        * Not available all the year
        * Not linked to an Address
        * Not all the France
