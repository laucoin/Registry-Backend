package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.util.Optional

@Component
class ParticipantReaderDtoMapper(
	private val partialUserMapper: PartialUserReaderDtoMapper,
	private val typeMapper: ParticipantTypeReaderDtoMapper,
	private val statusMapper: PresenceStatusReaderDtoMapper,
	private val projectMapper: ProjectReaderDtoMapper,
	private val groupMapper: GroupWithoutMemberReaderDtoMapper,
) : IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
	override fun toDto(model: ParticipantModel): ParticipantReaderDto {
		return ParticipantReaderDto(
			firstName = model.firstName,
			lastName = model.lastName,
			birthday = model.birthday,
			type = Optional.ofNullable(model.type).map(typeMapper::toDto).orElse(null),
			major = isMajor(model.birthday),
			groups = groupMapper.toDtoList(model.groups),
			availableGroups = groupMapper.toDtoList(model.availableGroups),
			status = Optional.ofNullable(model.status)
				.map { statusMapper.toDto(it, model.lastMovement, model.startAvailability, model.endAvailability) }
				.orElse(null),
			availabilityWarning = model.availabilityWarning,
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
			departedAt = model.departedAt,
			user = Optional.ofNullable(model.user).map(partialUserMapper::toDto).orElse(null),
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map(projectMapper::toDto).orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}

	private fun isMajor(birthday: LocalDate?): Boolean = birthday.isMajor()
}
