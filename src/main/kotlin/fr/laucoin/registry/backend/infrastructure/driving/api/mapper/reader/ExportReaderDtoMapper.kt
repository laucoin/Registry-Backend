package fr.laucoin.registry.backend.infrastructure.driving.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
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
import fr.laucoin.registry.backend.infrastructure.driving.api.mapper.DtoMapperConfig
import java.util.UUID
import fr.laucoin.registry.backend.domain.constant.ExportConst
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = DtoMapperConfig::class)
interface ExportReaderDtoMapper {
	@Mapping(target = "user", source = "model.user")
	@Mapping(target = "schemaVersion", constant = ExportConst.SCHEMA_VERSION)
	@Mapping(target = "subjectType", constant = ExportConst.SUBJECT_USER)
	fun toDto(model: UserExportModel): UserExportReaderDto

	@Mapping(target = "participant", source = "model")
	@Mapping(target = "schemaVersion", constant = ExportConst.SCHEMA_VERSION)
	@Mapping(target = "subjectType", constant = ExportConst.SUBJECT_PARTICIPANT)
	fun toDto(model: ParticipantExportModel): ParticipantExportReaderDto

	@Mapping(target = "blocked", source = "notVisible")
	fun toUserDto(user: UserModel): ExportUserDataReaderDto

	fun toPreferencesDto(preferences: PreferencesModel): ExportPreferencesReaderDto

	@Mapping(target = "projectId", source = "project.id", defaultExpression = "java(profile.getProjectId())")
	@Mapping(target = "projectName", source = "project.name")
	fun toProfileDto(profile: ProjectProfileModel): ExportProjectProfileReaderDto

	@Mapping(target = "id", source = "participant.id")
	@Mapping(target = "projectId", source = "participant.projectId")
	@Mapping(target = "firstName", source = "participant.firstName")
	@Mapping(target = "lastName", source = "participant.lastName")
	@Mapping(target = "birthday", source = "participant.birthday")
	@Mapping(target = "type", source = "participant.type")
	@Mapping(target = "startAvailability", source = "participant.startAvailability")
	@Mapping(target = "endAvailability", source = "participant.endAvailability")
	@Mapping(target = "groups", source = "participant.groups", qualifiedByName = ["groupNames"])
	@Mapping(target = "creation", source = "model", qualifiedByName = ["creationOf"])
	@Mapping(target = "lastEdition", source = "model", qualifiedByName = ["lastEditionOf"])
	fun toParticipantDto(model: ParticipantExportModel): ExportParticipantReaderDto

	@Mapping(target = "activity", source = "activity.name")
	fun toMovementDto(movement: MovementModel): ExportMovementReaderDto

	@Mapping(target = "byDataSubject", expression = "java(subjectUserId != null && history.getUser() != null && subjectUserId.equals(history.getUser().getId()))")
	fun toHistoryDto(history: HistoryModel, subjectUserId: UUID?): ExportHistoryReaderDto

	@Named("groupNames")
	fun groupNames(groups: List<GroupModel>): List<String> = groups.mapNotNull { it.name }

	@Named("creationOf")
	fun creationOf(model: ParticipantExportModel): ExportHistoryReaderDto? {
		return model.participant.creation?.let { toHistoryDto(it, model.participant.user?.id) }
	}

	@Named("lastEditionOf")
	fun lastEditionOf(model: ParticipantExportModel): ExportHistoryReaderDto? {
		return model.participant.lastEdition?.let { toHistoryDto(it, model.participant.user?.id) }
	}
}
