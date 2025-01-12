package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.config.GsonConfig
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.IN
import fr.laucoin.registry.backend.domain.enumeration.MovementTypeEnum.OUT
import fr.laucoin.registry.backend.domain.model.MovementModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.movement.MovementEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.MovementEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IMovementEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.eventId
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MovementModelPostgresRepositoryTest {
    private val gson: Gson = GsonConfig().gson()
    private val repository: IMovementEntityRepository = mock()
    private val contentRepository: IMovementContentEntityRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val mapper: MovementEntityMapper = spy(MovementEntityMapper(gson))
    private val contentMapper: MovementContentEntityMapper = spy()
    private val modelRepository: MovementModelPostgresRepository =
        MovementModelPostgresRepository(repository, contentRepository, transactionalOperator, mapper, contentMapper)

    companion object {
        @JvmStatic
        fun `Should findAll call repository findAll`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, null, null, null),
            Arguments.of(true, IN, null, null),
            Arguments.of(true, OUT, null, null),
            Arguments.of(true, null, now(), null),
            Arguments.of(true, null, null, now()),
            Arguments.of(true, null, now(), now()),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAll call repository findAll`(
        onlyVisible: Boolean,
        type: MovementTypeEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ) {
        // Arrange
        val movement = MovementEntity()
        `when`(repository.findAll(any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(movement))

        // Act
        modelRepository.findAll(eventId, onlyVisible, type, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(eventId, onlyVisible, type, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(movement)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Arrange
        val movement = MovementEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(movement))

        // Act
        modelRepository.findById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(movement)
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val movement = MovementModel()
        val movementEntity = MovementEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(movementEntity))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(movement))

        // Act
        modelRepository.create(movement).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(movement)
    }

    @Test
    fun `Should deleteById call repository deleteById`() {
        // Arrange
        val uuid = UUID.randomUUID()
        `when`(repository.deleteById(any<UUID>())).thenReturn(Mono.empty())

        // Act
        modelRepository.deleteById(uuid).block()

        // Assert
        verify(repository, times(1)).deleteById(uuid)
    }
}
