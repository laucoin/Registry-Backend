package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityReaderMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupContentEntity
import java.util.UUID
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface GroupContentEntityMapper: IEntityReaderMapper<ParticipantModel, GroupContentEntity> {
	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "id", source = "participantId")
	@Mapping(target = "firstName", source = "participantFirstName")
	@Mapping(target = "lastName", source = "participantLastName")
	@Mapping(target = "birthday", source = "participantBirthday")
	@Mapping(target = "type", source = "participantType")
	override fun toModel(entity: GroupContentEntity): ParticipantModel

	@BeanMapping(ignoreByDefault = true)
	@Mapping(target = "groupId", source = "groupId")
	@Mapping(target = "participantId", source = "model.id")
	fun toEntity(groupId: UUID, model: ParticipantModel): GroupContentEntity
}
