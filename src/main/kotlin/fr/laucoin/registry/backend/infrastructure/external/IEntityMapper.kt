package fr.laucoin.registry.backend.infrastructure.external

interface IEntityMapper<M, E>: IEntityReaderMapper<M, E>, IEntityWriterMapper<M, E>
