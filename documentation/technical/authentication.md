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

# 🔐 Authentication

Authentication management is delegated to the Organization authentication system, which must be configured when a new
Organization is created.

## Flow

### Authenticate

```mermaid
sequenceDiagram
    actor U as User
    participant F as Frontend
    participant B as Backend
    participant A as Authentication<br/>Provider
    autonumber
    U->>F: Navigate
    Note right of U: Non authenticated
    F->>B: Let the user authenticate
    Note right of B: User Redirection
    U-->>A: Starts OpenID Connect negotiation
    A->>B: Tokens
    B->>F: Cookie
    Note right of U: Authenticated
```

{% embed url="https://auth0.com/blog/the-backend-for-frontend-pattern-bff/" %}
Authentication flow explanation source
{% endembed %}

### Get ressource

```mermaid
sequenceDiagram
    actor U as User
    participant F as Frontend
    participant B as Backend
    participant A as Authentication<br/>Provider
    autonumber
    U->>F: Navigate
    Note right of U: Authenticated
    F->>B: Get ressource
    Note right of F: Cookie included
    B->>A: Validate token
    B->>B: Check Registry User's Roles
    B->>F: Ressource response
```
