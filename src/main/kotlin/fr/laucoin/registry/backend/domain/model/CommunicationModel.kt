package fr.laucoin.registry.backend.domain.model

import java.time.ZonedDateTime

data class CommunicationModel(
    var dateTime: ZonedDateTime = ZonedDateTime.now(),
    var message: String? = null,
    var movement: MovementModel? = null,
): GenericProjectModel()
