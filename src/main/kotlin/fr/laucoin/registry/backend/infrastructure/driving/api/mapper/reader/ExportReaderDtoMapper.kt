package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.HistoryModel
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.domain.model.ParticipantExportModel
import fr.laucoin.registry.backend.domain.model.PreferencesModel
import fr.laucoin.registry.backend.domain.model.ProjectProfileModel
import fr.laucoin.registry.backend.domain.model.UserExportModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportHistoryReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportMovementReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportParticipantReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportPreferencesReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportProjectProfileReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ExportUserDataReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.ParticipantExportReaderDto
import fr.laucoin.registry.backend.infrastructure.driving.api.dto.reader.UserExportReaderDto
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ExportReaderDtoMapper {
	fun toDto(model: UserExportModel) = UserExportReaderDto(
		generatedAt = model.generatedAt,
		user = toUserDto(model.user),
		preferences = model.preferences?.let(::toPreferencesDto),
		projectProfiles = model.projectProfiles.map(::toProfileDto),
		participants = model.participants.map(::toParticipantDto),
	)

	fun toDto(model: ParticipantExportModel) = ParticipantExportReaderDto(
		generatedAt = model.generatedAt,
		participant = toParticipantDto(model),
	)

	private fun toUserDto(user: UserModel) = ExportUserDataReaderDto(
		id = user.id,
		oidcId = user.oidcId,
		type = user.type.name,
		firstName = user.firstName,
		lastName = user.lastName,
		email = user.email,
		role = user.role,
		birthday = user.birthday,
		lastLogin = user.lastLogin,
		blocked = user.isNotVisible(),
	)

	private fun toPreferencesDto(preferences: PreferencesModel) = ExportPreferencesReaderDto(
		theme = preferences.theme.name,
		language = preferences.language,
	)

	private fun toProfileDto(profile: ProjectProfileModel) = ExportProjectProfileReaderDto(
		projectId = profile.project?.id ?: profile.projectId,
		projectName = profile.project?.name,
		role = profile.role,
		status = profile.status?.name,
		startAccess = profile.startAccess,
		endAccess = profile.endAccess,
	)

	private fun toParticipantDto(model: ParticipantExportModel): ExportParticipantReaderDto {
		val participant = model.participant
		val subjectUserId = participant.user?.id

		return ExportParticipantReaderDto(
			id = participant.id,
			projectId = participant.projectId,
			projectName = model.projectName,
			firstName = participant.firstName,
			lastName = participant.lastName,
			birthday = participant.birthday,
			type = participant.type?.name,
			startAvailability = participant.startAvailability,
			endAvailability = participant.endAvailability,
			groups = participant.groups.mapNotNull { it.name },
			movements = model.movements.map(::toMovementDto),
			creation = participant.creation?.let { toHistoryDto(it, subjectUserId) },
			lastEdition = participant.lastEdition?.let { toHistoryDto(it, subjectUserId) },
		)
	}

	private fun toMovementDto(movement: MovementModel) = ExportMovementReaderDto(
		dateTime = movement.dateTime,
		type = movement.type?.name,
		reason = movement.reason?.name,
		activity = movement.activity?.name,
	)

	private fun toHistoryDto(history: HistoryModel, subjectUserId: UUID?) = ExportHistoryReaderDto(
		dateTime = history.dateTime,
		byDataSubject = subjectUserId != null && history.user?.id == subjectUserId,
	)
}
