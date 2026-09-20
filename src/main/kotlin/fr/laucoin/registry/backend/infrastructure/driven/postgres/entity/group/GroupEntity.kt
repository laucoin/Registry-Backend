package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group

import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime

data class GroupEntity(
	var name: String? = null,
	var startAvailabilityDate: LocalDate? = null,
	var startAvailabilityTime: OffsetTime? = null,
	var endAvailabilityDate: LocalDate? = null,
	var endAvailabilityTime: OffsetTime? = null,
	var members: Long? = null,
	var insideMembers: Long? = null,
	var outsideMembers: Long? = null,
): GenericProjectEntity()
