package fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.impl

import fr.laucoin.registry.backend.domain.model.VehicleModel
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.entity.vehicle.VehicleEntity
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.mapper.VehicleEntityMapper
import fr.laucoin.registry.backend.infrastructure.external.datasource.postgres.repository.IVehicleEntityRepository
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
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class VehicleModelPostgresRepositoryTest {
    private val repository: IVehicleEntityRepository = mock()
    private val mapper: VehicleEntityMapper = spy(VehicleEntityMapper())
    private val modelRepository: VehicleModelPostgresRepository = VehicleModelPostgresRepository(repository, mapper)

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
        val vehicle = VehicleEntity()
        `when`(repository.findAll(any(), any(), any(), anyOrNull(), anyOrNull())).thenReturn(Flux.just(vehicle))

        // Act
        modelRepository.findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime).blockFirst()

        // Assert
        verify(repository, times(1)).findAll(eventId, onlyVisible, onlyPresent, startDateTime, endDateTime)
        verify(mapper, times(1)).toModel(vehicle)
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
        val vehicle = VehicleEntity()
        val uuid = UUID.randomUUID()
        val onlyVisible = true
        `when`(repository.findById(any(), any(), any())).thenReturn(Mono.just(vehicle))

        // Act
        modelRepository.findById(eventId, uuid, onlyVisible).block()

        // Assert
        verify(repository, times(1)).findById(eventId, uuid, onlyVisible)
        verify(mapper, times(1)).toModel(vehicle)
    }

    @Test
    fun `Should create call repository save`() {
        // Arrange
        val vehicle = VehicleModel()
        val vehicleEntity = VehicleEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(vehicleEntity))

        // Act
        modelRepository.create(vehicle).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(vehicle)
    }

    @Test
    fun `Should update call repository save`() {
        // Arrange
        val vehicle = VehicleModel()
        val vehicleEntity = VehicleEntity()
        `when`(repository.save(any())).thenReturn(Mono.just(vehicleEntity))

        // Act
        modelRepository.update(vehicle).block()

        // Assert
        verify(repository, times(1)).save(any())
        verify(mapper, times(1)).toEntity(vehicle)
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
