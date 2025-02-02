package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.domain.repository.IVehicleModelRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IVehicleEntityRepository
import java.time.ZonedDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Service
class VehicleModelPostgresRepository(
    private val repository: IVehicleEntityRepository,
    private val mapper: VehicleEntityMapper,
): IVehicleModelRepository {
    override fun findAll(
        eventId: UUID,
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ): Flux<VehicleModel> {
        return repository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).map(mapper::toModel)
    }

    override fun findAllByIds(eventId: UUID, ids: List<UUID>, onlyVisible: Boolean): Flux<VehicleModel> {
        return if (ids.isEmpty()) Flux.empty()
        else repository.findAllByIds(eventId, ids, onlyVisible).map(mapper::toModel)
    }

    override fun findById(eventId: UUID, id: UUID, onlyVisible: Boolean): Mono<VehicleModel> {
        return repository.findById(eventId, id, onlyVisible).map(mapper::toModel)
    }

    override fun create(element: VehicleModel): Mono<VehicleModel> {
        return save(element)
    }

    override fun update(element: VehicleModel): Mono<VehicleModel> {
        return save(element)
    }

    private fun save(element: VehicleModel): Mono<VehicleModel> {
        return repository.save(mapper.toEntity(element)).map(mapper::toModel)
    }

    override fun deleteById(id: UUID): Mono<Void> {
        return repository.deleteById(id)
    }
}
