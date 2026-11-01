package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.constant.DateTimeConst.EPOCH_UTC
import fr.laucoin.registry.backend.domain.enumeration.AlertCriticalityEnum
import fr.laucoin.registry.backend.domain.enumeration.AlertStatusEnum
import java.time.ZonedDateTime

data class AlertModel(
	var title: String? = null,
	var dateTime: ZonedDateTime = EPOCH_UTC,
	var status: AlertStatusEnum? = null,
	var criticality: AlertCriticalityEnum? = null,
	var communications: List<CommunicationModel>? = null,
	var lastCommunication: CommunicationModel? = null,
): GenericProjectModel()
