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

# 📊 Statistic 🟣

## Features

### As a User

I can consult the following Statistics over time at different levels (all my profiles, whole organization, event)

* Fire Risk over the time
* Average of Fire Risk
* Vehicles usage
* Most frequently used Vehicles
* Number of people by Event (major, minor, guest)
* Average of Participant by Event
* Average of kilometers by Vehicle
* Average of kilometers by Event
* Average of Participant by Registration
* Vehicle/Participant ratio by Event
* Average duration of Event
* Movements (people and vehicles average)
* Number of Registration (depending Ticketing)
* Number of Movements
* Number of Guest
* Number of Report
  * Smoke Report
  * Movement Report
  * All

## Constraints

* Kilometers of a Vehicle should be considered only if the initial and final value are not null.
* If a Vehicle has no data, a message should be displayed to inform User the data is not representative.
* The duration of an unfinished Movement will be limited to the duration of the event to minimize the impact on statistics.
