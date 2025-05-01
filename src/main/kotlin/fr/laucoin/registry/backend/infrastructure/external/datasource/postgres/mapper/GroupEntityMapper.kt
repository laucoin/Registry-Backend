package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class GroupEntityMapper: IEntityMapper<GroupModel, GroupEntity> {
    override fun toModel(entity: GroupEntity): GroupModel {
        return GroupModel().apply {
            name = entity.name
            startAvailability = if (Objects.isNull(entity.startAvailabilityDate)) null
            else CustomDateTimeModel(entity.startAvailabilityDate !!, entity.startAvailabilityTime)
            endAvailability = if (Objects.isNull(entity.endAvailabilityDate)) null
            else CustomDateTimeModel(entity.endAvailabilityDate !!, entity.endAvailabilityTime)
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
