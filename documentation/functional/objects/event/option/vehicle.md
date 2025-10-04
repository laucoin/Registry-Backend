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

# 🚗 Vehicle 🟡

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Registration**\
  For a car, this is the license plate. The aim is to uniquely identify a vehicle.
* **Brand**\
  The Vehicle brand (Example: Toyota, Ford, Peugeot, etc.)
* **Model**\
  The Vehicle model (Example: Golf, Hilux, etc.)
* **Type**\
  The Vehicle kind (Example: car, plane, boat, etc.)
* **Initial odometer value**\
  A Vehicle start an Event with an odometer value, it is useful for statistics
* **Final odometer value**\
  A Vehicle finish an Event with an odometer value, it is useful for statistics
* **Start date and time**\
  A Vehicle is maybe not available all the during all the event. We can specify a start presence date.
* **End date and time**\
  A Vehicle is maybe not available all the during all the event. We can specify an end presence date.
* **Hidden**\
  A Vehicle can be hide and restore.

## Features

If the Event has the Vehicle Option.

### As a User

* I can consult the Vehicles (with search, sort, etc.)
* I can consult a Vehicle history ([Movement Detail](../movement/movement-detail.md) linked with the Vehicle)
* I can see a warning message to fill the initial odometer value
* I can see a warning message to fill the final odometer value&#x20;
* I can create a Vehicle
* I can edit a Vehicle
* I can hide a Vehicle
* I can restore a Vehicle
* I can delete a Vehicle

{% hint style="info" %}
Bear in mind that this type of data is often created all at once (or at least a lot at once). The UX must be geared to
this context.
{% endhint %}

### As the Frontend

* I can listen new Vehicle Histories in a Web Socket to refresh displayed list
* I can listen edited Vehicle Histories in a Web Socket to refresh displayed list
* I can listen deleted Vehicle Histories in a Web Socket to refresh displayed list

## Constraints

### As a User

* I can sort Vehicles by registration.
* I can search Vehicle by registration, brand, model, type, presence dates range.
* I can see initial odometer warning message if the value is empty
* I can see final odometer warning message only
    * 1 day before the Event end
    * If the value is empty
* I cannot create or edit a Vehicle with End date and time before or equal Start date and time
