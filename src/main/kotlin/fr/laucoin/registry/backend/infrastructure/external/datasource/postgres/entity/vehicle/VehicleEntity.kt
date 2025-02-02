package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BEGIN
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_REGISTRATION
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import java.time.ZonedDateTime
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(VEHICLE_TABLE)
data class VehicleEntity(
    @Column(VEHICLE_REGISTRATION)
    var registration: String? = null,
    @Column(VEHICLE_BRAND)
    var brand: String? = null,
    @Column(VEHICLE_MODEL)
    var model: String? = null,
    @Column(VEHICLE_BEGIN)
    var begin: ZonedDateTime? = null,
    @Column(VEHICLE_END)
    var end: ZonedDateTime? = null,
): GenericEventEntity()
