package fr.laucoin.registry.backend.infrastructure.external

import fr.laucoin.registry.backend.domain.model.CustomDateTimeModel
import java.time.LocalDate
import java.time.OffsetTime
import java.util.Optional

interface IEntityReaderMapper<M, E> {
    fun toModel(entity: E): M

    fun mapCustomDateTime(date: LocalDate?, time: OffsetTime?): CustomDateTimeModel? {
        return Optional.ofNullable(date).map { CustomDateTimeModel(it, time) }.orElse(null)
    }
}
