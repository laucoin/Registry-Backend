package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

abstract class GenericEventReaderDto(
    var event: EventReaderDto? = null,
): GenericReaderDto()
