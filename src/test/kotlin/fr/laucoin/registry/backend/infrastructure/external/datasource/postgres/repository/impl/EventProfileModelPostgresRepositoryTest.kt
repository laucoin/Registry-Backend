package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.ACCEPTED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.INVITED
import fr.laucoin.registry.backend.domain.enumeration.ProfileStatusEnum.REJECTED
import fr.laucoin.registry.backend.domain.model.EventProfileModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.event.profile.EventProfileRoleCountEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.EventProfileRoleCountEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IEventProfileEntityRepository
import fr.laucoin.registry.backend.test.ModelExt.eventId
import java.time.ZonedDateTime
import java.time.ZonedDateTime.now
import java.util.UUID
import java.util.stream.Stream
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito.anyBoolean
import org.mockito.Mockito.mock
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verifyNoInteractions
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EventProfileModelPostgresRepositoryTest {
    private val repository: IEventProfileEntityRepository = mock()
    private val mapper: EventProfileEntityMapper = spy()
    private val roleCountMapper: EventProfileRoleCountEntityMapper = spy()
    private val modelRepository: EventProfileModelPostgresRepository =
        EventProfileModelPostgresRepository(repository, mapper, roleCountMapper)

    companion object {
        @JvmStatic
        fun `Should findEventProfilesByXId call repository findByXId`(): Stream<Arguments> = Stream.of(
            Arguments.of(true, true, ACCEPTED, now(), null),
            Arguments.of(true, true, INVITED, null, now()),
            Arguments.of(true, true, REJECTED, now(), now()),
        )
    }

    @ParameterizedTest
    @MethodSource("Should findEventProfilesByXId call repository findByXId")
    fun `Should findEventProfilesByEventId call repository findByEventId`(
        onlyVisible: Boolean,
        onlyUsable: Boolean,
        status: ProfileStatusEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ) {
        // Arrange
        val profile = EventProfileEntity()
        `when`(
            repository.findByEventId(
                any(),
                anyBoolean(),
                anyBoolean(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(profile))

        // Act
        modelRepository.findEventProfilesByEventId(eventId, onlyVisible, onlyUsable, status, startDateTime, endDateTime)
            .blockFirst()

        // Assert
        verify(repository, times(1)).findByEventId(eventId, onlyVisible, onlyUsable, status, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @ParameterizedTest
    @MethodSource("Should findEventProfilesByXId call repository findByXId")
    fun `Should findEventProfilesByUserId call repository findByUserId`(
        onlyVisible: Boolean,
        onlyActive: Boolean,
        status: ProfileStatusEnum?,
        startDateTime: ZonedDateTime?,
        endDateTime: ZonedDateTime?
    ) {
        // Arrange
        val userId = UUID.randomUUID()
        val profile = EventProfileEntity()
        `when`(
            repository.findByUserId(
                any(),
                anyBoolean(),
                anyBoolean(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull()
            )
        ).thenReturn(Flux.just(profile))

        // Act
        modelRepository.findEventProfilesByUserId(userId, onlyVisible, onlyActive, status, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findByUserId(userId, onlyVisible, onlyActive, status, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should findById call repository findByIdAndEventId`() {
        // Arrange
        val profile = EventProfileEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findByIdAndEventId(any(), any(), any())).thenReturn(Mono.just(profile))

        // Act
        modelRepository.findById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findByIdAndEventId(eventId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should findEventProfilesByIdAndUserId call repository findByIdAndUserId`() {
        // Arrange
        val profile = EventProfileEntity()
        val userId = UUID.randomUUID()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findByIdAndUserId(any(), any(), any())).thenReturn(Mono.just(profile))

        // Act
        modelRepository.findEventProfilesByIdAndUserId(userId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findByIdAndUserId(userId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should findEventProfileByEventAndUserId call repository findEventProfileByEventAndUserId`() {
        // Arrange
        val profile = EventProfileEntity()
        val userId = UUID.randomUUID()
        val onlyVisible = true
        val onlyActive = true
        val status = ACCEPTED
        `when`(repository.findEventProfileByEventAndUserId(any(), any(), any(), any(), any())).thenReturn(Mono.just(profile))

        // Act
        modelRepository.findEventProfileByEventAndUserId(eventId, userId, onlyVisible, onlyActive, status).block()

        // Assert
        verify(repository, times(1)).findEventProfileByEventAndUserId(eventId, userId, onlyVisible, onlyActive, status)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should findLevel0EventProfileRoleByUserId call repository findLevel0EventProfileRoleByUserId`() {
        // Arrange
        val count = EventProfileRoleCountEntity()
        val userId = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findLevel0EventProfileRoleByUserId(any(), any())).thenReturn(Flux.just(count))

        // Act
        modelRepository.findLevel0EventProfileRoleByUserId(userId, onlyVisible).blockFirst()

        // Assert
        verify(repository, times(1)).findLevel0EventProfileRoleByUserId(userId, onlyVisible)
        verifyNoInteractions(mapper)
        verify(roleCountMapper, times(1)).toModel(count)
    }

    @Test
    fun `Should findLevel0EventProfileRoleByEventId call repository findLevel0EventProfileRoleByEventId`() {
        // Arrange
        val profile = EventProfileEntity()
        val onlyVisible = true
        `when`(repository.findLevel0EventProfileRoleByEventId(any(), any())).thenReturn(Flux.just(profile))

        // Act
        modelRepository.findLevel0EventProfileRoleByEventId(eventId, onlyVisible).blockFirst()

        // Assert
        verify(repository, times(1)).findLevel0EventProfileRoleByEventId(eventId, onlyVisible)
        verify(mapper, times(1)).toModel(profile)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should create call repository save`() {
        // Arrange
        val profile = EventProfileModel()
        val profileEntity = EventProfileEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(profileEntity))

        // Act
        modelRepository.create(profile).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(profile)
        verify(mapper, times(1)).toModel(profileEntity)
        verifyNoInteractions(roleCountMapper)
    }

    @Test
    fun `Should saveAll call repository saveAll`() {
        // Arrange
        val profile = EventProfileModel()
        val profileEntity = EventProfileEntity()
        `when`(repository.saveAll(any<List<EventProfileEntity>>())).thenReturn(Flux.just(*arrayOf(profileEntity)))

        // Act
        modelRepository.saveAll(listOf(profile)).blockFirst()

        // Assert
        verify(repository, times(1)).saveAll(any<List<EventProfileEntity>>())
        verify(mapper, times(1)).toEntity(profile)
        verify(mapper, times(1)).toModel(profileEntity)
        verifyNoInteractions(roleCountMapper)
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
