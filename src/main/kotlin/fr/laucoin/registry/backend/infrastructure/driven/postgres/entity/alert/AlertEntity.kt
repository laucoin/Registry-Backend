package fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.alert

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.generic.GenericProjectEntity
import java.time.ZonedDateTime

data class AlertEntity(
	var dateTime: ZonedDateTime? = null,
	var title: String? = null,
	var status: AlertStatusEnum? = null,
): GenericProjectEntity()
