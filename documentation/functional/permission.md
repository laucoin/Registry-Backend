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

# 🎓 Permission

## Definition

**Roles:** When it comes to the management and access of digital assets common roles include “admin”, “editor”, “moderator” and “viewer”. Each of these roles will then be assigned a set of permissions. For example, the admin will typically have complete access to data and system features and be able to change system settings. An editor by contrast may be able to edit specific documents but is unable to affect any system-wide changes.

**Permissions:** Once roles have been established permissions are assigned which specify what actions are associated with a given role. Typical permissions include reading, editing, creating or deleting data. Other permissions include the ability to manage users and configure settings. In most cases, permissions are related to specific digital resources such as folders, files, database records and more.

{% embed url="https://www.cerbos.dev/blog/roles-and-permissions-definition" %}
Roles & Permissions definition
{% endembed %}

## Feature

In Registry, there is many features with many related called. The objective is to defined logical small features blocks.

As it is said in the [Functional Object](broken-reference) part, their is 3 privileges level, for more information check out [User](objects/user.md#roles-scope) and [Profile](objects/profile.md#profiles-scope) concerned section.

### User

That the case for all Users (non-related with permissions and roles).

* Can consult his direct parent Structure(s)
* Can consult himself and updated his application preferences (Cf. [User properties](objects/user.md#properties) for more information)
* Can consult his current and past Registration (Profile linked to a Registration)
* Can consult his current and past Event (Profile linked to an Event)

### Organization

For user with the following permission, he have this role for the Organization level

* **Structure**
  * REGISTRY\_STRUCTURE\_C\
    Create Structure
  * REGISTRY\_STRUCTURE\_R\
    Consult and search Structure
  * REGISTRY\_STRUCTURE\_U\
    Update Structure (include hide and restore)
  * REGISTRY\_STRUCTURE\_D\
    Delete Structure
* **User**
  * ~~REGISTRY\_USER\_C~~\
    User can't be created manually, as this is done automatically when you log in, if no user exists.
  * REGISTRY\_USER\_R\
    Consult and search User
  * REGISTRY\_USER\_U\
    Update User (include hide and restore)
  * REGISTRY\_USER\_D\
    Delete Users
* **Event**
  * REGISTRY\_EVENT\_C\
    Create Event
  * REGISTRY\_EVENT\_R\
    Consult and search Event
  * ~~REGISTRY\_EVENT\_U~~\
    Event's cannot not updated without a direct access to the concerned object.
  * ~~REGISTRY\_EVENT\_D~~\
    Event's cannot not deleted without a direct access to the concerned object.
* **Profile**
  * REGISTRY\_PROFILE\_C\
    Create a Support Profile
  * ~~REGISTRY\_PROFILE\_R~~\
    Event or Registration's Profile cannot be consult without a direct access to the concerned object.
  * ~~REGISTRY\_PROFILE\_U~~\
    Event or Registration's Profile cannot be updated without a direct access to the concerned object.
  * ~~REGISTRY\_PROFILE\_D~~\
    Event or Registration's Profile cannot be deleted without a direct access to the concerned object.
* Statistics
  * ~~REGISTRY\_STATISTIC\_C~~\
    A Statistic can obviously not be created
  * REGISTRY\_STATISTIC\_R\
    Global Organization (that mean all Events linked) Statistic can be consulted
  * ~~REGISTRY\_STATISTIC\_U~~\
    A Statistic can obviously not be updated
  * ~~REGISTRY\_STATISTIC\_D~~\
    A Statistic can obviously not be deleted

### Event

All the following permissions scopes are limited to the Event with the ID "EVENT'S ID"

* **Event**
  * ~~\[EVENT'S ID]\_REGISTRY\_EVENT\_C~~\
    There is not sub event, so an Event can obviously not be created in another one
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_R\
    Consult the Event
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_U\
    Update the Event
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_D\
    Delete the Event\

* **Ticketing**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_TICKETING\_C\
    Create Ticketing
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_TICKETING\_R\
    Consult and search Ticketing
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_TICKETING\_U\
    Update Ticketing
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_TICKETING\_D\
    Delete Ticketing
* **Registration**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REGISTRATION\_C\
    Create Registration
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REGISTRATION\_R\
    Consult and search Registration
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REGISTRATION\_U\
    Update Registration
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REGISTRATION\_D\
    Delete Registration
* **Group**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_GROUP\_C\
    Create Group
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_GROUP\_R\
    Consult and search Group
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_GROUP\_U\
    Update Group
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_GROUP\_D\
    Delete Group
* **Participant**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PARTICIPANT\_C\
    Create Participant
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PARTICIPANT\_R\
    Consult and search Participant
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PARTICIPANT\_U\
    Update Participant
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PARTICIPANT\_D\
    Delete Participant
* **Movement**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_C\
    Create Movement
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_R\
    Consult and search Movement
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_U\
    Update Movement
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_D\
    Delete Movement
* **Reason**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REASON\_C\
    Create Reason
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REASON\_R\
    Consult and search Reason
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REASON\_U\
    Update Reason
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_REASON\_D\
    Delete Reason
* **Vehicle**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_VEHICLE\_C\
    Create Vehicle
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_VEHICLE\_R\
    Consult and search Vehicle
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_VEHICLE\_U\
    Update Vehicle
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_VEHICLE\_D\
    Delete Vehicle
* **Activity**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_C\
    Create Activity
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_R\
    Consult and search Activity
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_U\
    Update Activity
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_D\
    Delete Activity
* **Phone Communication**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PHONE\_COMMUNICATION\_C\
    Create Phone Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PHONE\_COMMUNICATION\_R\
    Consult and search Phone Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PHONE\_COMMUNICATION\_U\
    Update Phone Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_PHONE\_COMMUNICATION\_D\
    Delete Phone Communication
* **Activity Communication**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_COMMUNICATION\_C\
    Create Activity Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_COMMUNICATION\_R\
    Consult and search Activity Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_COMMUNICATION\_U\
    Update Activity Communication
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_ACTIVITY\_COMMUNICATION\_D\
    Delete Activity Communication
* **Smoke Report**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_SMOKE\_REPORT\_C\
    Create Smoke Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_SMOKE\_REPORT\_R\
    Consult and search Smoke Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_SMOKE\_REPORT\_U\
    Update Smoke Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_SMOKE\_REPORT\_D\
    Delete Smoke Report
* **Movement Report**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_REPORT\_C\
    Create Movement Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_REPORT\_R\
    Consult and search Movement Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_REPORT\_U\
    Update Movement Report
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_MOVEMENT\_REPORT\_D\
    Delete Movement Report
* **Fire Risk**
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_FIRE\_RISK\_C\
    Create Fire Risk
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_FIRE\_RISK\_R\
    Consult and search Fire Risk
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_FIRE\_RISK\_U\
    Update Fire Risk
  * ~~\[EVENT'S ID]\_REGISTRY\_EVENT\_FIRE\_RISK\_D~~\
    A Fire Risk cannot be deleted (only refresh with updated)
* **Statistic**
  * ~~\[EVENT'S ID]\_REGISTRY\_EVENT\_STATISTIC\_C~~\
    A Statistic can obviously not be created
  * \[EVENT'S ID]\_REGISTRY\_EVENT\_STATISTIC\_R\
    Consult and search Statistic
  * ~~\[EVENT'S ID]\_REGISTRY\_EVENT\_STATISTIC\_U~~\
    A Statistic can obviously not be updated
  * ~~\[EVENT'S ID]\_REGISTRY\_EVENT\_STATISTIC\_D~~\
    A Statistic can obviously not be deleted

### Registration

* Group
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_GROUP\_C\
    Create Group
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_GROUP\_R\
    Consult and search Group
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_GROUP\_U\
    Update Group
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_GROUP\_D\
    Delete Group
* Participant
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_PARTICIPANT\_C\
    Create Participant
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_PARTICIPANT\_R\
    Consult and search Participant
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_PARTICIPANT\_U\
    Update Participant
  * \[REGISTRATION'S ID]\_REGISTRY\_REGISTRATION\_PARTICIPANT\_D\
    Delete Participant
