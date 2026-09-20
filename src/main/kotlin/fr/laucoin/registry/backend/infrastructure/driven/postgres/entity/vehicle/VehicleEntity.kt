package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.vehicle

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime

data class VehicleEntity(
	var licensePlate: String? = null,
	var brand: String? = null,
	var model: String? = null,
	var lastMovementType: MovementTypeEnum? = null,
	var lastMovementDateTime: ZonedDateTime? = null,
	var startAvailabilityDate: LocalDate? = null,
	var startAvailabilityTime: OffsetTime? = null,
	var endAvailabilityDate: LocalDate? = null,
	var endAvailabilityTime: OffsetTime? = null,
): GenericProjectEntity()
