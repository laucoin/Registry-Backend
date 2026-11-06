---
name: architecture-review
description: Evaluates macro-level architectural compliance for the Registry backend, including hexagonal layout, domain isolation (with tolerance for Spring annotations), UseCase/Core service separation, helper and cross-feature model mutualization, MapStruct mapper enforcement, Flyway rules, and API backward compatibility.
---

# Architecture Review (Macro Perspective)

Review the change (diff, files, or current branch) against the architectural invariants of the Registry backend. Only
files touched by the change are scored; gaps in untouched existing code are listed as debt without deduction. Start
with a baseline score of **100 points** and deduct points based on findings: **blocker −40**, **should-fix −10**, **nit
−2.5** (floor at 0).

## Evaluation Criteria

### 1. Hexagonal Layout & Domain Structure

- **Strict Layering:** Respect the hexagonal package structure (`config/`, `domain/`, `infrastructure/driving|driven`).
- **Domain Isolation & Spring Tolerance:** The `domain` package is isolated from API contracts and external
  infrastructure dependencies, but **Spring Boot annotations (e.g., `@Service`, `@Component`) are permitted** for
  business implementations. It reaches infrastructure only via explicit `port` interfaces.
- **Service & UseCase Breakdown (`domain/service/`):**
    - **Core Services (`service/core/`):** One per resource (suffix `Service`, e.g., `ProjectService`). Core services
      must **never** call sibling core services nor UseCases to prevent circular dependencies.
    - **Orchestrators (`service/usecase/`):** Business workflows (suffix `UseCase`, e.g., `ProjectProfileUseCase`).
      UseCases orchestrate core services and ports; they are only invoked by upstream callers (driving adapters /
      controllers). UseCases are **never** injected into core services, support services or other UseCases.
      Controllers (`infrastructure.driving`) reach `core/` only through a UseCase; they may call `support/` directly.
    - **Support Services (`service/support/`):** Technical services without a business resource (e.g., translation,
      concurrency, logging, principal cache; suffix `Service`). They are callable from controllers, UseCases and core
      services, and must **never** depend on core services nor UseCases.
    - **Naming Convention:** Retain `I*` interfaces and `impl/` packages (e.g., `IProjectService` +
      `impl/ProjectService`) to avoid scope churn.
- **Helper Placement & Cross-Feature Mutualization:**
    - Reusable, stateless, or utility logic must be extracted into dedicated helper classes in the correct package
      (`domain/helper` for domain logic, `infrastructure/helper` for technical tasks).
    - **Cross-Feature Mutualization:** Track and prevent duplication (DRY principle). Components, helpers, logic, and
      **domain data models** must be cross-feature mutualized whenever they share a common business concept, rather than
      being duplicated across modules.
- **ArchUnit Rules:** Enforce package isolation rules via ArchUnit tests.

### 2. Data Flow & MapStruct Mappers

- **Unidirectional Flow:** Controller → UseCase → Core Service → Port → Adapter.
- **Contract Isolation:** Controllers implement contract interfaces with endpoint metadata. No database entities or
  persistence models may leak through the API (verify return types and request bodies).
- **MapStruct Mappers:** Use **MapStruct interfaces** for all object-to-object and DTO mappings. Manual mapping
  methods/classes are prohibited. Complex mapping transformations must rely on MapStruct custom helper methods or
  decorators.

### 3. Configuration & Database (Flyway)

- **Externalized Configuration:** Configuration must be fully externalized via typed properties; no environment-specific
  values baked into the code. All config keys must be documented in `README.md`.
- **Schema Ownership:** Flyway is the sole schema owner. Database changes require a new forward-only migration file
  (`V...`). Applied migrations must never be edited.

### 4. Documentation & Backward Compatibility

- **Documentation Drift:** Check that endpoint, config key, migration, or dependency updates align with `README.md`,
  OpenAPI annotations, and `api-reference` impact (report drift; do not edit docs during review).
- **API Contracts & Versioning:** Breaking API/contract changes must be intentional, versioned (e.g., `/api/v2`), and
  explicitly flagged for the frontend (`../Registry-Frontend`) when request/response shapes change.

---

## Output Format

- **Partial Score:** Start at 100, apply deductions, and output the resulting score (e.g., `Architecture: 90/100`).
- **Findings List:** Grouped by severity with precise locations (`file:line`), explanations, and exact fixes.
