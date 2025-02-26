package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_MOVEMENT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_POOL_NAME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_BRAND
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_ID
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_MODEL
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.movement.MovementFields.MOVEMENT_CONTENT_VEHICLE_REGISTRATION
import java.time.LocalDate
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(MOVEMENT_CONTENT_TABLE)
data class MovementContentEntity(
    @Id
    var id: UUID? = null,

    @Column(MOVEMENT_CONTENT_MOVEMENT_ID)
    var movementId: UUID? = null,

    @Column(MOVEMENT_CONTENT_POOL_NAME)
    var poolName: String? = null,

    @Column(MOVEMENT_CONTENT_PARTICIPANT_ID)
    var participantId: UUID? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_PARTICIPANT_FIRST_NAME)
    var participantFirstName: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_PARTICIPANT_LAST_NAME)
    var participantLastName: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_PARTICIPANT_BIRTHDAY)
    var participantBirthday: LocalDate? = null,

    @Column(MOVEMENT_CONTENT_VEHICLE_ID)
    var vehicleId: UUID? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_VEHICLE_REGISTRATION)
    var vehicleRegistration: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_VEHICLE_BRAND)
    var vehicleBrand: String? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT_VEHICLE_MODEL)
    var vehicleModel: String? = null,
)
