package fr.laucoin.registry.backend.infrastructure.out.api.dto.writer

import java.util.UUID

/**
 * API v2 Preferences select-profile action. The body carries the
 * identifier of the Profile to activate — either directly (`profileId`) or
 * through its Project (`projectId`); both null clears the selection (v1
 * behaviour of `/profile/select` without id).
 */
data class PreferencesSelectProfileWriterDto(
	var profileId: UUID? = null,
	var projectId: UUID? = null,
)
