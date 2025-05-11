package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import com.nimbusds.jose.shaded.gson.Gson
import com.nimbusds.jose.shaded.gson.reflect.TypeToken
import fr.laucoin.registry.backend.domain.extension.AvailabilityElementExt.buildStatus
import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.IEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.extension.GenericExt.fillWithProjectAndModel
import java.util.Objects
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class ParticipantEntityMapper(private val gson: Gson): IEntityMapper<ParticipantModel, ParticipantEntity> {
    private val groupListType = object: TypeToken<List<GroupModel>>() {}.type
    private val uuidListType = object: TypeToken<List<UUID>>() {}.type

    override fun toModel(entity: ParticipantEntity): ParticipantModel {
        val formattedUser: UserModel? = if (Objects.nonNull(entity.userId)) {
            UserModel().apply {
                id = entity.userId
                firstName = entity.userFirstName
                lastName = entity.userLastName
                email = entity.userEmail
            }
        } else null

        val presentGroupIds = if (Objects.isNull(entity.availableGroups)) emptyList()
        else gson.fromJson<List<UUID>?>(entity.availableGroups, uuidListType).filter { Objects.nonNull(it) }

        return ParticipantModel().apply {
            firstName = entity.firstName
            lastName = entity.lastName
            birthday = entity.birthday
            type = entity.type
            groups = gson.fromJson<List<GroupModel>?>(entity.groups, groupListType).filter { Objects.nonNull(it.id) }
            availableGroups = groups.filter { presentGroupIds.contains(it.id) }
            startAvailability = if (Objects.isNull(entity.startAvailabilityDate)) null
            else CustomDateTimeModel(entity.startAvailabilityDate !!, entity.startAvailabilityTime)
            endAvailability = if (Objects.isNull(entity.endAvailabilityDate)) null
            else CustomDateTimeModel(entity.endAvailabilityDate !!, entity.endAvailabilityTime)
            status = buildStatus(entity.lastMovementType)
            lastMovement = entity.lastMovementDateTime
            user = formattedUser
            purged = entity.purged
        }.fillWithProjectAndEntity(entity)
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
