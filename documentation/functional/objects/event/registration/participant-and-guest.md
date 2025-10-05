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

# 🙎 Participant 🔵 & Guest 🟠

## Definition

There is 2 types of participant

* **Participant:** Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)
* **Guest:** Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

{% hint style="info" %}
Note a Participant (and only one by Event) can be link to an User.
{% endhint %}

### Properties

* **First name**\
  Participant or Guest first name. (For user it's the User's object value)
* **Last name**\
  Participant or Guest last name. (For user it's the User's object value)
* **Birthday**\
  Participant or Guest Birthday.\
  (Only optional for Guest)\
  (For user it's the User's object value)
* **Type**\
  The participant can be a Participant, Guest.\
  Note: The Guest feature can be enable or disabled.
* **User**\
  If participant type is User we can link him to a User.
* **Start date and time**\
  This is the Participant announced start date and time (by default, it's the Registration start date and time).
* **End date and time**\
  This is the Participant announced end date and time (by default, it's the Registration end date and time).
* **Hidden**\
  An User, Participant, Guest or Service account can be hide and restore.

## Features

### As a User

* I can consult the entire Participants & Guests (with search, sort, etc.)
* I can consult a Participants or Guest history ([Movement Detail](../movement/movement-detail.md))
* I can create a Participant or Guest
* I can edit a Participant or Guest
* I can hide a Participant or Guest
* I can restore a Participant or Guest
* I can delete a Participant or Guest
* I can impersonate a Participant or Guest

{% hint style="info" %}
Bear in mind that this type of data is often created all at once (or at least a lot at once). The UX must be geared to
this context.
{% endhint %}

### As the Backend

For more information, please check [GDPR](../../../gdpr.md#automatic-data-purge).

* I automatically impersonate Participants, 1 year after the end of the Event

## Constraints

### As a User

* I can sort Participants or Guest by last name.
* I can search Participant or Guest by last name, first name, presence dates range.
