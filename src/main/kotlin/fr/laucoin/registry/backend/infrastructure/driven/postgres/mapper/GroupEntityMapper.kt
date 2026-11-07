package fr.laucoin.registry.backend.infrastructure.driven.postgres.mapper

import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.driven.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.driven.postgres.entity.group.GroupEntity
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper(config = PostgresMapperConfig::class)
interface GroupEntityMapper: IEntityMapper<GroupModel, GroupEntity> {
	@IgnoreModelAudit
	@AvailabilityDatesToModel
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "membersCount", source = "members")
	@Mapping(target = "insideMembersCount", source = "insideMembers")
	@Mapping(target = "outsideMembersCount", source = "outsideMembers")
	@Mapping(target = "onSiteMembersCount", ignore = true)
	@Mapping(target = "inMembersCount", ignore = true)
	@Mapping(target = "outMembersCount", ignore = true)
	@Mapping(target = "overdueMembersCount", ignore = true)
	@Mapping(target = "members", ignore = true)
	override fun toModel(entity: GroupEntity): GroupModel

	@IgnoreEntityAudit
	@AvailabilityDatesToEntity
	@Mapping(target = "members", ignore = true)
	@Mapping(target = "insideMembers", ignore = true)
	@Mapping(target = "outsideMembers", ignore = true)
	override fun toEntity(model: GroupModel): GroupEntity
}
