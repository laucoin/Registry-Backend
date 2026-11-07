package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ActivityModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.activity.ActivityEntity
import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.ReportingPolicy

@Mapper(config = PostgresMapperConfig::class)
interface ActivityEntityMapper: IEntityMapper<ActivityModel, ActivityEntity> {
	@BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
	@IgnoreModelAudit
	@AvailabilityDatesToModel
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "allowedParticipants", ignore = true)
	override fun toModel(entity: ActivityEntity): ActivityModel

	@IgnoreEntityAudit
	@AvailabilityDatesToEntity
	@Mapping(target = "duration", ignore = true)
	@Mapping(target = "minAllowedParticipants", source = "allowedParticipants.lower")
	@Mapping(target = "maxAllowedParticipants", source = "allowedParticipants.upper")
	override fun toEntity(model: ActivityModel): ActivityEntity
}
