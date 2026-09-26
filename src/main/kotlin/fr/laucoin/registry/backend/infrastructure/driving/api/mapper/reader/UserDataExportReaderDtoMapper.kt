package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.UserDataExportModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserDataExportReaderDto
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class UserDataExportReaderDtoMapper(
	private val userMapper: UserReaderDtoMapper,
	private val preferencesMapper: PreferencesReaderDtoMapper,
	private val projectProfileMapper: ProjectProfileReaderDtoMapper,
	private val movementMapper: MovementReaderDtoMapper,
	private val communicationMapper: CommunicationReaderDtoMapper,
	private val alertMapper: AlertReaderDtoMapper,
	private val activityMapper: ActivityReaderDtoMapper,
	private val vehicleMapper: VehicleReaderDtoMapper,
	private val groupMapper: GroupWithoutMemberReaderDtoMapper,
	private val participantDataExportMapper: ParticipantDataExportReaderDtoMapper,
) {
	fun toDto(model: UserDataExportModel): UserDataExportReaderDto {
		return UserDataExportReaderDto(
			user = Optional.ofNullable(model.user).map(userMapper::toDto).orElse(null),
			preferences = Optional.ofNullable(model.preferences).map(preferencesMapper::toDto).orElse(null),
			projectProfiles = model.projectProfiles.map(projectProfileMapper::toDto),
			createdProjectProfiles = model.createdProjectProfiles.map(projectProfileMapper::toDto),
			movements = model.movements.map(movementMapper::toDto),
			communications = model.communications.map(communicationMapper::toDto),
			alerts = model.alerts.map(alertMapper::toDto),
			activities = model.activities.map(activityMapper::toDto),
			vehicles = model.vehicles.map(vehicleMapper::toDto),
			groups = model.groups.map(groupMapper::toDto),
			linkedParticipants = model.linkedParticipants.map(participantDataExportMapper::toDto),
		)
	}
}
