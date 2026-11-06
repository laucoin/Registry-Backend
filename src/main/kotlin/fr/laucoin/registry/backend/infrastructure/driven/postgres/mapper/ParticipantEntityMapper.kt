package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.ParticipantMappingHelper
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class, uses = [ParticipantMappingHelper::class])
interface ParticipantEntityMapper: IEntityMapper<ParticipantModel, ParticipantEntity> {
	@IgnoreModelAudit
	@AvailabilityDatesToModel
	@Mapping(target = "groups", ignore = true)
	@Mapping(target = "availableGroups", ignore = true)
	@Mapping(target = "lastMovement", source = "lastMovementDateTime")
	@Mapping(target = "user", source = "entity", qualifiedByName = ["userOf"])
	override fun toModel(entity: ParticipantEntity): ParticipantModel

	@BeanMapping(ignoreByDefault = true)
	@KeepGenericProjectColumns
	@AvailabilityDatesToEntity
	@Mapping(target = "firstName", source = "firstName")
	@Mapping(target = "lastName", source = "lastName")
	@Mapping(target = "birthday", source = "birthday")
	@Mapping(target = "type", source = "type")
	@Mapping(target = "userId", source = "user.id")
	override fun toEntity(model: ParticipantModel): ParticipantEntity
}
