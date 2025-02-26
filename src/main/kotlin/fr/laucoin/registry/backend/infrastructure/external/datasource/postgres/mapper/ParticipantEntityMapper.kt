package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.fillWithEventAndModel
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ParticipantEntityMapper(private val gson: Gson): IEntityMapper<ParticipantModel, ParticipantEntity> {
    private val listType = object: TypeToken<List<GroupModel>>() {}.type

    override fun toModel(entity: ParticipantEntity): ParticipantModel {
        val formattedUser: UserModel? = if (Objects.nonNull(entity.userId)) {
            UserModel().apply {
                id = entity.userId
                firstName = entity.userFirstName
                lastName = entity.userLastName
                email = entity.userEmail
            }
        } else null

        return ParticipantModel().apply {
            firstName = entity.firstName
            lastName = entity.lastName
            birthday = entity.birthday
            groups = gson.fromJson<List<GroupModel>?>(entity.groups, listType).filter { Objects.nonNull(it.id) }
            begin = entity.begin
            end = entity.end
            user = formattedUser
            purged = entity.purged
        }.fillWithEventAndEntity(entity)
    }

    override fun toEntity(model: ParticipantModel): ParticipantEntity {
        return ParticipantEntity().apply {
            firstName = model.firstName
            lastName = model.lastName
            birthday = model.birthday
            begin = model.begin
            end = model.end
            userId = model.user?.id
            purged = model.purged
        }.fillWithEventAndModel(model)
    }
}
