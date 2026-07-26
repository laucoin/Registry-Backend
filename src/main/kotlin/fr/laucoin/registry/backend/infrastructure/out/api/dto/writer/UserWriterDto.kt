package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

/**
 * API v2 field edit for a User (ADR 017 §3: plain field edits are
 * `PATCH /{id}` with the changed fields — currently only the role).
 */
data class UserWriterDto(
	val role: String? = null,
)
