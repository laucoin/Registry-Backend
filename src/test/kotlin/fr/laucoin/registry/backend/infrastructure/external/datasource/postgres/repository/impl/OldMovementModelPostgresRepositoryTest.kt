package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ActivityEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.springframework.transaction.reactive.TransactionalOperator

class OldMovementModelPostgresRepositoryTest {
    private val repository: IMovementEntityRepository = mock()
    private val contentRepository: IMovementContentEntityRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val activityMapper: ActivityEntityMapper = spy()
    private val contentMapper: MovementContentEntityMapper = spy()
    private val mapper: MovementEntityMapper = spy(MovementEntityMapper(activityMapper))
    private val modelRepository: MovementModelPostgresRepository =
        MovementModelPostgresRepository(repository, contentRepository, transactionalOperator, mapper, contentMapper)

//    companion object {
//        @JvmStatic
//        fun `Should findAll call repository findAll`(): Stream<Arguments> = Stream.of(
//            Arguments.of(true, null, null, null),
//            Arguments.of(true, IN, null, null),
//            Arguments.of(true, OUT, null, null),
//            Arguments.of(true, null, now(), null),
//            Arguments.of(true, null, null, now()),
//            Arguments.of(true, null, now(), now()),
//        )
//    }
//
//    @ParameterizedTest
//    @MethodSource
//    fun `Should findAll call repository findAll`(
//        visibilitySearched: Boolean?,
//        type: MovementTypeEnum?,
//        startDateTime: ZonedDateTime?,
//        endDateTime: ZonedDateTime?
//    ) {
//        // Arrange
//        val movement = MovementEntity()
//        whenever(repository.findAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(movement))
//
//        // Act
//        modelRepository.findPage(eventId,,,, endDateTime).blockFirst()
//
//        // Assert
//        verify(repository).findAll(eventId, onlyVisible, type, startDateTime, endDateTime)
//        verify(mapper).toModel(movement)
//    }
//
//    @Test
//    fun `Should findById call repository findById`() {
//        // Arrange
//        val movement = MovementEntity()
//        val uuid = UUID.randomUUID()
//        val onlyVisible = true
//        whenever(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement))
//
//        // Act
//        modelRepository.findById(eventId, uuid, onlyVisible).block()
//
//        // Assert
//        verify(repository).findById(eventId, uuid, onlyVisible)
//        verify(mapper).toModel(movement)
//    }
//
//    @Test
//    fun `Should create call repository save`() {
//        // Arrange
//        val movement = MovementModel()
//        val movementEntity = MovementEntity()
//        whenever(repository.save(any())).thenReturn(Mono.just(movementEntity))
//        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(movement))
//
//        // Act
//        modelRepository.create(movement).block()
//
//        // Assert
//        verify(repository).save(any())
//        verify(mapper).toEntity(movement)
//    }
//
//    @Test
//    fun `Should update call repository save`() {
//        // Arrange
//        val movement = MovementModel()
//        val movementEntity = MovementEntity()
//        whenever(repository.save(any())).thenReturn(Mono.just(movementEntity))
//        whenever(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(movement))
//
//        // Act
//        modelRepository.update(movement).block()
//
//        // Assert
//        verify(repository).save(any())
//        verify(mapper).toEntity(movement)
//    }
//
//    @Test
//    fun `Should deleteById call repository deleteById`() {
//        // Arrange
//        val uuid = UUID.randomUUID()
//        whenever(repository.deleteById(any<UUID>())).thenReturn(Mono.empty())
//
//        // Act
//        modelRepository.deleteById(uuid).block()
//
//        // Assert
//        verify(repository).deleteById(uuid)
//    }
}
