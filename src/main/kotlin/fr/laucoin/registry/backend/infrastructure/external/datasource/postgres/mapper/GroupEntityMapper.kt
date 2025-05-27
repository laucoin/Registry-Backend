package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import org.springframework.stereotype.Component

@Component
class GroupEntityMapper: IEntityMapper<GroupModel, GroupEntity> {
    override fun toModel(entity: GroupEntity): GroupModel {
        return GroupModel().apply {
            name = entity.name
            status = buildStatus()
            startAvailability = mapCustomDateTime(entity.startAvailabilityDate, entity.startAvailabilityTime)
            endAvailability = mapCustomDateTime(entity.endAvailabilityDate, entity.endAvailabilityTime)
            membersCount = entity.members
            insideMembersCount = entity.insideMembers
            outsideMembersCount = entity.outsideMembers
        }.fillWithProjectAndEntity(entity)
    }

    override fun toEntity(model: GroupModel): GroupEntity {
        return GroupEntity().apply {
            name = model.name
            startAvailabilityDate = model.startAvailability?.date
            startAvailabilityTime = model.startAvailability?.time
            endAvailabilityDate = model.endAvailability?.date
            endAvailabilityTime = model.endAvailability?.time
        }.fillWithProjectAndModel(model)
    }
}
