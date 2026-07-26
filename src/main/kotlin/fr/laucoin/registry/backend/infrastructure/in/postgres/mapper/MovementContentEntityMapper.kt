package fr.laucoin.registry.backend.infrastructure.`in`.postgres.mapper

import fr.laucoin.registry.backend.domain.model.MovementModel.MovementContentModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.`in`.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.movement.MovementContentEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.`in`.postgres.entity.vehicle.VehicleEntity
import org.springframework.stereotype.Component
import java.util.Optional
import java.util.UUID

@Component
class MovementContentEntityMapper(
	private val participantEntityMapper: ParticipantEntityMapper,
	private val vehicleEntityMapper: VehicleEntityMapper
) : IEntityReaderMapper<MovementContentModel, MovementContentEntity> {
	override fun toModel(entity: MovementContentEntity): MovementContentModel {
		return MovementContentModel(
			id = entity.id,
			poolName = entity.poolName,
			participant = mapParticipantEntity(entity),
			vehicle = mapVehicleEntity(entity),
		)
	}

	private fun mapParticipantEntity(entity: MovementContentEntity): ParticipantModel? {
		return Optional.ofNullable(entity.participantId).map {
			participantEntityMapper.toModel(
				ParticipantEntity().apply {
					id = it
					firstName = entity.participantFirstName
					lastName = entity.participantLastName
					birthday = entity.participantBirthday
					type = entity.participantType
				}
			)
		}.orElse(null)
	}

	private fun mapVehicleEntity(entity: MovementContentEntity): VehicleModel? {
		return Optional.ofNullable(entity.vehicleId).map {
			vehicleEntityMapper.toModel(
				VehicleEntity().apply {
					id = it
					licensePlate = entity.vehicleLicensePlate
					brand = entity.vehicleBrand
					model = entity.vehicleModel
				}
			)
		}.orElse(null)
	}

	fun toEntity(movementId: UUID, model: MovementContentModel): MovementContentEntity {
		return MovementContentEntity().apply {
			this.movementId = movementId
			poolName = model.poolName
			participantId = model.participant?.id
			vehicleId = model.vehicle?.id
		}
	}
}
