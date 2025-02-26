package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END

object VehicleQueries {
    const val IN_DATE_RANGE_CLAUSE = """
        (:startDateTime IS NULL OR :startDateTime <= t.$VEHICLE_END) AND
        (:endDateTime IS NULL OR :endDateTime >= t.$VEHICLE_BEGIN)
    """

    const val PRESENT_CLAUSE = """
        (:onlyPresent IS FALSE OR (
            (t.$VEHICLE_BEGIN IS NULL OR t.$VEHICLE_BEGIN <= CURRENT_TIMESTAMP)
            AND (t.$VEHICLE_END IS NULL OR t.$VEHICLE_END <= CURRENT_TIMESTAMP)
        ))
    """
}
