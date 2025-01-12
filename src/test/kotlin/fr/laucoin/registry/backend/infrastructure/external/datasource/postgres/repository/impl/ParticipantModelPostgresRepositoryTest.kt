package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.config.GsonConfig
import fr.laucoin.registry.backend.domain.model.ParticipantModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.participant.ParticipantEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.ParticipantEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IParticipantEntityRepository
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

class ParticipantModelPostgresRepositoryTest {
    private val gson: Gson = GsonConfig().gson()
    private val repository: IParticipantEntityRepository = mock()
    private val groupContentRepository: IGroupContentEntityRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val mapper: ParticipantEntityMapper = spy(ParticipantEntityMapper(gson))
    private val groupContentMapper: GroupContentEntityMapper = spy()
    private val modelRepository: ParticipantModelPostgresRepository =
        ParticipantModelPostgresRepository(repository, groupContentRepository, transactionalOperator, mapper, groupContentMapper)

    companion object {
        @JvmStatic
        fun `Should findAll call repository findAll`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, true, null, null),
            Arguments.of(true, true, now(), null),
            Arguments.of(true, true, null, now()),
            Arguments.of(true, true, now(), now()),
        )
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAll call repository findAll`(
        onlyVisible: Boolean,
        onlyPresent: Boolean,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ) {
        // Arrange
        val participant = ParticipantEntity()
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(participant))

        // Act
        modelRepository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(participant)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Arrange
        val participant = ParticipantEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(participant))

        // Act
        modelRepository.findById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(participant)
    }

    @Test
    fun `Should save call repository save`() {
        // Arrange
        val participant = ParticipantModel()
        val participantEntity = ParticipantEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(participantEntity))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(participant))

        // Act
        modelRepository.create(participant).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(participant)
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
