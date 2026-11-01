package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.constant.DateTimeConst.EPOCH_UTC
import java.time.ZonedDateTime

data class CommunicationModel(
	var dateTime: ZonedDateTime = EPOCH_UTC,
	var message: String? = null,
	var movement: MovementModel? = null,
	var alert: AlertModel? = null,
): GenericProjectModel()
