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

# 🎟️ Ticketing 🟢

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Name**\
  This is the Ticketing displayed name (if empty, event name). The name is the human identifier for the Ticketing. It must not be
  unique but it's recommended to create the most explicit name.
* **Description**\
  The description of the event for the participant
* **Start date and time**\
  This is the Event start date and time access (by default, it's the event start date and time).
* **End date and time**\
  This is the Event end date and time access (by default, it's the event end date and time).
* **Public**\
  In the organization options, you can declare list of public to receive (example: Scouts et Guides de France received multiple age
  range like 14-17 years old)
* **Individual or Group**\
  Note: if you want to open registration for both you just need to create two ticketing.
    * Individual: Individual registration
    * Group: Group registration
* **Opening date and time**\
  The Ticketing will be open, only after this date.
* **Closing date and time**\
  The Ticketing will be open until this date.
* **Event**\
  That the Event targeted by the ticketing.
* **Hidden**\
  A Ticketing can be hide and restore.

## Features

### As a User

* I can consult the Ticketing (with search, sort, etc.)
* I can create a Ticketing
* I can edit a Ticketing
* I can hide a Ticketing
* I can restore a Ticketing
* I can delete a Ticketing

{% hint style="info" %}
Bear in mind that this type of data is often created all at once (or at least a lot at once). The UX must be geared to this context.
{% endhint %}

## Constraint

### As a User

* I cannot create or edit a Ticketing with
    * End date and time before or equal Start date and time
    * Closing date and time before or equal Opening date and time
* I cannot update Start date and time and End date and time if there is Registration linked &#x20;
