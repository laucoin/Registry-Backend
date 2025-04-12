package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle

object VehicleFields {
    const val VEHICLE_TABLE = "tb_vehicle"

    const val VEHICLE_LICENSE_PLATE = "license_plate"
    const val VEHICLE_BRAND = "brand"
    const val VEHICLE_MODEL = "model"
    const val VEHICLE_LAST_MOVEMENT_TYPE = "last_movement_type"
    const val VEHICLE_LAST_MOVEMENT_DATE_TIME = "last_movement_date_time"
    const val VEHICLE_START_AVAILABILITY_DATE = "start_availability_date"
    const val VEHICLE_START_AVAILABILITY_TIME = "start_availability_time"
    const val VEHICLE_END_AVAILABILITY_DATE = "end_availability_date"
    const val VEHICLE_END_AVAILABILITY_TIME = "end_availability_time"
}
