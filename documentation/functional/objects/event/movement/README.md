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

# 🚶 Movement 🔵

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Date and time**\
  Date and time when the participant leave the event
* **Reason**\
  Reason can be predefined by Event to specify the movement reason.
* **Activity**\
  Some reason can required an activity to link to a movement.
* **Description**\
  If the generic reason are not enough, you can add more information in the description.
* **Drafted**\
  A Movement can be drafted, It is just a Movement preparation to save time during departure.
* **Hidden**\
  A Movement can be hide and restore

## Features

### As a User

* I can consult the Movements (with search, sort, etc.)
* I can see current Movements Details (in linked Movement)
* I can get the Participant count (splitted by major minor)
    * Currently present in the Event (non related to inside and outside)
    * Inside the event
    * Outside the event
    * By Registration (if it is a group Registration)
* I can create a Movement
    * I would see scheduled Movement to facilitate the field completion
    * I would see a warning message if I try to create an entry for an already present Participant, Guest or Vehicle (
      the reverse is
      true).
    * I would see a warning message if I try to use a Vehicle with no initial odometer value
* I can schedule a Movement
* I can edit a Movement
* I can hide a Movement
* I can restore a Movement
* I can delete a Movement

### As the Frontend

* I can listen new Movements in a Web Socket to refresh displayed list
* I can listen edited Movements in a Web Socket to refresh displayed list
* I can listen deleted Movements in a Web Socket to refresh displayed list

## Constraints

### As a User

* I can sort Movements by the last edition date.
* I can search Movement by date range, Participant, Guest, Activity or Vehicle properties.
* I can see scheduled Movement if the landing date is close (value to defined in Event parameters)

### Other

* A current Movement/Movement Detail is a Movement Detail with an anormal situation:
    * A Participant outside the Event (Obviously if you're within the range of his dates of presence).
    * A Guest inside the Event.
