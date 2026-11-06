package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.GUEST
import fr.laucoin.registry.backend.domain.enumeration.ParticipantTypeEnum.REGISTERED
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.driven.postgres.helper.MovementMappingHelper
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.Named

@Mapper(config = PostgresMapperConfig::class, uses = [MovementMappingHelper::class])
interface MovementEntityMapper: IEntityMapper<MovementModel, MovementEntity> {
	@IgnoreModelAudit
	@Mapping(target = "activity", source = "entity", qualifiedByName = ["activityOf"])
	@Mapping(target = "contentType", source = "entity", qualifiedByName = ["contentTypeOf"])
	@Mapping(target = "content", ignore = true)
	override fun toModel(entity: MovementEntity): MovementModel

	@BeanMapping(ignoreByDefault = true)
	@KeepGenericProjectColumns
	@Mapping(target = "dateTime", source = "dateTime")
	@Mapping(target = "type", source = "type")
	@Mapping(target = "reason", source = "reason")
	@Mapping(target = "activityId", source = "activity.id")
	override fun toEntity(model: MovementModel): MovementEntity

	@Named("contentTypeOf")
	fun contentTypeOf(entity: MovementEntity): ParticipantTypeEnum {
		return entity.reason?.participantType
			?: if (entity.type == IN || entity.activityId != null) REGISTERED else GUEST
	}
}
