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

# 🗓️ Event 🔵

## Definition

Cf. [Glossary](../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Name**\
  The name is the human identifier for the Event. It must not be unique but it's recommended to create the most explicit name.
* **Start date and time**\
  This is the start date.
* **End date and time**\
  This is the end date.
* **Address**\
  Useful information to detail the event.
* **Options**\
  Some features can be enabled for specifics events. The list of available option should be defined in the organization config.
* **Scheduled display threshold**\
  This is the threshold at which scheduled movements are shown.
* **Hidden**\
  An Event can be hide and restore. Obviously, nothing related to a disabled event can be perform except edit, re-enable and delete it.

## Features

### As a User

* I can consult all the Events (with search, sort, etc.).
* I can create an Event.
* I can edit a Event.
* I can hide the concerned Event.
* I can restore the concerned Event.
* I can delete the concerned Event.

### As the Backend

* I automatically delete Event and all related data 5 years after the end.

## Constraints

### As a User

* Without profile, I cannot edit, hide, restore an Event.
* I can sort Events by name and Address's fields
* I can search Events by name and date range
* I cannot create or edit an Event with End date and time before or equal Start date and time
