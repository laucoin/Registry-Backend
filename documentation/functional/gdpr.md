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

# 📨 GDPR

## GDPR

The General Data Protection Regulation is an European law which impose a framework about natural person data usage.

### Needs

To make Registry work properly, we need some user personal information. Moreover, Registry usage create some personal
data like movement related to an Event.

#### First and Last name

The first and last name are store in Registry to identify a user easily and to allow him to identify his account.

#### Email

The user email is store to allow him to sign in. Moreover, we can send communication about the application scope (
registration confirmation, etc.).

Consent about opt-in is store in the opt-in consent property.

{% hint style="info" %}
No commercial information will be send
{% endhint %}

#### Birthday

Birthday is store to identify minors, propose feature flipping (like propose or not the driver contract, etc.), inform
Event organizator about a birthday during the event.

#### Driver license

If an organization require driver contract, we will ask the driver license of the Participant. We need to store the
driver's license and related information so that we can report the Participant if he or she turns out to have committed
an offense with one of the event vehicles.

In France 🇫🇷, We must indicate who drive the vehicle in case of infraction. In this context, data relating to the
driver's license will be kept for a maximum of 1 year after the end of the event, the person's participation or last
outing as a driver.

#### Movement

One of the application's objectives is to preserve the safety of participants by keeping track of their comings and
goings at the event. In certain cases, such as certain activities carried out by the event organizers, and only in this
context, a Participant's position can be identified and saved.

### Right to be forgotten

User can sign in to Registry, they can ask for his data removal. In some case, we cannot access his demand:

* If a User is registered for a futur or unfinished Event (since 30 days)
* If the User drove a vehicle less than a year ago

### Automatic data purge

1 year after the end of an event, all personal data and movements related will be anonymized.

Thats very important especially for the participant who are not allowed to sign in and they cannot use the right to be
forgotten directly for the application. If a participant contacts the Registry team directly, their request will be
processed if they are eligible for the right to be forgotten.
