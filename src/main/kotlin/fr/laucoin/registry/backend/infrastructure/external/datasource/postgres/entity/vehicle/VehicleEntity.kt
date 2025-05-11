package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericProjectEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_END_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LAST_MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_LICENSE_PLATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_DATE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_START_AVAILABILITY_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleFields.VEHICLE_TABLE
import java.time.LocalDate
import java.time.OffsetTime
import java.time.ZonedDateTime
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(VEHICLE_TABLE)
data class VehicleEntity(
    @Column(VEHICLE_LICENSE_PLATE)
    var licensePlate: String? = null,
    @Column(VEHICLE_BRAND)
    var brand: String? = null,
    @Column(VEHICLE_MODEL)
    var model: String? = null,
    @ReadOnlyProperty
    @Column(VEHICLE_LAST_MOVEMENT_TYPE)
    var lastMovementType: MovementTypeEnum? = null,
    @ReadOnlyProperty
    @Column(VEHICLE_LAST_MOVEMENT_DATE_TIME)
    var lastMovementDateTime: ZonedDateTime? = null,
    @Column(VEHICLE_START_AVAILABILITY_DATE)
    var startAvailabilityDate: LocalDate? = null,
    @Column(VEHICLE_START_AVAILABILITY_TIME)
    var startAvailabilityTime: OffsetTime? = null,
    @Column(VEHICLE_END_AVAILABILITY_DATE)
    var endAvailabilityDate: LocalDate? = null,
    @Column(VEHICLE_END_AVAILABILITY_TIME)
    var endAvailabilityTime: OffsetTime? = null,
): GenericProjectEntity()
