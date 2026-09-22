package fr.laucoin.registry.backend.infrastructure.driven

interface IEntityMapper<M, E>: IEntityReaderMapper<M, E>, IEntityWriterMapper<M, E>
