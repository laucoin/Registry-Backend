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

# 🎙️ Activity Communication 🟠

## Definition

Cf. [Glossary](../../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Date and time**\
  That the communication moment.
* **Activity**\
  That the activity who communicate with the Event.
* **Message**\
  The message is the content of communication.
* **Location**\
  The position of the activity during the communication.
* **Smoke Report**
* **Movement Report**
* **Hidden**\
  An Activity Communication can be hide and restore.

## Features

If the Event has the Activity Communication Option.

### As a User

* I can consult the Activity Communications (with search, sort, etc.)
* I can see a timer with the timer since the last communication (if defined in
  the [Activity](../activity.md#properties))
* I can create easily create an Activity Communication for Activity in progress.
* I can create an Activity Communication
* I can edit an Activity Communication
* I can hide an Activity Communication
* I can restore an Activity Communication
* I can delete an Activity Communication

### As the Frontend

* I can listen new Activity Communication in a Web Socket to refresh displayed list 🟣
* I can listen edited Activity Communication in a Web Socket to refresh displayed list 🟣
* I can listen deleted Activity Communication in a Web Socket to refresh displayed list 🟣

## Constraints

### As a User

* I can sort Activity Communications by date and time.
* I can search Activity Communication by activity, contains report (smoke or movement) date and time.
