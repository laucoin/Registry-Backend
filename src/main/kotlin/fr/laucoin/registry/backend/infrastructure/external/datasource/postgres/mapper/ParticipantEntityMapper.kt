package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper

import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.domain.model.UserModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantEntity
import java.util.Objects
import org.springframework.stereotype.Component

@Component
class ParticipantEntityMapper: IGenericEventEntityMapper<ParticipantModel, ParticipantEntity> {
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
            begin = entity.begin
            end = entity.end
            user = formattedUser
            purged = entity.purged
            event = mapEventEntity(entity)
        }.fillWithEntity(entity)
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
            eventId = model.event?.id
        }.fillWithModel(model)
    }
}
