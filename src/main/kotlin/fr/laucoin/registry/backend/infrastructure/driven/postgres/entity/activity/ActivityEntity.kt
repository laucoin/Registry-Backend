package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime

data class ActivityEntity(
	var name: String? = null,
	var description: String? = null,
	var duration: String? = null,
	var minAllowedParticipants: Int? = null,
	var maxAllowedParticipants: Int? = null,
	var startAvailabilityDate: LocalDate? = null,
	var startAvailabilityTime: OffsetTime? = null,
	var endAvailabilityDate: LocalDate? = null,
	var endAvailabilityTime: OffsetTime? = null,
): GenericProjectEntity()
