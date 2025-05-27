package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.user.UserEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import java.util.Optional
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantEntityMapper(
    private val gson: Gson,
    private val userMapper: UserEntityMapper,
): IEntityMapper<ParticipantModel, ParticipantEntity> {
    private val groupListType = object: TypeToken<List<GroupModel>>() {}.type
    private val uuidListType = object: TypeToken<List<UUID>>() {}.type

    override fun toModel(entity: ParticipantEntity): ParticipantModel {
        return ParticipantModel().apply {
            firstName = entity.firstName
            lastName = entity.lastName
            birthday = entity.birthday
            type = entity.type
            groups = gson.fromJson<List<GroupModel>?>(entity.groups, groupListType).filter { Objects.nonNull(it.id) }
            availableGroups = groups.filter { buildPresentGroupIds(entity.availableGroups).contains(it.id) }
            startAvailability = mapCustomDateTime(entity.startAvailabilityDate, entity.startAvailabilityTime)
            endAvailability = mapCustomDateTime(entity.endAvailabilityDate, entity.endAvailabilityTime)
            status = buildStatus(entity.lastMovementType)
            lastMovement = entity.lastMovementDateTime
            user = mapUser(entity)
            purged = entity.purged
        }.fillWithProjectAndEntity(entity)
    }

    private fun buildPresentGroupIds(availableGroups: String?): List<UUID> {
        return Optional.ofNullable(availableGroups).map { g ->
            gson.fromJson<List<UUID>?>(g, uuidListType).filter { Objects.nonNull(it) }
        }.orElse(emptyList())
    }

    private fun mapUser(entity: ParticipantEntity): UserModel? {
        return Optional.ofNullable(entity.userId).map {
            userMapper.toModel(UserEntity().apply {
                id = it
                firstName = entity.userFirstName
                lastName = entity.userLastName
                email = entity.userEmail
            })
        }.orElse(null)
    }

    override fun toEntity(model: ParticipantModel): ParticipantEntity {
        return ParticipantEntity().apply {
            firstName = model.firstName
            lastName = model.lastName
            birthday = model.birthday
            type = model.type
            startAvailabilityDate = model.startAvailability?.date
            startAvailabilityTime = model.startAvailability?.time
            endAvailabilityDate = model.endAvailability?.date
            endAvailabilityTime = model.endAvailability?.time
            userId = model.user?.id
            purged = model.purged
        }.fillWithProjectAndModel(model)
    }
}
