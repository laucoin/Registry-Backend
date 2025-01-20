package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import com.nimbusds.jose.shaded.gson.Gson
import fr.laucoin.registry.backend.config.GsonConfig
import fr.laucoin.registry.backend.domain.model.GroupModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.group.GroupEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupContentEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.GroupEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupContentEntityRepository
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IGroupEntityRepository
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

class GroupModelPostgresRepositoryTest {
    private val gson: Gson = GsonConfig().gson()
    private val repository: IGroupEntityRepository = mock()
    private val contentRepository: IGroupContentEntityRepository = mock()
    private val transactionalOperator: TransactionalOperator = mock()
    private val mapper: GroupEntityMapper = spy(GroupEntityMapper(gson))
    private val contentMapper: GroupContentEntityMapper = spy()
    private val modelRepository: GroupModelPostgresRepository =
        GroupModelPostgresRepository(repository, contentRepository, transactionalOperator, mapper, contentMapper)

    companion object {
        @JvmStatic
        fun `Should findAll call repository findAll`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, true, null, null),
            Arguments.of(true, true, now(), null),
            Arguments.of(true, true, null, now()),
            Arguments.of(true, true, now(), now()),
        )

        @JvmStatic
        fun `Should findAllByIds call repository findAllByIds`(): Stream<Arguments> = Stream.of(
            Arguments.of(emptyList<UUID>(), 0),
            Arguments.of(listOf(UUID.randomUUID()), 1),
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
        val group = GroupEntity()
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(group))

        // Act
        modelRepository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(group)
    }

    @ParameterizedTest
    @MethodSource
    fun `Should findAllByIds call repository findAllByIds`(ids: List<UUID>, expectedCall: Int) {
        // Arrange
        val onlyVisible = true
        `when`(repository.findAllByIds(any(), any(), any())).thenReturn(Flux.empty())

        // Act
        modelRepository.findAllByIds(eventId, ids, onlyVisible).blockFirst()

        // Assert
        verify(repository, times(expectedCall)).findAllByIds(eventId, ids, onlyVisible)
    }

    @Test
    fun `Should findById call repository findById`() {
        // Arrange
        val group = GroupEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(group))

        // Act
        modelRepository.findById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(group)
    }

    @Test
    fun `Should create call repository save`() {
        // Arrange
        val group = GroupModel()
        val groupEntity = GroupEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(groupEntity))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(group))

        // Act
        modelRepository.create(group).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(group)
    }

    @Test
    fun `Should update call repository save`() {
        // Arrange
        val group = GroupModel()
        val groupEntity = GroupEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(groupEntity))
        `when`(transactionalOperator.transactional(any<Mono<*>>())).thenReturn(Mono.just(group))

        // Act
        modelRepository.update(group).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(group)
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
