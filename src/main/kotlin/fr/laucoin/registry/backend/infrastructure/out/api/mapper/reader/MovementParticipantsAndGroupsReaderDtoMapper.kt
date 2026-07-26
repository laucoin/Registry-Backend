package fr.laucoin.registry.backend.infrastructure.out.api.mapper.reader

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.out.api.dto.reader.MovementParticipantsAndGroupsReaderDto
import org.springframework.stereotype.Component

@Component
class MovementParticipantsAndGroupsReaderDtoMapper(
	private val participantReaderMapper: ParticipantReaderDtoMapper,
	private val groupReaderMapper: GroupReaderDtoMapper,
) : IGenericReaderDtoMapper<Pair<List<ParticipantModel>, List<GroupModel>>, MovementParticipantsAndGroupsReaderDto> {
	override fun toDto(model: Pair<List<ParticipantModel>, List<GroupModel>>): MovementParticipantsAndGroupsReaderDto {
		return MovementParticipantsAndGroupsReaderDto(
			participants = participantReaderMapper.toDtoList(model.first),
			groups = groupReaderMapper.toDtoList(model.second),
		)
	}
}
