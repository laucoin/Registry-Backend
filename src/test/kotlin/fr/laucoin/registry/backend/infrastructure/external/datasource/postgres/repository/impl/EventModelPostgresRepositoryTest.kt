package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.EventModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.EventEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventEntityRepository
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventModelPostgresRepositoryTest {
    private val repository: IEventEntityRepository = mock()
    private val mapper: EventEntityMapper = spy()
    private val modelRepository: EventModelPostgresRepository = EventModelPostgresRepository(
        repository, mapper
    )

    companion object {
        @JvmStatic
        fun `Should findAll call repository findAll`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, null, null),
            Arguments.of(true, now(), null),
            Arguments.of(true, null, now()),
            Arguments.of(true, now(), now()),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAll call repository findAll`(
        onlyVisible: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ) {
        // Arrange
        val event = EventEntity()
        `when`(repository.findAll(any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(event))

        // Act
        modelRepository.findAll(onlyVisible, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(onlyVisible, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(event)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Arrange
        val event = EventEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any())).thenReturn(Mono.just(event))

        // Act
        modelRepository.findById(uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(uuid, onlyVisible)
        verify(mapper, times(1)).toModel(event)
    }

    @Test
    fun `Should create call repository save`() {
        // Arrange
        val event = EventModel()
        val eventEntity = EventEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(eventEntity))

        // Act
        modelRepository.create(event).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(event)
        verify(mapper, times(1)).toModel(eventEntity)
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
