package fr.laucoin.registry.backend.infrastructure.internal.web.dto.reader

abstract class GenericProjectReaderDto(
    var project: ProjectReaderDto? = null,
): GenericReaderDto()
