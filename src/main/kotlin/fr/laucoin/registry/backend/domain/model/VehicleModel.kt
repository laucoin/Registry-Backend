package fr.laucoin.registry.backend.domain.model

import fr.laucoin.registry.backend.domain.enumeration.UsableElementStatusEnum

data class VehicleModel(
    var licensePlate: String? = null,
    var brand: String? = null,
    var model: String? = null,
    var status: UsableElementStatusEnum? = null,
    var startAvailability: CustomDateTimeModel? = null,
    var endAvailability: CustomDateTimeModel? = null,
): GenericProjectModel()
