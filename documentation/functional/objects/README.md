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

# 🔫 Objects

```mermaid
erDiagram
    "Fire Risk"
    Organization ||..O{ Structure: has
    Organization ||..O{ User: has
    Structure ||..O{ Structure: has
    Structure }|..O{ User: has
    Structure ||..O{ Profile: has
    User ||..O{ Profile: has
    Profile ||..O| Event: scopes
    Profile ||..O| Registration: scopes
    Event ||..O| Address: locates
    Event ||..O{ Vehicle: welcomes
    Event ||..O{ Ticketing: exposes
    Ticketing ||..O{ Registration: participates
    Event ||..O{ Activity: proposes
    Event ||..O{ Movement: has
    Event ||..O{ Reason: has
    Movement ||..O| Reason: justifies
    Movement ||..|{ "Movement Detail": has
    Movement ||..O| Activity: instantiates
    "Movement Detail" ||..O| Vehicle: moves
    "Movement Detail" ||..|| Participant: moves
    Event ||..O{ Registration: participates
    Registration ||..O{ Group: has
    Group ||..O{ Participant: has
    Event ||..O{ Participant: has
    Registration ||..O{ Participant: has
    Event ||..O{ "Phone Communication": communicates
    Event ||..O{ "Activity Communication": communicates
    "Activity Communication" ||..|| Activity: communicates
    "Activity Communication" ||..O| "Smoke Report": reports
    "Smoke Report" ||..O| "Smoke Detail": details
    "Activity Communication" ||..O| "Movement Report": reports
```
