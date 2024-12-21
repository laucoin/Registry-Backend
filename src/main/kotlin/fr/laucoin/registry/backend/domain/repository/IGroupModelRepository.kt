package fr.laucoin.registry.backend.domain.repository

import fr.laucoin.registry.backend.domain.model.GroupModel
import java.time.ZonedDateTime
import java.util.UUID
import reactor.core.publisher.Flux

interface IGroupModelRepository: IGenericReadEventModelRepository<GroupModel>, IGenericWriteModelRepository<GroupModel> {
    fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<GroupModel>

    fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<GroupModel>
}
