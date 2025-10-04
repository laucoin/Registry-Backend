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

# 🎳 Activity 🟡

## Definition

Cf. [Glossary](../../../../../../../../Perso/registry-documentation/glossary.md)

### Properties

* **Name**\
  The name is the human identifier for the Activity. It must not be unique but it's recommended to create the most
  explicit name.
* **Estimated Duration**\
  Indicative information about activity (Example: 2-3 hours)
* **Required communication time**\
  Maximum time without communication (Only if Activity Communication option is enabled for the current Event)
* **Minimum participant**\
  The minimum activity's participant to do it
* **Maximum participant**\
  The maximum activity's participant to do it
* **Strict**\
  If the activity is strict, it's mean the minimum and maximum participant are non overridable.
* **Hidden**\
  An Activity can be hide and restore

## Features

If the Event has the Activity Option.

### As a User

* I can consult the Activities (with search, sort, etc.)
* I can create an Activity
* I can edit an Activity
* I can hide an Activity
* I can restore an Activity
* I can delete an Activity

{% hint style="info" %}
Bear in mind that this type of data is often created all at once (or at least a lot at once). The UX must be geared to
this context.
{% endhint %}

## Constraints

### As a User

* I can sort Activities by name.
* I can search Activity by name, duration and participant range.
