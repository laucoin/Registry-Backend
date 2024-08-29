package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_CONTENT
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_DATE_TIME
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_TABLE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementFields.MOVEMENT_TYPE
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.generic.GenericEventEntity
import java.time.ZonedDateTime
import org.springframework.data.annotation.ReadOnlyProperty
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table

@Table(MOVEMENT_TABLE)
data class MovementEntity(
    @Column(MOVEMENT_DATE_TIME)
    var dateTime: ZonedDateTime? = null,
    @Column(MOVEMENT_TYPE)
    var type: MovementTypeEnum? = null,
    @ReadOnlyProperty
    @Column(MOVEMENT_CONTENT)
    var content: String = "[]",
): GenericEventEntity()
