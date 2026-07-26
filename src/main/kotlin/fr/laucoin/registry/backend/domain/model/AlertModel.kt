package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import java.time.ZonedDateTime

data class AlertModel(
	var title: String? = null,
	var dateTime: ZonedDateTime = ZonedDateTime.now(),
	var status: AlertStatusEnum? = null,
	var communications: List<CommunicationModel>? = null,
) : GenericProjectModel()
