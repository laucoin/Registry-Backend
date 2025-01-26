package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndModel
import org.springframework.stereotype.Component

@Component
class GroupEntityMapper(private val gson: Gson): IEntityMapper<GroupModel, GroupEntity> {
    private val listType = object: TypeToken<List<ParticipantModel>>() {}.type

    override fun toModel(entity: GroupEntity): GroupModel {
        return GroupModel().apply {
            name = entity.name
            begin = entity.begin
            end = entity.end
            members = gson.fromJson(entity.members, listType)
        }.fillWithEventAndEntity(entity)
    }

    override fun toEntity(model: GroupModel): GroupEntity {
        return GroupEntity().apply {
            name = model.name
            begin = model.begin
            end = model.end
        }.fillWithEventAndModel(model)
    }
}
