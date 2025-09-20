package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.extension.DateExt.isMajor
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.ParticipantReaderDto
import java.time.LocalDate
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class ParticipantReaderDtoMapper(
	private val partialUserMapper: PartialUserReaderDtoMapper,
	private val typeMapper: ParticipantTypeReaderDtoMapper,
	private val statusMapper: PresenceStatusReaderDtoMapper,
	private val projectMapper: ProjectReaderDtoMapper,
	private val groupMapper: GroupWithoutMemberReaderDtoMapper,
): IGenericReaderDtoMapper<ParticipantModel, ParticipantReaderDto> {
	override fun toDto(model: ParticipantModel, locale: Locale): ParticipantReaderDto {
		return ParticipantReaderDto(
			firstName = model.firstName,
			lastName = model.lastName,
			birthday = model.birthday,
			type = Optional.ofNullable(model.type).map { typeMapper.toDto(it, locale) }.orElse(null),
			major = isMajor(model.birthday),
			groups = groupMapper.toDtoList(model.groups, locale),
			availableGroups = groupMapper.toDtoList(model.availableGroups, locale),
			status = Optional.ofNullable(model.status)
				.map {
					statusMapper.toDto(
						it,
						locale,
						model.lastMovement,
						model.startAvailability,
						model.endAvailability
					)
				}
				.orElse(null),
			startAvailability = model.startAvailability,
			endAvailability = model.endAvailability,
			user = Optional.ofNullable(model.user).map { partialUserMapper.toDto(it, locale) }.orElse(null),
			purged = model.purged,
		).apply {
			id = model.id
			project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}

	private fun isMajor(birthday: LocalDate?): Boolean = birthday.isMajor()
}
