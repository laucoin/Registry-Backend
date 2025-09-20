package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.GroupReaderDto
import java.util.Locale
import java.util.Optional
import org.springframework.stereotype.Component

@Component
class GroupReaderDtoMapper(
	private val projectMapper: ProjectReaderDtoMapper,
	private val availabilityStatusMapper: AvailabilityStatusReaderDtoMapper,
	private val participantMapper: ParticipantReaderDtoMapper,
): IGenericReaderDtoMapper<GroupModel, GroupReaderDto> {
	override fun toDto(model: GroupModel, locale: Locale): GroupReaderDto {
		return GroupReaderDto(
			members = participantMapper.toDtoList(model.members, locale),
		).apply {
			id = model.id
			status = Optional.ofNullable(model.status)
				.map { availabilityStatusMapper.toDto(it, locale, model.startAvailability, model.endAvailability) }
				.orElse(null)
			project = Optional.ofNullable(model.project).map { projectMapper.toDto(it, locale) }.orElse(null)
			name = model.name
			startAvailability = model.startAvailability
			endAvailability = model.endAvailability
			membersCount = model.membersCount
			insideMembersCount = model.insideMembersCount
			outsideMembersCount = model.outsideMembersCount
			visible = model.visible
			creation = model.creation
			lastEdition = model.lastEdition
		}
	}
}
