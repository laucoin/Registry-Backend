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

# 📱 Phone Communication 🟣

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Date and time**\
  That the call moment
* **Receiver**\
  The receiver is the target of the call
* **Emitter**\
  The emitter is the caller, the person who want to say something
* **Message**\
  The message is the content of communication.
* **Hidden**\
  A phone Communication can be hide and restore

## Features

If the Event has the Phone Communication Option.

### As a User

* I can consult the Phone Communications (with search, sort, etc.)
* I can create a Phone Communication
* I can edit a Phone Communication
* I can hide a Phone Communication
* I can restore a Phone Communication
* I can delete a Phone Communication

### As the Frontend

* I can listen new Phone Communications in a Web Socket to refresh displayed list 🟣
* I can listen edited Phone Communications in a Web Socket to refresh displayed list 🟣
* I can listen deleted Phone Communications in a Web Socket to refresh displayed list 🟣

## Constraints

### As a User

* I can sort Phone Communications by date and time.
* I can search Phone Communication by caller, called, date and time.
